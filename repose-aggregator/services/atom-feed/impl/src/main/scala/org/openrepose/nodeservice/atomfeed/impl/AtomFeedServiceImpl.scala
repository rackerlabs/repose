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

import akka.actor._
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.filter.SystemModelInterrogator
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.spring.ReposeSpringProperties
import org.openrepose.core.systemmodel.SystemModel
import org.openrepose.docs.repose.atom_feed_service.v1.{AtomFeedServiceConfigType, OpenStackIdentityV2AuthenticationType}
import org.openrepose.nodeservice.atomfeed.impl.actors.NotifierManager._
import org.openrepose.nodeservice.atomfeed.impl.actors._
import org.openrepose.nodeservice.atomfeed.{AtomFeedListener, AtomFeedService, AuthenticatedRequestFactory}
import org.springframework.beans.factory.annotation.Value

import scala.collection.JavaConversions._
import scala.language.postfixOps

@Named
class AtomFeedServiceImpl @Inject()(@Value(ReposeSpringProperties.CORE.REPOSE_VERSION) reposeVersion: String,
                                    @Value(ReposeSpringProperties.NODE.CLUSTER_ID) clusterId: String,
                                    @Value(ReposeSpringProperties.NODE.NODE_ID) nodeId: String,
                                    configurationService: ConfigurationService)
  extends AtomFeedService with LazyLogging {

  import AtomFeedServiceImpl._

  private val actorSystem: ActorSystem = ActorSystem.create("AtomFeedServiceSystem")

  private var isServiceEnabled = false
  private var feedActors: Map[String, FeedActorPair] = Map.empty
  private var listenerNotifierManagers: Map[String, ActorRef] = Map.empty
  // note: feed readers creation/destruction determined by configuration updates
  // note: notifier managers creation/destruction determined by registering/unregistering

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
    logger.info("Unregistering configuration listeners and shutting down service")
    configurationService.unsubscribeFrom(DEFAULT_CONFIG, AtomFeedServiceConfigurationListener)
    configurationService.unsubscribeFrom(SYSTEM_MODEL_CONFIG, SystemModelConfigurationListener)

    actorSystem.shutdown()
  }

  override def registerListener(feedId: String, listener: AtomFeedListener): String = {
    val listenerId = java.util.UUID.randomUUID().toString
    logger.info("Attempting to register a listener with id: " + listenerId + " to feed with id: " + feedId)

    val notifierManager = feedActors.get(feedId) match {
      case Some(FeedActorPair(manager, _)) =>
        manager
      case None =>
        val manager = actorSystem.actorOf(Props[NotifierManager], name = feedId + NOTIFIER_MANAGER_TAG)
        feedActors = feedActors + (feedId -> FeedActorPair(manager, None))

        if (isServiceEnabled) {
          manager ! ServiceEnabled
        }

        manager
    }

    notifierManager ! AddNotifier(listenerId, listener)

    listenerNotifierManagers = listenerNotifierManagers + (listenerId -> notifierManager)

    listenerId
  }

  override def unregisterListener(listenerId: String): Unit = {
    logger.info("Attempting to unregister the listener with id: " + listenerId)

    listenerNotifierManagers.get(listenerId) match {
      case Some(notifierManager) =>
        notifierManager ! RemoveNotifier(listenerId)
        listenerNotifierManagers = listenerNotifierManagers - listenerId
      case None =>
        logger.debug("Listener not registered: " + listenerId)
    }
  }

  case class FeedActorPair(notifierManager: ActorRef, feedReader: Option[ActorRef])

  private object AtomFeedServiceConfigurationListener extends UpdateListener[AtomFeedServiceConfigType] {
    private var initialized = false

    override def configurationUpdated(configurationObject: AtomFeedServiceConfigType): Unit = {
      synchronized {
        logger.trace("Service configuration updated")
        initialized = true

        configurationObject.getFeed foreach { feedConfig =>
          val notifierManager = feedActors.get(feedConfig.getId) match {
            case Some(actorPair) =>
              // todo: only replace the current feed reader if the configuration has changed
              actorPair.feedReader.foreach(_ ! PoisonPill)
              actorPair.notifierManager
            case None =>
              actorSystem.actorOf(Props[NotifierManager], name = feedConfig.getId + NOTIFIER_MANAGER_TAG)
          }

          // note: if the old feed reader actor was sent a PoisonPill, the construction of this new feed reader actor
          //       may result in the existence of multiple actors with the same name
          val feedReader = actorSystem.actorOf(FeedReader.props(
            feedConfig.getUri,
            Option(feedConfig.getAuthentication).map(buildAuthenticatedRequestFactory),
            notifierManager,
            feedConfig.getPollingFrequency,
            feedConfig.getEntryOrder,
            reposeVersion
          ), feedConfig.getId + FEED_READER_TAG)

          feedActors = feedActors + (feedConfig.getId -> FeedActorPair(notifierManager, Some(feedReader)))

          if (isServiceEnabled) {
            notifierManager ! ServiceEnabled
          }
        }
      }
    }

    override def isInitialized: Boolean = initialized
  }

  private object SystemModelConfigurationListener extends UpdateListener[SystemModel] {
    private var initialized = false

    override def configurationUpdated(configurationObject: SystemModel): Unit = {
      synchronized {
        logger.trace("System model configuration updated")

        initialized = true

        val systemModelInterrogator = new SystemModelInterrogator(clusterId, nodeId)

        isServiceEnabled = systemModelInterrogator.getServiceForCluster(configurationObject, SERVICE_NAME).isPresent

        if (isServiceEnabled) {
          feedActors foreach { case (_, actorPair) =>
            actorPair.notifierManager ! ServiceEnabled
          }
        } else {
          feedActors foreach { case (_, actorPair) =>
            actorPair.notifierManager ! ServiceDisabled
          }
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
  private final val NOTIFIER_MANAGER_TAG = "NotifierManager"
  private final val FEED_READER_TAG = "FeedReader"

  def buildAuthenticatedRequestFactory(config: OpenStackIdentityV2AuthenticationType): AuthenticatedRequestFactory = {
    val fqcn = config.getFqcn

    val arfInstance = try {
      val arfClass = Class.forName(fqcn).asSubclass(classOf[AuthenticatedRequestFactory])

      val arfConstructors = arfClass.getConstructors
      arfConstructors find { constructor =>
        val paramTypes = constructor.getParameterTypes
        paramTypes.size == 1 && classOf[OpenStackIdentityV2AuthenticationType].isAssignableFrom(paramTypes(0))
      } map { constructor =>
        constructor.newInstance(config)
      } orElse {
        arfConstructors.find(_.getParameterTypes.isEmpty).map(_.newInstance())
      }
    } catch {
      case cnfe: ClassNotFoundException =>
        throw new IllegalArgumentException(fqcn + " was not found", cnfe)
      case cce: ClassCastException =>
        throw new IllegalArgumentException(fqcn + " is not an AuthenticatedRequestFactory", cce)
    }

    arfInstance match {
      case Some(factory: AuthenticatedRequestFactory) => factory
      case _ => throw new IllegalArgumentException(fqcn + " is not a valid AuthenticatedRequestFactory")
    }
  }
}
