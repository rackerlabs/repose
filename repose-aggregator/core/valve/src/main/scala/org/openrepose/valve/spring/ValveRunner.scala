package org.openrepose.valve.spring

import java.util.concurrent.{CountDownLatch, ConcurrentHashMap}
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Named

import org.eclipse.jetty.server.Server
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.container.config.ContainerConfiguration
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.systemmodel.SystemModel
import org.openrepose.valve.ReposeJettyServer
import org.springframework.beans.factory.annotation.Autowired


/**
 * A singleton that's spring aware because of the services it needs to use.
 */
@Named("valveRunner")
class ValveRunner @Autowired()(
                                configService: ConfigurationService
                                ) {

  private val systemModelXsdURL = getClass.getResource("/META-INF/schema/system-model/system-model.xsd")
  private val containerXsdUrl = getClass.getResource("/META-INF/schema/container/container-configuration.xsd")

  //Just a single countdown latch that gets triggered when told to stop
  private val runLatch = new CountDownLatch(1)

  case class ReposeNode(clusterId:String, nodeId:String)

  val activeNodes = new ConcurrentHashMap[ReposeNode,ReposeJettyServer]()
  val nodeModificationLock = new Object()

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

    val containerConfigListener = new UpdateListener[ContainerConfiguration] {
      var initialized = false

      override def configurationUpdated(configurationObject: ContainerConfiguration): Unit = {
        //Any change to this results in a restart of all nodes
        restartAllNodes()
      }

      override def isInitialized: Boolean = {
        initialized
      }
    }

    val systemModelConfigListener = new UpdateListener[SystemModel] {
      var initialized = false
      override def configurationUpdated(configurationObject: SystemModel): Unit = {
        //Figure out what nodes are new in the system model, and do things
        //If there are nodes that are new, start them up
        //If a node has changed, restart it
        //If a node is gone, stop it
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
    0
  }


  private def restartAllNodes() = {
    nodeModificationLock.synchronized {
      //Acquired the lock, for each node, kill it, and create a new one
    }
  }

  /**
   * Tell the things to stop
   */
  def stop(): Unit = {
    runLatch.countDown()
  }
}
