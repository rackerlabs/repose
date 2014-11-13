package org.openrepose.valve.spring

import java.net.{InetAddress, NetworkInterface, UnknownHostException}
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Named

import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.container.config.ContainerConfiguration
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.systemmodel.SystemModel
import org.openrepose.valve.ReposeJettyServer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Autowired


/**
 * A singleton that's spring aware because of the services it needs to use.
 */
@Named
class ValveRunner @Autowired()(
                                configService: ConfigurationService
                                ) extends DisposableBean {

  private val LOG = LoggerFactory.getLogger(this.getClass)

  private val systemModelXsdURL = getClass.getResource("/META-INF/schema/system-model/system-model.xsd")
  private val containerXsdUrl = getClass.getResource("/META-INF/schema/container/container-configuration.xsd")

  //Just a single countdown latch that gets triggered when told to stop
  private val runLatch = new CountDownLatch(1)

  private var activeNodes = Set[ReposeJettyServer]()
  private val nodeModificationLock = new Object()

  private val currentContainerConfig = new AtomicReference[ContainerConfiguration]()
  private val currentSystemModel = new AtomicReference[SystemModel]()

  // Note this won't pick up on interfaces added after repose has been turned on I think (oh well?)
  private val localAddresses: Set[InetAddress] = {
    import scala.collection.JavaConverters._
    NetworkInterface.getNetworkInterfaces.asScala.toList.flatMap(interface =>
      interface.getInetAddresses.asScala.toList).toSet
  }

  def getActiveNodes: Set[ReposeJettyServer] = {
    nodeModificationLock.synchronized {
      Set.empty[ReposeJettyServer] ++ activeNodes
    }
  }

  /**
   * This method should block, so that way the primary java method doesn't quit
   * @return
   */
  def run(configRoot: String, insecure: Boolean): Int = {
    //Putting the config listeners in here, because I want the context for the configRoot, and the Insecure string

    def updateNodes(): Unit = {
      val systemModel = currentSystemModel.get
      val containerConfig = currentContainerConfig.get

      if (Option(systemModel).isDefined && Option(containerConfig).isDefined) {


        val sslConfig = containerConfig.getDeploymentConfig.getSslConfiguration

        def isLocal(host: String): Boolean = {
          try {
            localAddresses.contains(InetAddress.getByName(host))
          } catch {
            case e: UnknownHostException => false
          }
        }

        //Because it's so much easier to have all this in one object rather than having to get it from a hierarchy
        case class ConfiguredNode(clusterId: String, nodeId: String, host: String, httpPort: Option[Int], httpsPort: Option[Int])

        import scala.collection.JavaConversions._
        //Figure out what nodes are new in the system model, and do things
        //Get a list of ConfiguredNodes (so I have all the information I need) from the XML object by mapping the heck out of the
        //ugly jaxb objects
        val newConfiguredLocalNodes = systemModel.getReposeCluster.toList.flatMap { cluster =>
          cluster.getNodes.getNode.toList.filter(node => isLocal(node.getHostname)).map { xmlNode =>
            //This is a wrapper function to go from the XSD's primitive int type to an Option, so that it has meaning
            val intToOption: Int => Option[Int] = { i =>
              if (i == 0) {
                None
              } else {
                Some(i)
              }
            }
            ConfiguredNode(cluster.getId,
              xmlNode.getId,
              xmlNode.getHostname,
              intToOption(xmlNode.getHttpPort),
              intToOption(xmlNode.getHttpsPort))
          }
        }

        //If there are no configured local nodes, we're going to bail on all of it
        if (newConfiguredLocalNodes.isEmpty) {
          LOG.error("No local nodes found in system-model, exiting Valve!")
          runLatch.countDown()
        } else {
          //Grab ahold of the node lock, so that no other thread dorks with our nodes while we are
          nodeModificationLock.synchronized {
            //Build a list of nodes that we're going to stop
            //This list is things that are in the active list, but not in the newly parsed list.
            val stopList = activeNodes.filterNot { activeNode =>
              newConfiguredLocalNodes.exists { node =>
                node.nodeId == activeNode.nodeId &&
                  node.clusterId == activeNode.clusterId
              }
            }

            //Get things that aren't in the active nodes list, but are in the new configured nodes list
            //These we're going to start up
            val startList = newConfiguredLocalNodes.filterNot { n =>
              activeNodes.exists { active =>
                active.nodeId == n.nodeId &&
                  active.clusterId == n.clusterId
              }
            }

            //The combination of these two lists will also duplicate nodes, so that a node will be restarted with
            //different settings!

            //Shutdown all the stop nodes
            activeNodes = activeNodes -- stopList //Take out all the nodes that we're going to stop
            stopList.foreach { node =>
              node.shutdown()
            }

            //Start up all the new nodes, replacing the existing nodes list with a new one
            activeNodes = activeNodes ++ startList.map { n =>
              val node = new ReposeJettyServer(configRoot, n.clusterId, n.nodeId, n.httpPort, n.httpsPort, Option(sslConfig), insecure)
              node.start()
              node
            }
          }
        }
      }
    }

    val containerConfigListener = new UpdateListener[ContainerConfiguration] {
      var initialized = false

      override def configurationUpdated(configurationObject: ContainerConfiguration): Unit = {
        //Any change to this results in a restart of all nodes
        currentContainerConfig.set(configurationObject)

        nodeModificationLock.synchronized {
          activeNodes = activeNodes.map { node =>
            node.restart()
          }
        }

        //This might be the best way to trigger this without blocking
        //We make a call to update nodes in here because during startup, I might have received a systemmodel update before
        // getting a container config, so at that point I need to trigger a refresh of the nodes
        updateNodes()
      }

      override def isInitialized: Boolean = {
        initialized
      }
    }

    val systemModelConfigListener = new UpdateListener[SystemModel] {
      var initialized = false

      override def configurationUpdated(configurationObject: SystemModel): Unit = {
        currentSystemModel.set(configurationObject)
        //Set the current system model, and just update the nodes.
        updateNodes()
      }

      override def isInitialized: Boolean = {
        initialized
      }
    }


    //Only subscribe to the config files when told to start
    //Stupid APIs are stupid and also dumb
    configService.subscribeTo[ContainerConfiguration]("container.cfg.xml", containerXsdUrl, containerConfigListener, classOf[ContainerConfiguration])
    configService.subscribeTo[SystemModel]("system-model.cfg.xml", systemModelXsdURL, systemModelConfigListener, classOf[SystemModel])

    //Stay running, so that the thing doesn't exit or something

    //Await this latch forever!, better than a runloop
    runLatch.await() //This blocks this guy, so that the run thread keeps wedged here

    //Deregister from configs, will only happen after the runlatch has been released
    configService.unsubscribeFrom("container.cfg.xml", containerConfigListener)
    configService.unsubscribeFrom("system-model.cfg.xml", systemModelConfigListener)

    //Stop all local nodes
    nodeModificationLock.synchronized {
      activeNodes.foreach { n =>
        n.shutdown()
      }
      activeNodes = Set.empty[ReposeJettyServer]
    }
    0
  }


  /**
   * This will destroy the bean and shut it all down
   */
  override def destroy(): Unit = {
    runLatch.countDown()
  }
}
