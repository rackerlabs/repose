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
package org.openrepose.filters.openstackidentityv3

import java.net.URL

import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import com.typesafe.scalalogging.slf4j.StrictLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.servlet.filter.FilterAction
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.DatastoreService
import org.openrepose.core.services.datastore.types.HashSetPatch
import org.openrepose.core.services.httpclient.{HttpClientService, HttpClientServiceClient}
import org.openrepose.filters.openstackidentityv3.config.OpenstackIdentityV3Config
import org.openrepose.filters.openstackidentityv3.utilities.Cache._
import org.openrepose.filters.openstackidentityv3.utilities.OpenStackIdentityV3API
import org.openrepose.nodeservice.atomfeed.{AtomFeedListener, AtomFeedService, LifecycleEvents}

import scala.collection.JavaConversions._
import scala.xml.XML

@Named
class OpenStackIdentityV3Filter @Inject()(configurationService: ConfigurationService,
                                          datastoreService: DatastoreService,
                                          atomFeedService: AtomFeedService,
                                          httpClientService: HttpClientService)
  extends Filter with UpdateListener[OpenstackIdentityV3Config] with StrictLogging {

  private final val DEFAULT_CONFIG = "openstack-identity-v3.cfg.xml"

  private val datastore = datastoreService.getDefaultDatastore

  private var initialized = false
  private var configFilename: String = _
  private var httpClient: HttpClientServiceClient = _
  private var openStackIdentityV3Handler: OpenStackIdentityV3Handler = _

  override def init(filterConfig: FilterConfig) {
    configFilename = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    logger.info("Initializing filter using config " + configFilename)
    val xsdURL: URL = getClass.getResource("/META-INF/schema/config/openstack-identity-v3.xsd")
    configurationService.subscribeTo(filterConfig.getFilterName,
      configFilename,
      xsdURL,
      this,
      classOf[OpenstackIdentityV3Config])
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain) {
    if (!isInitialized) {
      logger.error("Filter has not yet initialized... Please check your configuration files and your artifacts directory.")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
    } else {
      val requestWrapper = new HttpServletRequestWrapper(servletRequest.asInstanceOf[HttpServletRequest])
      val response = servletResponse.asInstanceOf[HttpServletResponse]

      val filterAction = openStackIdentityV3Handler.handleRequest(requestWrapper, response)
      filterAction match {
        case FilterAction.RETURN => // no action to take
        case FilterAction.PASS =>
          filterChain.doFilter(requestWrapper, response)
        case FilterAction.PROCESS_RESPONSE =>
          filterChain.doFilter(requestWrapper, response)
          openStackIdentityV3Handler.handleResponse(response)
        case FilterAction.NOT_SET =>
          logger.error("Unexpected internal filter state")
          response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
      }
    }
  }

  def isInitialized = initialized

  override def destroy() {
    configurationService.unsubscribeFrom(configFilename, this)
    CacheInvalidationFeedListener.unregisterFeeds()
  }

  def configurationUpdated(config: OpenstackIdentityV3Config) {
    // This will also un-register any Atom Feeds not present in the new config.
    CacheInvalidationFeedListener.updateFeeds(
      Option(config.getCache).map(_.getAtomFeed.map(_.getId).toSet).getOrElse(Set.empty)
    )

    httpClient = httpClientService.getClient(config.getConnectionPoolId)

    val identityAPI = new OpenStackIdentityV3API(config, datastore, httpClient)
    openStackIdentityV3Handler = new OpenStackIdentityV3Handler(config, identityAPI)
    initialized = true
  }

  object CacheInvalidationFeedListener extends AtomFeedListener {

    private var registeredFeeds = List.empty[RegisteredFeed]

    def unregisterFeeds(): Unit = {
      registeredFeeds synchronized {
        registeredFeeds foreach { feed =>
          atomFeedService.unregisterListener(feed.unique)
        }
      }
    }

    def updateFeeds(feeds: Set[String]): Unit = {
      registeredFeeds synchronized {
        // Unregister the feeds we no longer care about.
        val feedsToUnregister = registeredFeeds.filterNot(regFeed => feeds.contains(regFeed.id))
        feedsToUnregister.foreach(feed => atomFeedService.unregisterListener(feed.unique))

        // Register with only the new feeds we aren't already registered with.
        val registeredFeedIds = registeredFeeds.map(_.id)
        val feedsToRegister = feeds.filterNot(posFeed => registeredFeedIds.contains(posFeed))
        val newRegisteredFeeds = feedsToRegister.map(feedId => RegisteredFeed(feedId, atomFeedService.registerListener(feedId, this)))

        // Update to the still and newly registered feeds.
        registeredFeeds = registeredFeeds.diff(feedsToUnregister) ++ newRegisteredFeeds
      }
    }

    override def onNewAtomEntry(atomEntry: String): Unit = {
      logger.debug("Processing atom feed entry: {}", atomEntry)
      val atomXml = XML.loadString(atomEntry)
      val resourceId = (atomXml \\ "event" \\ "@resourceId").map(_.text).headOption
      if (resourceId.isDefined) {
        val resourceType = (atomXml \\ "event" \\ "@resourceType").map(_.text)
        val authTokens = resourceType.headOption match {
          // User OR Token Revocation Record (TRR) event.
          case Some("USER") | Some("TRR_USER") =>
            val tokens = Option(datastore.get(getUserIdKey(resourceId.get)).asInstanceOf[Set[String]])
            datastore.remove(getUserIdKey(resourceId.get))
            tokens.getOrElse(Set.empty[String])
          case Some("TOKEN") => Set(resourceId.get)
          case _ => Set.empty[String]
        }

        authTokens foreach { authToken =>
          datastore.remove(getGroupsKey(authToken))
          datastore.remove(getTokenKey(authToken))
        }
      }
    }

    override def onLifecycleEvent(event: LifecycleEvents): Unit = {
      logger.debug(s"Received Lifecycle Event: $event")
    }

    case class RegisteredFeed(id: String, unique: String)

  }

}
