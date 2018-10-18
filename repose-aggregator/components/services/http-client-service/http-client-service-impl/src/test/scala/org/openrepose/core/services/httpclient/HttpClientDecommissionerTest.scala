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
package org.openrepose.core.services.httpclient

import org.apache.http.impl.client.CloseableHttpClient
import org.junit.runner.RunWith
import org.mockito.Mockito.{never, verify}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec}

@RunWith(classOf[JUnitRunner])
class HttpClientDecommissionerTest extends FunSpec with BeforeAndAfterEach with MockitoSugar {

  val clientInstanceId = "client-id"

  var closeableHttpClient: CloseableHttpClient = _
  var httpClientDecommissioner: HttpClientDecommissioner = _

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    closeableHttpClient = mock[CloseableHttpClient]

    httpClientDecommissioner = new HttpClientDecommissioner()
  }

  override protected def afterEach(): Unit = {
    super.afterEach()

    httpClientDecommissioner.destroy()
  }

  describe("decommissioning") {
    it("should decommission a client that was never in use") {
      httpClientDecommissioner.decommissionClient(clientInstanceId, closeableHttpClient)

      httpClientDecommissioner.run()

      verify(closeableHttpClient).close()
    }

    it("should not decommission a client that is in use") {
      val userId = "user-id"

      httpClientDecommissioner.registerUser(clientInstanceId, userId)
      httpClientDecommissioner.decommissionClient(clientInstanceId, closeableHttpClient)

      httpClientDecommissioner.run()

      verify(closeableHttpClient, never).close()
    }

    it("should decommission a client that was in use but is no longer") {
      val userId = "user-id"

      httpClientDecommissioner.registerUser(clientInstanceId, userId)
      httpClientDecommissioner.decommissionClient(clientInstanceId, closeableHttpClient)
      httpClientDecommissioner.deregisterUser(clientInstanceId, userId)

      httpClientDecommissioner.run()

      verify(closeableHttpClient).close()
    }
  }
}
