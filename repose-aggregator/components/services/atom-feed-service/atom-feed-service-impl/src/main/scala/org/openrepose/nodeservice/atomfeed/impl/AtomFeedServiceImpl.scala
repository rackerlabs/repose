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
import com.typesafe.scalalogging.slf4j.StrictLogging
import io.opentracing.Tracer
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.filter.SystemModelInterrogator
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.httpclient.HttpClientService
import org.openrepose.core.spring.ReposeSpringProperties
import org.openrepose.core.systemmodel.config.SystemModel
import org.openrepose.docs.repose.atom_feed_service.v1.{AtomFeedConfigType, AtomFeedServiceConfigType, OpenStackIdentityV2AuthenticationType}
import org.openrepose.nodeservice.atomfeed.impl.actors.NotifierManager._
import org.openrepose.nodeservice.atomfeed.impl.actors._
import org.openrepose.nodeservice.atomfeed.{AtomFeedListener, AtomFeedService, AuthenticatedRequestFactory}
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.support.AbstractApplicationContext

import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

@Named
class AtomFeedServiceImpl @Inject()(@Value(ReposeSpringProperties.CORE.REPOSE_VERSION) reposeVersion: String,
                                    @Value(ReposeSpringProperties.NODE.NODE_ID) nodeId: String,
                                    httpClientService: HttpClientService,
                                    configurationService: ConfigurationService,
                                    applicationContext: ApplicationContext,
                                    tracer: Tracer)
  extends AtomFeedService with StrictLogging {

  import AtomFeedServiceImpl._

  private val actorSystem: ActorSystem = ActorSystem.create("AtomFeedServiceSystem")

  private var isServiceEnabled = false
  private var feedActors: Map[String, FeedActorTrio] = Map.empty
  private var listenerNotifierManagers: Map[String, ActorRef] = Map.empty
  private var feedConfigurations: Seq[AtomFeedConfigType] = Seq.empty
  // note: feed readers creation/destruction determined by configuration updates
  // note: notifier managers creation/destruction determined by registering/unregistering

  @PostConstruct
  def init(): Unit = {
    logger.info("Initializing and registering configuration listeners")
    val xsdURL = getClass.getResource("/META-INF/schema/config/atom-feed-service.xsd")

    configurationService.subscribeTo(
      DefaultConfig,
      xsdURL,
      AtomFeedServiceConfigurationListener,
      classOf[AtomFeedServiceConfigType]
    )
    configurationService.subscribeTo(
      SystemModelConfig,
      SystemModelConfigurationListener,
      classOf[SystemModel]
    )
  }

  @PreDestroy
  def destroy(): Unit = {
    logger.info("Unregistering configuration listeners and shutting down service")
    configurationService.unsubscribeFrom(DefaultConfig, AtomFeedServiceConfigurationListener)
    configurationService.unsubscribeFrom(SystemModelConfig, SystemModelConfigurationListener)

    actorSystem.shutdown()
  }

  override def registerListener(feedId: String, listener: AtomFeedListener): String = {
    val listenerId = java.util.UUID.randomUUID().toString
    logger.info("Attempting to register a listener with id: " + listenerId + " to feed with id: " + feedId)

    val notifierManager = feedActors.get(feedId) match {
      case Some(FeedActorTrio(manager, _, _)) =>
        manager
      case None =>
        val manager = actorSystem.actorOf(Props[NotifierManager], name = feedId + NotifierManagerTag)
        feedActors += (feedId -> FeedActorTrio(manager, None, None))

        if (isServiceEnabled) {
          manager ! ServiceEnabled
        }

        manager
    }

    notifierManager ! AddNotifier(listenerId, listener)

    listenerNotifierManagers += (listenerId -> notifierManager)

    listenerId
  }

  override def unregisterListener(listenerId: String): Unit = {
    logger.info("Attempting to unregister the listener with id: " + listenerId)

    listenerNotifierManagers.get(listenerId) match {
      case Some(notifierManager) =>
        notifierManager ! RemoveNotifier(listenerId)
        listenerNotifierManagers -= listenerId
      case None =>
        logger.debug("Listener not registered: " + listenerId)
    }
  }

  case class FeedActorTrio(notifierManager: ActorRef, feedReader: Option[ActorRef], authenticationContext: Option[AbstractApplicationContext])

  private object AtomFeedServiceConfigurationListener extends UpdateListener[AtomFeedServiceConfigType] {
    private var initialized = false

    override def configurationUpdated(configurationObject: AtomFeedServiceConfigType): Unit = {
      synchronized {
        logger.trace("Service configuration updated")
        initialized = true

        // Destroy FeedReaders that have been removed from, or changed in, the configuration
        feedConfigurations.diff(configurationObject.getFeed) foreach { deadFeedConfig =>
          feedActors.get(deadFeedConfig.getId) foreach { actorTrio =>
            actorTrio.notifierManager ! PoisonPill
            actorTrio.feedReader foreach (_ ! PoisonPill)
            actorTrio.authenticationContext foreach (_.close())
            feedActors -= deadFeedConfig.getId
          }
        }

        // Create FeedReaders that are defined in the configuration
        configurationObject.getFeed.diff(feedConfigurations) foreach { newFeedConfig =>
          val authenticationConfig = Option(newFeedConfig.getAuthentication)

          // Define a new Spring context to hold the authentication component and its configuration.
          val authenticationContext = new AnnotationConfigApplicationContext()

          val notifierManager = feedActors.get(newFeedConfig.getId)
            .map(_.notifierManager)
            .getOrElse(actorSystem.actorOf(Props[NotifierManager], name = newFeedConfig.getId + NotifierManagerTag))

          def buildAuthenticatedRequestFactory(feedConfig: AtomFeedConfigType, authConfig: Any): AuthenticatedRequestFactory = {

            def getFqcnFromConfig(cfg: Any): String = {
              cfg match {
                case good if good.isInstanceOf[OpenStackIdentityV2AuthenticationType] =>
                  good.asInstanceOf[OpenStackIdentityV2AuthenticationType].getFqcn
                case bad =>
                  throw new IllegalArgumentException(bad.getClass.getName + " is not a know AuthenticatedRequestFactory")
              }
            }

            def getClassFromFqcn(fqcn: String): Class[_ <: AuthenticatedRequestFactory] = {
              try {
                Class.forName(fqcn).asSubclass(classOf[AuthenticatedRequestFactory])
              } catch {
                case cnfe: ClassNotFoundException =>
                  throw new IllegalArgumentException(fqcn + " was not found", cnfe)
                case cce: ClassCastException =>
                  throw new IllegalArgumentException(fqcn + " is not an AuthenticatedRequestFactory", cce)
              }
            }

            authenticationContext.setParent(applicationContext)

            // Register the feed's config and the authentication config as a Spring Bean so that it can be auto-wired
            // when the AuthenticatedRequestFactory is created.
            authenticationContext.getBeanFactory.registerSingleton(feedConfig.getId+"_FEED", feedConfig)
            authenticationContext.getBeanFactory.registerSingleton(feedConfig.getId+"_AUTH", authConfig)
            authenticationContext.refresh()

            // Take the configured FQCN of the authentication component and create a complete Spring Bean in an
            // isolated context (thereby auto-wiring Repose services).
            val authCls = getClassFromFqcn(getFqcnFromConfig(authConfig))
            val authenticatedRequestFactory = Try(authenticationContext
              .getAutowireCapableBeanFactory
              .createBean(authCls, AUTOWIRE_CONSTRUCTOR, true)
            ) match {
              case Success(success) => success
              case Failure(ex) =>
                // Since Spring didn't create the desired bean,
                logger.debug("Bean Creation error. Reason: {}", ex.getLocalizedMessage)
                logger.trace("", ex)
                // try to manually creat a new instance of the class.
                authCls.newInstance
            }

            Option(authenticatedRequestFactory.asInstanceOf[AuthenticatedRequestFactory]) match {
              case Some(factory: AuthenticatedRequestFactory) =>
                factory
              case _ => throw new IllegalArgumentException("Unable to instantiate a valid AuthenticatedRequestFactory")
            }
          }

          val feedReader = actorSystem.actorOf(
            FeedReader.props(
              newFeedConfig.getUri,
              httpClientService,
              tracer,
              newFeedConfig.getConnectionPoolId,
              authenticationConfig.map(buildAuthenticatedRequestFactory(newFeedConfig, _)),
              authenticationConfig.map(_.getTimeout).filterNot(_ == 0).map(_.milliseconds).getOrElse(Duration.Inf),
              notifierManager,
              newFeedConfig.getPollingFrequency,
              newFeedConfig.getEntryOrder,
              reposeVersion),
            newFeedConfig.getId + FeedReaderTag)

          feedActors += (newFeedConfig.getId -> FeedActorTrio(notifierManager, Some(feedReader), Some(authenticationContext)))

          if (isServiceEnabled) {
            notifierManager ! ServiceEnabled
          }

          feedConfigurations = feedConfigurations :+ newFeedConfig
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

        isServiceEnabled = SystemModelInterrogator.getService(configurationObject, ServiceName).isPresent

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
  final val ServiceName = "atom-feed-service"

  private final val DefaultConfig = ServiceName + ".cfg.xml"
  private final val SystemModelConfig = "system-model.cfg.xml"
  private final val NotifierManagerTag = "NotifierManager"
  private final val FeedReaderTag = "FeedReader"
}
