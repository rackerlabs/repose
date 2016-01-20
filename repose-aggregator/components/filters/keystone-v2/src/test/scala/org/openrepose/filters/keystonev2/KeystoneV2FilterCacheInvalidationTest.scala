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

import com.mockrunner.mock.web.MockFilterConfig
import org.junit.runner.RunWith
import org.mockito.AdditionalMatchers._
import org.mockito.Matchers.{any => mockitoAny, eq => mockitoEq, _}
import org.mockito.Mockito
import org.mockito.Mockito._
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.{Datastore, DatastoreService}
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClientFactory
import org.openrepose.filters.keystonev2.KeystoneRequestHandler._
import org.openrepose.filters.keystonev2.config.AtomFeedType
import org.openrepose.nodeservice.atomfeed.{AtomFeedListener, AtomFeedService}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class KeystoneV2FilterCacheInvalidationTest extends FunSpec
with MockedAkkaServiceClient
with IdentityResponses
with Matchers
with MockitoSugar
with BeforeAndAfter {

  private val mockConfigurationService = mock[ConfigurationService]
  private val mockAkkaServiceClientFactory = mock[AkkaServiceClientFactory]
  when(mockAkkaServiceClientFactory.newAkkaServiceClient(or(anyString(), isNull.asInstanceOf[String]))).thenReturn(mockAkkaServiceClient)
  private val mockDatastoreService = mock[DatastoreService]
  private val mockDatastore = mock[Datastore]
  Mockito.when(mockDatastoreService.getDefaultDatastore).thenReturn(mockDatastore)

  before {
    reset(mockDatastore)
    reset(mockConfigurationService)
    mockAkkaServiceClient.reset()
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

    def getMockAtomFeedService(): AtomFeedService = {
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
      val filter = new KeystoneV2Filter(mockConfigurationService, mockAkkaServiceClientFactory, mockAtomFeedService, mockDatastoreService)
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
      val filter = new KeystoneV2Filter(mockConfigurationService, mockAkkaServiceClientFactory, mockAtomFeedService, mockDatastoreService)
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
      val filter = new KeystoneV2Filter(mockConfigurationService, mockAkkaServiceClientFactory, mockAtomFeedService, mockDatastoreService)
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
      """.stripMargin)

    val userId = "TestUser123"
    val tokenOne = UUID.randomUUID().toString
    val tokenTwo = UUID.randomUUID().toString
    val filter = new KeystoneV2Filter(mockConfigurationService, mockAkkaServiceClientFactory, mock[AtomFeedService], mockDatastoreService)

    val config = new MockFilterConfig
    filter.init(config)
    filter.KeystoneV2ConfigListener.configurationUpdated(configuration)

    it("removes the token, but doesn't touch the User to Token cache on a TOKEN event") {
      // Based on example from: https://github.com/rackerlabs/standard-usage-schemas/blob/b43ed83/message_samples/identity/xml/
      filter.CacheInvalidationFeedListener.onNewAtomEntry(
        s"""<event xmlns="http://docs.rackspace.com/core/event"
            |      xmlns:id="http://docs.rackspace.com/event/identity/trr/user"
            |      id="12345678-1234-5678-9012-123456789012"
            |      version="2"
            |      resourceId="$tokenOne"
            |      eventTime="1970-01-01T00:00:00Z"
            |      type="DELETE"
            |      region="DFW"
            |      dataCenter="DFW1">
            |    <id:product serviceCode="CloudIdentity"
            |                version="1"
            |                resourceType="TOKEN"
            |    />
            |</event>
            |
        """.stripMargin
      )

      verify(mockDatastore).remove(s"$TOKEN_KEY_PREFIX$tokenOne")
      verify(mockDatastore).remove(s"$ENDPOINTS_KEY_PREFIX$tokenOne")
      verify(mockDatastore).remove(s"$GROUPS_KEY_PREFIX$tokenOne")
      verify(mockDatastore, never()).put(mockitoAny(), mockitoAny(), mockitoEq(600), mockitoEq(TimeUnit.SECONDS))
      mockAkkaServiceClient.validate()
    }

    List("USER", "TRR_USER").foreach { resourceType =>
      it(s"removes the User to Token cache along with the token cache on a $resourceType event") {
        when(mockDatastore.get(s"$USER_ID_KEY_PREFIX$userId")).thenReturn(Vector(tokenOne, tokenTwo), null)
        // Based on example from: https://github.com/rackerlabs/standard-usage-schemas/blob/b43ed83/message_samples/identity/xml/
        filter.CacheInvalidationFeedListener.onNewAtomEntry(
          s"""<event xmlns="http://docs.rackspace.com/core/event"
              |      xmlns:id="http://docs.rackspace.com/event/identity/token"
              |      version="1"
              |      id="12345678-1234-5678-9012-123456789012"
              |      resourceId="$userId"
              |      eventTime="1970-01-01T00:00:00Z"
              |      type="DELETE"
              |      region="DFW"
              |      dataCenter="DFW1">
              |    <id:product serviceCode="CloudIdentity"
              |                version="1"
              |                resourceType="$resourceType"
              |    />
              |</event>
        """.stripMargin
        )

        verify(mockDatastore).remove(s"$USER_ID_KEY_PREFIX$userId")

        verify(mockDatastore).remove(s"$TOKEN_KEY_PREFIX$tokenOne")
        verify(mockDatastore).remove(s"$ENDPOINTS_KEY_PREFIX$tokenOne")
        verify(mockDatastore).remove(s"$GROUPS_KEY_PREFIX$tokenOne")

        verify(mockDatastore).remove(s"$TOKEN_KEY_PREFIX$tokenTwo")
        verify(mockDatastore).remove(s"$ENDPOINTS_KEY_PREFIX$tokenTwo")
        verify(mockDatastore).remove(s"$GROUPS_KEY_PREFIX$tokenTwo")
        mockAkkaServiceClient.validate()
      }
    }

    it("doesn't remove anything on any other event") {
      // Based on example from: https://github.com/rackerlabs/standard-usage-schemas/blob/b43ed83/message_samples/identity/xml/
      filter.CacheInvalidationFeedListener.onNewAtomEntry(
        s"""<event xmlns="http://docs.rackspace.com/core/event"
            |      xmlns:id="http://docs.rackspace.com/event/identity/trr/user"
            |      id="12345678-1234-5678-9012-123456789012"
            |      version="2"
            |      resourceId="$tokenOne"
            |      eventTime="1970-01-01T00:00:00Z"
            |      type="DELETE"
            |      region="DFW"
            |      dataCenter="DFW1">
            |    <id:product serviceCode="CloudIdentity"
            |                version="1"
            |                resourceType="TEST"
            |    />
            |</event>
            |
        """.stripMargin
      )

      verify(mockDatastore, never()).remove(s"$USER_ID_KEY_PREFIX$userId")

      verify(mockDatastore, never()).remove(s"$TOKEN_KEY_PREFIX$tokenOne")
      verify(mockDatastore, never()).remove(s"$ENDPOINTS_KEY_PREFIX$tokenOne")
      verify(mockDatastore, never()).remove(s"$GROUPS_KEY_PREFIX$tokenOne")

      verify(mockDatastore, never()).remove(s"$TOKEN_KEY_PREFIX$tokenTwo")
      verify(mockDatastore, never()).remove(s"$ENDPOINTS_KEY_PREFIX$tokenTwo")
      verify(mockDatastore, never()).remove(s"$GROUPS_KEY_PREFIX$tokenTwo")
      mockAkkaServiceClient.validate()
    }
  }
}
