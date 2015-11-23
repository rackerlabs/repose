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

import org.junit.runner.RunWith
import org.mockito.AdditionalMatchers._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.openrepose.core.services.datastore.DatastoreService
import org.openrepose.core.services.serviceclient.akka.{AkkaServiceClientFactory, AkkaServiceClient}
import org.openrepose.filters.openstackidentityv3.config.{DelegatingType, OpenstackIdentityService, OpenstackIdentityV3Config}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class OpenStackIdentityV3HandlerFactoryTest extends FunSpec with BeforeAndAfter with Matchers with MockitoSugar {

  val mockAkkaServiceClient = mock[AkkaServiceClient]
  val mockAkkaServiceClientFactory = mock[AkkaServiceClientFactory]
  val mockDatastoreService = mock[DatastoreService]
  var handlerFactory: OpenStackIdentityV3HandlerFactory = _

  before {
    reset(mockAkkaServiceClientFactory)
    when(mockDatastoreService.getDefaultDatastore).thenReturn(null)
    when(mockAkkaServiceClientFactory.newAkkaServiceClient(or(anyString(), isNull.asInstanceOf[String]))).thenReturn(mockAkkaServiceClient)

    handlerFactory = new OpenStackIdentityV3HandlerFactory(mockAkkaServiceClientFactory, mockDatastoreService)
  }

  describe("buildHandler") {
    it("should return an OpenStack Identity v3 handler") {
      val identityService = new OpenstackIdentityService()
      identityService.setUri("")

      val config = new OpenstackIdentityV3Config()
      val delegating = new DelegatingType()
      delegating.setQuality(0.5)
      config.setOpenstackIdentityService(identityService)
      config.setTokenCacheTimeout(0)
      config.setGroupsCacheTimeout(0)
      config.setCacheOffset(0)
      config.setDelegating(delegating)

      handlerFactory.configurationUpdated(config)
      handlerFactory.buildHandler shouldBe a[OpenStackIdentityV3Handler]
    }
  }

  describe("getListeners") {
    it("should return a map of listeners one of which listens to the OpenStack Identity configuration file") {
      val listeners = handlerFactory.getListeners

      listeners should have size 1
      listeners should contain key classOf[OpenstackIdentityV3Config]
    }
  }

  describe("configurationUpdated") {
    it("should use the akka service client factory to get an instance with the configured connection pool id") {
      val connectionPoolId = "some_conn_pool_id"
      val identityService = new OpenstackIdentityService()
      identityService.setUri("")
      val config = new OpenstackIdentityV3Config()
      config.setOpenstackIdentityService(identityService)
      config.setConnectionPoolId(connectionPoolId)

      handlerFactory.configurationUpdated(config)

      verify(mockAkkaServiceClientFactory).newAkkaServiceClient(connectionPoolId)
    }

    it("should destroy the previous akka service client") {
      val firstAkkaServiceClient = mock[AkkaServiceClient]
      val secondAkkaServiceClient = mock[AkkaServiceClient]
      when(mockAkkaServiceClientFactory.newAkkaServiceClient(or(anyString(), isNull.asInstanceOf[String])))
        .thenReturn(firstAkkaServiceClient)
        .thenReturn(secondAkkaServiceClient)

      val identityService = new OpenstackIdentityService()
      identityService.setUri("")
      val config = new OpenstackIdentityV3Config()
      config.setOpenstackIdentityService(identityService)

      handlerFactory.configurationUpdated(config)
      handlerFactory.configurationUpdated(config)

      verify(mockAkkaServiceClientFactory, times(2)).newAkkaServiceClient(or(anyString(), isNull.asInstanceOf[String]))
      verify(firstAkkaServiceClient, times(1)).destroy()
      verify(secondAkkaServiceClient, never()).destroy()
    }
  }

  describe("destroy") {
    it("should destroy the akka service client") {
      val identityService = new OpenstackIdentityService()
      identityService.setUri("")
      val config = new OpenstackIdentityV3Config()
      config.setOpenstackIdentityService(identityService)
      handlerFactory.configurationUpdated(config)

      handlerFactory.destroy()

      verify(mockAkkaServiceClient).destroy()
    }
  }
}
