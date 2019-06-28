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

import java.util.UUID

import org.junit.runner.RunWith
import org.mockito.AdditionalMatchers._
import org.mockito.Mockito._
import org.mockito.{Matchers => MockitoMatchers}
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.{Datastore, DatastoreService}
import org.openrepose.core.services.httpclient.{HttpClientService, HttpClientServiceClient}
import org.openrepose.filters.openstackidentityv3.config.{OpenstackIdentityService, OpenstackIdentityV3Config}
import org.openrepose.filters.openstackidentityv3.utilities.Cache._
import org.openrepose.nodeservice.atomfeed.{AtomFeedListener, AtomFeedService}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

import scala.collection.immutable.HashSet

@RunWith(classOf[JUnitRunner])
class OpenStackIdentityV3FilterTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {

  val mockConfigurationService = mock[ConfigurationService]
  val mockHttpClientService = mock[HttpClientService]
  val mockHttpClient = mock[HttpClientServiceClient]
  val mockDatastoreService = mock[DatastoreService]
  val mockDatastore = mock[Datastore]
  val mockAtomFeedService = mock[AtomFeedService]

  var filter: OpenStackIdentityV3Filter = _

  val idPrefix = "pre-"
  val atomFeedIdOne = "AtomFeedId1"
  val atomFeedIdTwo = "AtomFeedId2"
  val atomFeedIdThree = "AtomFeedId3"
  val userId = "some-user"
  val tokenOne = UUID.randomUUID.toString
  val tokenTwo = UUID.randomUUID.toString
  val tokenThree = UUID.randomUUID.toString

  override def beforeEach() = {
    reset(mockConfigurationService)
    reset(mockHttpClientService)
    reset(mockDatastore)
    reset(mockAtomFeedService)

    when(mockDatastoreService.getDefaultDatastore).thenReturn(mockDatastore)
    when(mockHttpClientService.getClient(or(MockitoMatchers.anyString(), MockitoMatchers.isNull.asInstanceOf[String]))).thenReturn(mockHttpClient)
    when(mockAtomFeedService.registerListener(MockitoMatchers.eq(atomFeedIdOne), MockitoMatchers.any[AtomFeedListener]))
      .thenReturn(idPrefix + atomFeedIdOne)
    when(mockAtomFeedService.registerListener(MockitoMatchers.eq(atomFeedIdTwo), MockitoMatchers.any[AtomFeedListener]))
      .thenReturn(idPrefix + atomFeedIdTwo)
    when(mockAtomFeedService.registerListener(MockitoMatchers.eq(atomFeedIdThree), MockitoMatchers.any[AtomFeedListener]))
      .thenReturn(idPrefix + atomFeedIdThree)

    filter = new OpenStackIdentityV3Filter(mockConfigurationService,
      mockDatastoreService,
      mockAtomFeedService,
      mockHttpClientService)
  }

  describe("configurationUpdated") {
    it("should use the akka service client factory to get an instance with the configured connection pool id") {
      val connectionPoolId = "some_conn_pool_id"
      val identityService = new OpenstackIdentityService()
      identityService.setUri("")
      val config = new OpenstackIdentityV3Config()
      config.setOpenstackIdentityService(identityService)
      config.setConnectionPoolId(connectionPoolId)

      filter.configurationUpdated(config)

      verify(mockHttpClientService).getClient(connectionPoolId)
    }

    it("should obtain a client client") {
      val firstHttpClient = mock[HttpClientServiceClient]
      val secondHttpClient = mock[HttpClientServiceClient]
      when(mockHttpClientService.getClient(or(MockitoMatchers.anyString(), MockitoMatchers.isNull.asInstanceOf[String])))
        .thenReturn(firstHttpClient)
        .thenReturn(secondHttpClient)

      val identityService = new OpenstackIdentityService()
      identityService.setUri("")
      val config = new OpenstackIdentityV3Config()
      config.setOpenstackIdentityService(identityService)

      filter.configurationUpdated(config)
      filter.configurationUpdated(config)

      verify(mockHttpClientService, times(2)).getClient(or(MockitoMatchers.anyString(), MockitoMatchers.isNull.asInstanceOf[String]))
    }
  }

