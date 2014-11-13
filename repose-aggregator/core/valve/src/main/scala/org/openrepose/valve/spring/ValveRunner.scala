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
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Autowired


/**
 * A singleton that's spring aware because of the services it needs to use.
 */
@Named("valveRunner")
class ValveRunner @Autowired()(
                                configService: ConfigurationService
                                ) extends DisposableBean {

  private val systemModelXsdURL = getClass.getResource("/META-INF/schema/system-model/system-model.xsd")
  private val containerXsdUrl = getClass.getResource("/META-INF/schema/container/container-configuration.xsd")

  //Just a single countdown latch that gets triggered when told to stop
  private val runLatch = new CountDownLatch(1)

  //TODO: figure out how to make this safer with regards to mutability
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

  //This is where the magic happens
  //Have to configure the springs
  //Start with the core services only first, then build a webapp context for each one,
  // So the core services need to be handed to the jetty builders
  // THis is a spring managed bean, to get stuff from the core service context, but we don't want to hand this one
  // to the various jetties, so use the core service context directly
  //Subscribe to the container.cfg.xml and the system-model.cfg.xml
  //Also give it the properties that we know about here, "insecure" and "configRoot"
  // Each one of the servers will also have a clusterId, nodeId, and port number
  // One node line == one server, just use multiple connectors for HTTP and HTTPS
  // Might need to know that it's HTTPS... (because if you swap from HTTP to HTTPS, without knowing it won't restart it)
  //On each successful configuration of either of those, trigger a rebuild of the servers....

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

        case class ConfiguredNode(clusterId: String, nodeId: String, host: String, httpPort: Option[Int], httpsPort: Option[Int])

        import scala.collection.JavaConversions._
        //Figure out what nodes are new in the system model, and do things
        //Get a list of ConfiguredNodes (so I have all the information I need) from the XML object by mapping the heck out of the
        //ugly jaxb objects
        val newConfiguredLocalNodes = systemModel.getReposeCluster.toList.flatMap { cluster =>
          cluster.getNodes.getNode.toList.filter(node => isLocal(node.getHostname)).map { xmlNode =>
            implicit val intToOption: Int => Option[Int] = { i =>
              if (i == 0) {
                None
              } else {
                Some(i)
              }
            }
            ConfiguredNode(cluster.getId, xmlNode.getId, xmlNode.getHostname, xmlNode.getHttpPort, xmlNode.getHttpsPort)
          }
        }

        nodeModificationLock.synchronized {
          val stopList = activeNodes.filterNot { activeNode =>
            newConfiguredLocalNodes.exists { node =>
              node.nodeId == activeNode.nodeId &&
                node.clusterId == activeNode.clusterId
            }
          }

          val startList = newConfiguredLocalNodes.filterNot { n =>
            activeNodes.exists { active =>
              active.nodeId == n.nodeId &&
                active.clusterId == n.clusterId
            }
          }

          //Shutdown all the stop nodes
          activeNodes = activeNodes -- stopList //Take out all the nodes that we're going to stop
          stopList.foreach { node =>
            node.shutdown()
          }

          //Start up all the new nodes
          activeNodes = activeNodes ++ startList.map { n =>
            val node = new ReposeJettyServer(configRoot, n.clusterId, n.nodeId, n.httpPort, n.httpsPort, Option(sslConfig), insecure)
            node.start()
            node
          }
        }
        //If there are nodes that are new, start them up
        //If a node has changed, restart it
        //If a node is gone, stop it
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
    runLatch.await()

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
