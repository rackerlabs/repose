/*
 *  Copyright (c) 2015 Rackspace US, Inc.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.openrepose.filters.openstackidentityv3

import org.junit.runner.RunWith
import org.mockito.Mockito.when
import org.openrepose.core.services.datastore.DatastoreService
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient
import org.openrepose.filters.openstackidentityv3.config.{DelegatingType, OpenstackIdentityService, OpenstackIdentityV3Config}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class OpenStackIdentityV3HandlerFactoryTest extends FunSpec with BeforeAndAfter with Matchers with MockitoSugar {

  var handlerFactory: OpenStackIdentityV3HandlerFactory = _

  val mockAkkaServiceClient = mock[AkkaServiceClient]
  val mockDatastoreService = mock[DatastoreService]

  before {
    when(mockDatastoreService.getDefaultDatastore).thenReturn(null)

    handlerFactory = new OpenStackIdentityV3HandlerFactory(mockAkkaServiceClient, mockDatastoreService)
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
}