  describe("atom feed cache invalidation") {
    it("register the feeds") {
      val feedIds = Set(atomFeedIdOne, atomFeedIdTwo, atomFeedIdThree)

      filter.CacheInvalidationFeedListener.updateFeeds(feedIds)

      verify(mockAtomFeedService, never).unregisterListener(MockitoMatchers.anyString())
      verify(mockAtomFeedService).registerListener(MockitoMatchers.eq(atomFeedIdOne), MockitoMatchers.any[AtomFeedListener])
      verify(mockAtomFeedService).registerListener(MockitoMatchers.eq(atomFeedIdTwo), MockitoMatchers.any[AtomFeedListener])
      verify(mockAtomFeedService).registerListener(MockitoMatchers.eq(atomFeedIdThree), MockitoMatchers.any[AtomFeedListener])
    }

    it("register new feeds while leaving old feeds") {
      val oldFeedIds = Set(atomFeedIdOne)
      val newFeedIds = Set(atomFeedIdTwo, atomFeedIdThree)

      filter.CacheInvalidationFeedListener.updateFeeds(oldFeedIds)

      verify(mockAtomFeedService, never).unregisterListener(MockitoMatchers.anyString())
      verify(mockAtomFeedService).registerListener(MockitoMatchers.eq(atomFeedIdOne), MockitoMatchers.any[AtomFeedListener])
      verify(mockAtomFeedService, never).registerListener(MockitoMatchers.eq(atomFeedIdTwo), MockitoMatchers.any[AtomFeedListener])
      verify(mockAtomFeedService, never).registerListener(MockitoMatchers.eq(atomFeedIdThree), MockitoMatchers.any[AtomFeedListener])

      filter.CacheInvalidationFeedListener.updateFeeds(oldFeedIds ++ newFeedIds)

      verify(mockAtomFeedService, never).unregisterListener(MockitoMatchers.anyString())
      verify(mockAtomFeedService).registerListener(MockitoMatchers.eq(atomFeedIdOne), MockitoMatchers.any[AtomFeedListener])
      verify(mockAtomFeedService).registerListener(MockitoMatchers.eq(atomFeedIdTwo), MockitoMatchers.any[AtomFeedListener])
      verify(mockAtomFeedService).registerListener(MockitoMatchers.eq(atomFeedIdThree), MockitoMatchers.any[AtomFeedListener])
    }

    it("unregister unwanted feeds and register new feeds") {
      val oldFeedIds = Set(atomFeedIdOne, atomFeedIdTwo)
      val newFeedIds = Set(atomFeedIdTwo, atomFeedIdThree)

      filter.CacheInvalidationFeedListener.updateFeeds(oldFeedIds)

      verify(mockAtomFeedService, never).unregisterListener(MockitoMatchers.anyString())
      verify(mockAtomFeedService).registerListener(MockitoMatchers.eq(atomFeedIdOne), MockitoMatchers.any[AtomFeedListener])
      verify(mockAtomFeedService).registerListener(MockitoMatchers.eq(atomFeedIdTwo), MockitoMatchers.any[AtomFeedListener])
      verify(mockAtomFeedService, never).registerListener(MockitoMatchers.eq(atomFeedIdThree), MockitoMatchers.any[AtomFeedListener])

      filter.CacheInvalidationFeedListener.updateFeeds(newFeedIds)

      verify(mockAtomFeedService).unregisterListener(idPrefix + atomFeedIdOne)
      verify(mockAtomFeedService).registerListener(MockitoMatchers.eq(atomFeedIdTwo), MockitoMatchers.any[AtomFeedListener])
      verify(mockAtomFeedService).registerListener(MockitoMatchers.eq(atomFeedIdThree), MockitoMatchers.any[AtomFeedListener])
    }

    it("unregisters feeds on destruction") {
      val feedIds = Set(atomFeedIdOne, atomFeedIdTwo, atomFeedIdThree)

      filter.CacheInvalidationFeedListener.updateFeeds(feedIds)
      filter.destroy()

      verify(mockAtomFeedService).unregisterListener(idPrefix + atomFeedIdOne)
      verify(mockAtomFeedService).unregisterListener(idPrefix + atomFeedIdTwo)
      verify(mockAtomFeedService).unregisterListener(idPrefix + atomFeedIdThree)
    }

    it("removes the token, but doesn't touch the User to Token cache on a TOKEN event") {
      filter.CacheInvalidationFeedListener.onNewAtomEntry(
        s"""<?xml version="1.0" encoding="UTF-8"?>
            |<atom:entry xmlns:atom="http://www.w3.org/2005/Atom"
            |            xmlns:xsd="http://www.w3.org/2001/XMLSchema"
            |            xmlns="http://www.w3.org/2001/XMLSchema">
            |   <atom:id>urn:uuid:e53d007a-fc23-11e1-975c-cfa6b29bb814</atom:id>
            |   <atom:category term="rgn:DFW"/>
            |   <atom:category term="dc:DFW1"/>
            |   <atom:category term="rid:4a2b42f4-6c63-11e1-815b-7fcbcf67f549"/>
            |   <atom:category term="cloudidentity.token.token.delete"/>
            |   <atom:category term="type:cloudidentity.token.token.delete"/>
            |   <atom:title>CloudIdentity</atom:title>
            |   <atom:content type="application/xml">
            |      <event xmlns="http://docs.rackspace.com/core/event"
            |             xmlns:sample="http://docs.rackspace.com/event/identity/token"
            |             id="e53d007a-fc23-11e1-975c-cfa6b29bb814"
            |             version="1"
            |             tenantId="5914283"
            |             resourceId="$tokenOne"
            |             eventTime="2013-03-15T11:51:11Z"
            |             type="DELETE"
            |             dataCenter="DFW1"
            |             region="DFW">
            |          <sample:product serviceCode="CloudIdentity" version="1" resourceType="TOKEN"
            |                          tenants="1234 tenant2 3882"/>
            |      </event>
            |   </atom:content>
            |   <atom:link href="https://ord.feeds.api.rackspacecloud.com/identity/events/entries/urn:uuid:e53d007a-fc23-11e1-975c-cfa6b29bb814"
            |              rel="self"/>
            |   <atom:updated>2013-03-01T19:42:35.507Z</atom:updated>
            |   <atom:published>2013-03-01T19:42:35.507</atom:published>
            |</atom:entry>
            |""".stripMargin
      )

      verify(mockDatastore).remove(getTokenKey(tokenOne))
      verify(mockDatastore).remove(getGroupsKey(tokenOne))
      verify(mockDatastore, never).remove(MockitoMatchers.startsWith(UserIdKeyPrefix))
    }

    List("USER", "TRR_USER") foreach { resourceType =>
      it(s"removes the User to Token cache along with the token cache on a $resourceType event") {
        when(mockDatastore.get(getUserIdKey(userId))).thenReturn(HashSet(tokenOne, tokenTwo), null)

        filter.CacheInvalidationFeedListener.onNewAtomEntry(
          s"""<?xml version="1.0" encoding="UTF-8"?>
              |<atom:entry xmlns:atom="http://www.w3.org/2005/Atom"
              |            xmlns:xsd="http://www.w3.org/2001/XMLSchema"
              |            xmlns="http://www.w3.org/2001/XMLSchema">
              |   <atom:id>urn:uuid:e53d007a-fc23-11e1-975c-cfa6b29bb814</atom:id>
              |   <atom:category term="rgn:DFW"/>
              |   <atom:category term="dc:DFW1"/>
              |   <atom:category term="rid:4a2b42f4-6c63-11e1-815b-7fcbcf67f549"/>
              |   <atom:category term="cloudidentity.user.trr_user.delete"/>
              |   <atom:category term="type:cloudidentity.user.trr_user.delete"/>
              |   <atom:title>CloudIdentity</atom:title>
              |   <atom:content type="application/xml">
              |      <event xmlns="http://docs.rackspace.com/core/event"
              |             xmlns:sample="http://docs.rackspace.com/event/identity/trr/user"
              |             id="e53d007a-fc23-11e1-975c-cfa6b29bb814"
              |             version="2"
              |             resourceId="$userId"
              |             eventTime="2013-03-15T11:51:11Z"
              |             type="DELETE"
              |             dataCenter="DFW1"
              |             region="DFW">
              |         <sample:product serviceCode="CloudIdentity"
              |                         version="1"
              |                         resourceType="$resourceType"
              |                         tokenCreationDate="2013-09-26T15:32:00Z">
              |            <sample:tokenAuthenticatedBy values="PASSWORD APIKEY"/>
              |         </sample:product>
              |      </event>
              |   </atom:content>
              |   <atom:link href="https://ord.feeds.api.rackspacecloud.com/identity/events/entries/urn:uuid:e53d007a-fc23-11e1-975c-cfa6b29bb814"
              |              rel="self"/>
              |   <atom:updated>2013-03-01T19:42:35.507Z</atom:updated>
              |   <atom:published>2013-03-01T19:42:35.507</atom:published>
              |</atom:entry>
              |""".stripMargin
        )

        verify(mockDatastore).remove(getUserIdKey(userId))

        verify(mockDatastore).remove(getTokenKey(tokenOne))
        verify(mockDatastore).remove(getGroupsKey(tokenOne))

        verify(mockDatastore).remove(getTokenKey(tokenTwo))
        verify(mockDatastore).remove(getGroupsKey(tokenTwo))
      }
    }

    it("doesn't remove anything from the cache on unknown events") {
      filter.CacheInvalidationFeedListener.onNewAtomEntry(
        s"""<?xml version="1.0" encoding="UTF-8"?>
            |<atom:entry xmlns:atom="http://www.w3.org/2005/Atom"
            |            xmlns:xsd="http://www.w3.org/2001/XMLSchema"
            |            xmlns="http://www.w3.org/2001/XMLSchema">
            |   <atom:id>urn:uuid:e53d007a-fc23-11e1-975c-cfa6b29bb814</atom:id>
            |   <atom:category term="rgn:DFW"/>
            |   <atom:category term="dc:DFW1"/>
            |   <atom:category term="rid:4a2b42f4-6c63-11e1-815b-7fcbcf67f549"/>
            |   <atom:category term="cloudidentity.token.token.delete"/>
            |   <atom:category term="type:cloudidentity.token.token.delete"/>
            |   <atom:title>CloudIdentity</atom:title>
            |   <atom:content type="application/xml">
            |      <event xmlns="http://docs.rackspace.com/core/event"
            |             xmlns:sample="http://docs.rackspace.com/event/identity/token"
            |             id="e53d007a-fc23-11e1-975c-cfa6b29bb814"
            |             version="1"
            |             tenantId="5914283"
            |             resourceId="$tokenOne"
            |             eventTime="2013-03-15T11:51:11Z"
            |             type="DELETE"
            |             dataCenter="DFW1"
            |             region="DFW">
            |          <sample:product serviceCode="CloudIdentity" version="1" resourceType="UNKNOWN"
            |                          tenants="1234 tenant2 3882"/>
            |      </event>
            |   </atom:content>
            |   <atom:link href="https://ord.feeds.api.rackspacecloud.com/identity/events/entries/urn:uuid:e53d007a-fc23-11e1-975c-cfa6b29bb814"
            |              rel="self"/>
            |   <atom:updated>2013-03-01T19:42:35.507Z</atom:updated>
            |   <atom:published>2013-03-01T19:42:35.507</atom:published>
            |</atom:entry>
            |""".stripMargin
      )

      verify(mockDatastore, never).remove(getUserIdKey(userId))

      verify(mockDatastore, never).remove(getTokenKey(tokenOne))
      verify(mockDatastore, never).remove(getGroupsKey(tokenOne))

      verify(mockDatastore, never).remove(getTokenKey(tokenTwo))
      verify(mockDatastore, never).remove(getGroupsKey(tokenTwo))
    }

    it("doesn't have any problems with bogus content") {
      filter.CacheInvalidationFeedListener.onNewAtomEntry(
        s"""<?xml version="1.0" encoding="UTF-8"?>
            |<atom:entry>
            |   <atom:content type="application/bad">
            |      <event id="bogus"/>
            |   </atom:content>
            |</atom:entry>
            |""".stripMargin
      )

      verify(mockDatastore, never).remove(MockitoMatchers.anyString)
    }

    it("doesn't have any problems with corrupted content") {
      filter.CacheInvalidationFeedListener.onNewAtomEntry(
        s"""<?xml version="1.0" encoding="UTF-8"?>
            |<atom:entry>
            |   <atom:content type="application/bad">
            |      <event xmlns="http://docs.rackspace.com/core/event"
            |             xmlns:sample="http://docs.rackspace.com/event/identity/token"
            |             id="bogus"
            |             resourceId="corrupted">
            |          <sample:product serviceCode="CloudIdentity"/>
            |      </event>
            |   </atom:content>
            |</atom:entry>
            |""".stripMargin
      )

      verify(mockDatastore, never).remove(MockitoMatchers.anyString)
    }
  }
}
