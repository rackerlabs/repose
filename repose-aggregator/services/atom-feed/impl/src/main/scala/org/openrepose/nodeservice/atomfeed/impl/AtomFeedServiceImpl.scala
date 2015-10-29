/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.nodeservice.atomfeed.impl

import javax.annotation.{PostConstruct, PreDestroy}
import javax.inject.{Inject, Named}

import akka.actor.{ActorRefFactory, ActorSystem, Cancellable}
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.filter.SystemModelInterrogator
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.spring.ReposeSpringProperties
import org.openrepose.core.systemmodel.SystemModel
import org.openrepose.docs.repose.atom_feed_service.v1.AtomFeedServiceConfigType
import org.openrepose.nodeservice.atomfeed.impl.actors.FeedReader.ReadFeed
import org.openrepose.nodeservice.atomfeed.impl.actors._
import org.openrepose.nodeservice.atomfeed.{AtomFeedListener, AtomFeedService}
import org.springframework.beans.factory.annotation.Value

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

@Named
class AtomFeedServiceImpl @Inject()(@Value(ReposeSpringProperties.NODE.CLUSTER_ID) clusterId: String,
                                    @Value(ReposeSpringProperties.NODE.NODE_ID) nodeId: String,
                                    configurationService: ConfigurationService)
  extends AtomFeedService with LazyLogging {

  import AtomFeedServiceImpl._

  private var isServiceEnabled: Boolean = false
  private var running: Boolean = false
  private var actorSystem: ActorSystem = _
  private var serviceConfig: AtomFeedServiceConfigType = _
  private var feedSchedules: Map[String, Cancellable] = Map.empty
  private var feedListeners: Map[String, Map[String, AtomFeedListener]] = Map.empty

  @PostConstruct
  def init(): Unit = {
    logger.info("Initializing and registering configuration listeners")
    val xsdURL = getClass.getResource("/META-INF/schema/config/atom-feed-service.xsd")

    configurationService.subscribeTo(
      DEFAULT_CONFIG,
      xsdURL,
      AtomFeedServiceConfigurationListener,
      classOf[AtomFeedServiceConfigType]
    )
    configurationService.subscribeTo(
      SYSTEM_MODEL_CONFIG,
      SystemModelConfigurationListener,
      classOf[SystemModel]
    )
  }

  @PreDestroy
  def destroy(): Unit = {
    configurationService.unsubscribeFrom(DEFAULT_CONFIG, AtomFeedServiceConfigurationListener)
    configurationService.unsubscribeFrom(SYSTEM_MODEL_CONFIG, SystemModelConfigurationListener)

    if (running) {
      actorSystem.shutdown()
    }
  }

  override def registerListener(feedId: String, listener: AtomFeedListener): String = {
    ifRunning {
      serviceConfig.getFeed.find(feedConfig => feedId.equals(feedConfig.getId)) match {
        case Some(feedConfig) =>
          val listenerId = java.util.UUID.randomUUID().toString
          logger.debug("Registering a listener with id: " + listenerId + " to feed with id: " + feedId)

          val pastListeners = feedListeners.getOrElse(feedId, Map.empty)
          feedListeners = feedListeners + (feedId -> (pastListeners + (listenerId -> listener)))
          if (pastListeners.isEmpty) {
            val schedule = actorSystem.scheduler.schedule(
              Duration.Zero,
              feedConfig.getPollingFrequency.seconds,
              actorSystem.actorOf(FeedReader.props(
                feedConfig.getUri,
                (arf: ActorRefFactory) =>
                  arf.actorOf(Authenticator.props(feedConfig.getAuthentication)),
                (arf: ActorRefFactory) =>
                  arf.actorOf(Notifier.props(feedListeners.getOrElse(feedId, Map.empty).values.toSet)),
                feedConfig.getEntryOrder
              )),
              ReadFeed
            )
            feedSchedules = feedSchedules + (feedId -> schedule)
          }

          listenerId
        case None =>
          throw new IllegalArgumentException("feedId parameter does not match a configured feed id")
      }
    }
  }

  override def unregisterListener(listenerId: String): Unit = {
    ifRunning {
      feedListeners find {
        case (_, listeners) => listeners.contains(listenerId)
      } match {
        case Some((feedId, listeners)) =>
          logger.debug("Un-registering the listener: " + listenerId)

          val newListeners = listeners filterNot { case (id, listener) => listenerId.equals(id) }
          feedListeners = feedListeners + (feedId -> newListeners)
          if (newListeners.isEmpty) {
            feedSchedules(feedId).cancel()
          }
        case None =>
          logger.debug("Listener not registered: " + listenerId)
      }
    }
  }

  override def isRunning: Boolean = running

  private def startService(): Unit = {
    synchronized {
      if (isInitialized && isServiceEnabled && !running) {
        logger.info("Starting the service")
        actorSystem = ActorSystem.create("AtomFeedServiceSystem")
        actorSystem.registerOnTermination(running = false)
        running = true
      } else if (running) {
        logger.debug("Service not starting -- service is already running")
      } else if (!isServiceEnabled) {
        logger.debug("Service not starting -- disabled by configuration")
      } else {
        logger.debug("Service not starting -- waiting on configuration files to load")
      }
    }
  }

  private def stopService(): Unit = {
    synchronized {
      if (running) {
        logger.info("Stopping the service")
        actorSystem.shutdown()
        actorSystem.awaitTermination(5 seconds)
      } else {
        logger.debug("Service not stopping -- service is already stopped")
      }
    }
  }

  private def ifRunning[T](f: => T): T = {
    if (running) f
    else throw new IllegalStateException("Service is not running")
  }

  private def getListenersForFeed(feedId: String): Map[String, AtomFeedListener] = {
    feedListeners.getOrElse(feedId, Map.empty)
  }

  def isInitialized: Boolean = {
    SystemModelConfigurationListener.isInitialized && AtomFeedServiceConfigurationListener.isInitialized
  }

  object AtomFeedServiceConfigurationListener extends UpdateListener[AtomFeedServiceConfigType] {
    private var initialized = false

    override def configurationUpdated(configurationObject: AtomFeedServiceConfigType): Unit = {
      synchronized {
        initialized = true
        serviceConfig = configurationObject

        if (running) {
          stopService() // Stop the service if it's running to force an update of the Actors
        }
        startService() // Try to start the service in case the system model was loaded first
      }
    }

    override def isInitialized: Boolean = initialized
  }

  object SystemModelConfigurationListener extends UpdateListener[SystemModel] {
    private var initialized = false

    override def configurationUpdated(configurationObject: SystemModel): Unit = {
      synchronized {
        initialized = true

        val systemModelInterrogator = new SystemModelInterrogator(clusterId, nodeId)
        isServiceEnabled = systemModelInterrogator.getServiceForCluster(configurationObject, SERVICE_NAME).isPresent

        if (isServiceEnabled) {
          startService()
        } else {
          stopService()
        }
      }
    }

    override def isInitialized: Boolean = initialized
  }

}

object AtomFeedServiceImpl {
  final val SERVICE_NAME = "atom-feed-service"

  private final val DEFAULT_CONFIG = SERVICE_NAME + ".cfg.xml"
  private final val SYSTEM_MODEL_CONFIG = "system-model.cfg.xml"
}
