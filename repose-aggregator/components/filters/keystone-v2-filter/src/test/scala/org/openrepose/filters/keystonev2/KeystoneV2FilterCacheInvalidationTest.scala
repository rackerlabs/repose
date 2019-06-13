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
package org.openrepose.filters.keystonev2

import java.util.UUID
import java.util.concurrent.TimeUnit

import org.junit.runner.RunWith
import org.mockito.AdditionalMatchers._
import org.mockito.Matchers.{any => mockitoAny, eq => mockitoEq, _}
import org.mockito.Mockito
import org.mockito.Mockito._
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.{Datastore, DatastoreService}
import org.openrepose.core.services.httpclient.{HttpClientService, HttpClientServiceClient}
import org.openrepose.filters.keystonev2.KeystoneRequestHandler._
import org.openrepose.filters.keystonev2.config.AtomFeedType
import org.openrepose.nodeservice.atomfeed.{AtomFeedListener, AtomFeedService}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.mock.web.MockFilterConfig

@RunWith(classOf[JUnitRunner])
class KeystoneV2FilterCacheInvalidationTest extends FunSpec
with IdentityResponses
with Matchers
with MockitoSugar
with BeforeAndAfterEach {

  private val mockConfigurationService = mock[ConfigurationService]
  private val mockHttpClient = mock[HttpClientServiceClient]
  private val mockHttpClientService = mock[HttpClientService]
  when(mockHttpClientService.getClient(or(anyString(), isNull.asInstanceOf[String]))).thenReturn(mockHttpClient)
  private val mockDatastoreService = mock[DatastoreService]
  private val mockDatastore = mock[Datastore]
  Mockito.when(mockDatastoreService.getDefaultDatastore).thenReturn(mockDatastore)

  override def beforeEach(): Unit = {
    reset(mockDatastore)
    reset(mockConfigurationService)
    reset(mockHttpClient)
  }

  describe("The Atom feed (un)registration") {
    val atomFeedType1 = new AtomFeedType
    val atomFeedType2 = new AtomFeedType
    val atomFeedType3 = new AtomFeedType
    val atomFeedType4 = new AtomFeedType
    val atomFeedType5 = new AtomFeedType
    val atomFeedId1 = "AtomFeedId1"
    val atomFeedId2 = "AtomFeedId2"
    val atomFeedId3 = "AtomFeedId3"
    val atomFeedId4 = "AtomFeedId4"
    val atomFeedId5 = "AtomFeedId5"
    val atomFeedPre = "SVC-"
    atomFeedType1.setId(atomFeedId1)
    atomFeedType2.setId(atomFeedId2)
    atomFeedType3.setId(atomFeedId3)
    atomFeedType4.setId(atomFeedId4)
    atomFeedType5.setId(atomFeedId5)

    def getMockAtomFeedService: AtomFeedService = {
      val mockAtomFeedService = mock[AtomFeedService]
      when(mockAtomFeedService.registerListener(mockitoEq(atomFeedId1), mockitoAny[AtomFeedListener])).thenReturn(atomFeedPre + atomFeedId1)
      when(mockAtomFeedService.registerListener(mockitoEq(atomFeedId2), mockitoAny[AtomFeedListener])).thenReturn(atomFeedPre + atomFeedId2)
      when(mockAtomFeedService.registerListener(mockitoEq(atomFeedId3), mockitoAny[AtomFeedListener])).thenReturn(atomFeedPre + atomFeedId3)
      when(mockAtomFeedService.registerListener(mockitoEq(atomFeedId4), mockitoAny[AtomFeedListener])).thenReturn(atomFeedPre + atomFeedId4)
      when(mockAtomFeedService.registerListener(mockitoEq(atomFeedId5), mockitoAny[AtomFeedListener])).thenReturn(atomFeedPre + atomFeedId5)
      mockAtomFeedService
    }

    it("register the feeds") {
      val mockAtomFeedService = getMockAtomFeedService
      val filter = new KeystoneV2Filter(mockConfigurationService, mockHttpClientService, mockAtomFeedService, mockDatastoreService)
      val feedsList = List(atomFeedType1, atomFeedType2, atomFeedType3, atomFeedType4, atomFeedType5)

      filter.CacheInvalidationFeedListener.registerFeeds(feedsList)

      verify(mockAtomFeedService, never()).unregisterListener(anyString())
      verify(mockAtomFeedService).registerListener(mockitoEq(atomFeedId1), mockitoAny[AtomFeedListener])
      verify(mockAtomFeedService).registerListener(mockitoEq(atomFeedId2), mockitoAny[AtomFeedListener])
      verify(mockAtomFeedService).registerListener(mockitoEq(atomFeedId3), mockitoAny[AtomFeedListener])
      verify(mockAtomFeedService).registerListener(mockitoEq(atomFeedId4), mockitoAny[AtomFeedListener])
      verify(mockAtomFeedService).registerListener(mockitoEq(atomFeedId5), mockitoAny[AtomFeedListener])
    }

    it("register new feeds while leaving old feeds") {
      val mockAtomFeedService = getMockAtomFeedService
      val filter = new KeystoneV2Filter(mockConfigurationService, mockHttpClientService, mockAtomFeedService, mockDatastoreService)
      val oldList = List(atomFeedType1, atomFeedType2, atomFeedType3)
      val newList = List(atomFeedType4, atomFeedType5)

      filter.CacheInvalidationFeedListener.registerFeeds(oldList)

      verify(mockAtomFeedService, never()).unregisterListener(anyString())
      verify(mockAtomFeedService).registerListener(mockitoEq(atomFeedId1), mockitoAny[AtomFeedListener])
      verify(mockAtomFeedService).registerListener(mockitoEq(atomFeedId2), mockitoAny[AtomFeedListener])
      verify(mockAtomFeedService).registerListener(mockitoEq(atomFeedId3), mockitoAny[AtomFeedListener])
      verify(mockAtomFeedService, never()).registerListener(mockitoEq(atomFeedId4), mockitoAny[AtomFeedListener])
      verify(mockAtomFeedService, never()).registerListener(mockitoEq(atomFeedId5), mockitoAny[AtomFeedListener])

      filter.CacheInvalidationFeedListener.registerFeeds(oldList ++ newList)

      verify(mockAtomFeedService, never()).unregisterListener(anyString())
      verify(mockAtomFeedService).registerListener(mockitoEq(atomFeedId4), mockitoAny[AtomFeedListener])
      verify(mockAtomFeedService).registerListener(mockitoEq(atomFeedId5), mockitoAny[AtomFeedListener])
    }

    it("unregister unwanted feeds and register new feeds") {
      val mockAtomFeedService = getMockAtomFeedService
      val filter = new KeystoneV2Filter(mockConfigurationService, mockHttpClientService, mockAtomFeedService, mockDatastoreService)
      val oldList = List(atomFeedType1, atomFeedType2, atomFeedType3)
      val newList = List(atomFeedType3, atomFeedType4, atomFeedType5)

      filter.CacheInvalidationFeedListener.registerFeeds(oldList)

      verify(mockAtomFeedService, never()).unregisterListener(anyString())
      verify(mockAtomFeedService).registerListener(mockitoEq(atomFeedId1), mockitoAny[AtomFeedListener])
      verify(mockAtomFeedService).registerListener(mockitoEq(atomFeedId2), mockitoAny[AtomFeedListener])
      verify(mockAtomFeedService).registerListener(mockitoEq(atomFeedId3), mockitoAny[AtomFeedListener])
      verify(mockAtomFeedService, never()).registerListener(mockitoEq(atomFeedId4), mockitoAny[AtomFeedListener])
      verify(mockAtomFeedService, never()).registerListener(mockitoEq(atomFeedId5), mockitoAny[AtomFeedListener])

      filter.CacheInvalidationFeedListener.registerFeeds(newList)

      verify(mockAtomFeedService).unregisterListener(atomFeedPre + atomFeedId1)
      verify(mockAtomFeedService).unregisterListener(atomFeedPre + atomFeedId2)
      verify(mockAtomFeedService, times(1)).registerListener(mockitoEq(atomFeedId3), mockitoAny[AtomFeedListener])
      verify(mockAtomFeedService).registerListener(mockitoEq(atomFeedId4), mockitoAny[AtomFeedListener])
      verify(mockAtomFeedService).registerListener(mockitoEq(atomFeedId5), mockitoAny[AtomFeedListener])
    }
  }

  describe("Configured with cache invalidation via an Atom feed") {
    def configuration = Marshaller.keystoneV2ConfigFromString(
      """<?xml version="1.0" encoding="UTF-8"?>
        |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
        |    <identity-service
        |            uri="https://some.identity.com"
        |    />
        |    <cache>
        |        <atom-feed id="some-feed"/>
        |    </cache>
        |</keystone-v2>
        | """.stripMargin
    )

    val userId = "TestUser123"
    val tokenOne = UUID.randomUUID().toString
    val tokenTwo = UUID.randomUUID().toString
    val filter = new KeystoneV2Filter(mockConfigurationService, mockHttpClientService, mock[AtomFeedService], mockDatastoreService)

    val config = new MockFilterConfig
    filter.init(config)
    filter.configurationUpdated(configuration)

    it("removes the token, but doesn't touch the User to Token cache on a TOKEN event") {
      // This was taken from: https://github.com/rackerlabs/standard-usage-schemas/blob/master/message_samples/identity/xml/cloudidentity-token-token-delete-v1-response.xml
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

      verify(mockDatastore).remove(s"$TOKEN_KEY_PREFIX$tokenOne")
      verify(mockDatastore).remove(s"$ENDPOINTS_KEY_PREFIX$tokenOne")
      verify(mockDatastore).remove(s"$GROUPS_KEY_PREFIX$tokenOne")
      verify(mockDatastore, never()).put(mockitoAny(), mockitoAny(), mockitoEq(600), mockitoEq(TimeUnit.SECONDS))
    }

    List("USER", "TRR_USER").foreach { resourceType =>
      it(s"removes the User to Token cache along with the token cache on a $resourceType event") {
        when(mockDatastore.get(s"$USER_ID_KEY_PREFIX$userId")).thenReturn(Set(tokenOne, tokenTwo), null)
        // This was taken from: https://github.com/rackerlabs/standard-usage-schemas/blob/master/message_samples/identity/xml/cloudidentity-user-trr_user-delete-v1-response.xml
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

        verify(mockDatastore).remove(s"$USER_ID_KEY_PREFIX$userId")

        verify(mockDatastore).remove(s"$TOKEN_KEY_PREFIX$tokenOne")
        verify(mockDatastore).remove(s"$ENDPOINTS_KEY_PREFIX$tokenOne")
        verify(mockDatastore).remove(s"$GROUPS_KEY_PREFIX$tokenOne")

        verify(mockDatastore).remove(s"$TOKEN_KEY_PREFIX$tokenTwo")
        verify(mockDatastore).remove(s"$ENDPOINTS_KEY_PREFIX$tokenTwo")
        verify(mockDatastore).remove(s"$GROUPS_KEY_PREFIX$tokenTwo")
      }
    }

    it("doesn't remove anything on any other event") {
      // This was taken from: https://github.com/rackerlabs/standard-usage-schemas/blob/master/message_samples/identity/xml/cloudidentity-token-token-delete-v1-response.xml
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

      verify(mockDatastore, never()).remove(s"$USER_ID_KEY_PREFIX$userId")

      verify(mockDatastore, never()).remove(s"$TOKEN_KEY_PREFIX$tokenOne")
      verify(mockDatastore, never()).remove(s"$ENDPOINTS_KEY_PREFIX$tokenOne")
      verify(mockDatastore, never()).remove(s"$GROUPS_KEY_PREFIX$tokenOne")

      verify(mockDatastore, never()).remove(s"$TOKEN_KEY_PREFIX$tokenTwo")
      verify(mockDatastore, never()).remove(s"$ENDPOINTS_KEY_PREFIX$tokenTwo")
      verify(mockDatastore, never()).remove(s"$GROUPS_KEY_PREFIX$tokenTwo")
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

      verify(mockDatastore, never()).remove(anyString())
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

      verify(mockDatastore, never()).remove(anyString())
    }
  }
}
