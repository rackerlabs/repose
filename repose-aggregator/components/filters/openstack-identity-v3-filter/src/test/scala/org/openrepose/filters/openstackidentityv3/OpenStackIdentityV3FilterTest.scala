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
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.DatastoreService
import org.openrepose.core.services.httpclient.HttpClientService
import org.openrepose.core.services.serviceclient.akka.{AkkaServiceClient, AkkaServiceClientFactory}
import org.openrepose.filters.openstackidentityv3.config.{OpenstackIdentityService, OpenstackIdentityV3Config}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class OpenStackIdentityV3FilterTest extends FunSpec with BeforeAndAfter with Matchers with MockitoSugar {

  val mockConfigurationService = mock[ConfigurationService]
  val mockHttpClientService = mock[HttpClientService]
  val mockAkkaServiceClient = mock[AkkaServiceClient]
  val mockAkkaServiceClientFactory = mock[AkkaServiceClientFactory]
  val mockDatastoreService = mock[DatastoreService]

  var filter: OpenStackIdentityV3Filter = _

  before {
    reset(mockAkkaServiceClientFactory)
    when(mockDatastoreService.getDefaultDatastore).thenReturn(null)
    when(mockAkkaServiceClientFactory.newAkkaServiceClient(or(anyString(), isNull.asInstanceOf[String]))).thenReturn(mockAkkaServiceClient)

    filter = new OpenStackIdentityV3Filter(mockConfigurationService,
      mockDatastoreService,
      mockHttpClientService,
      mockAkkaServiceClientFactory)
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

      filter.configurationUpdated(config)
      filter.configurationUpdated(config)

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
      filter.configurationUpdated(config)

      filter.destroy()

      verify(mockAkkaServiceClient).destroy()
    }
  }
}
