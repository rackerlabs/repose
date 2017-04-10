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
package org.openrepose.nodeservice.request

import java.util.Optional

import org.junit.runner.RunWith
import org.mockito.Mockito.{verify, when}
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.healthcheck.HealthCheckService
import org.openrepose.nodeservice.containerconfiguration.ContainerConfigurationService
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

import scala.util.Random

@RunWith(classOf[JUnitRunner])
class RequestHeaderServiceImplTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {

  private val version = "9.8.7.6"
  private var request: HttpServletRequestWrapper = _
  private var containerConfigurationService: ContainerConfigurationService = _
  private var requestHeaderServiceImpl: RequestHeaderServiceImpl = _

  override def beforeEach(): Unit = {
    request = mock[HttpServletRequestWrapper]
    containerConfigurationService = mock[ContainerConfigurationService]
    requestHeaderServiceImpl = new RequestHeaderServiceImpl(mock[ConfigurationService], containerConfigurationService, mock[HealthCheckService], "cluster", "node", version)
  }

  describe("The Request Header Service Implementation") {
    it("should set the X-Forwarded-For header") {
      val remote = "1.2.3.4"
      when(request.getRemoteAddr).thenReturn(remote)
      requestHeaderServiceImpl.setXForwardedFor(request)
      verify(request).addHeader(CommonHttpHeader.X_FORWARDED_FOR, remote)
    }

    val local = Math.abs(Random.nextInt)
    val prefixToString: String => String = { string => if (string == null || string.length < 1) s"Repose:$local" else string }
    Seq("1.0", "1.1") foreach { protocol =>
      Seq("prefix", "", null) foreach { prefix =>
        it(s"should set the Via header with protocol '$protocol', prefix '$prefix', and should include version '$version'") {
          when(request.getLocalPort).thenReturn(local)
          when(request.getProtocol).thenReturn(s"HTTP/$protocol")
          when(containerConfigurationService.getRequestVia).thenReturn(Optional.ofNullable(prefix))

          requestHeaderServiceImpl.setVia(request)

          verify(request).addHeader(CommonHttpHeader.VIA, s"$protocol ${prefixToString(prefix)} (Repose/$version)}")
        }
      }
    }
  }
}
