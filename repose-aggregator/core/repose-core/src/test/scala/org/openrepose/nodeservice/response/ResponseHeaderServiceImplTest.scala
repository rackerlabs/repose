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
package org.openrepose.nodeservice.response

import java.util.Optional
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.junit.runner.RunWith
import org.mockito.Matchers.anyString
import org.mockito.Mockito.{never, verify, when}
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.nodeservice.containerconfiguration.ContainerConfigurationService
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

import scala.util.Random

@RunWith(classOf[JUnitRunner])
class ResponseHeaderServiceImplTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {

  private val version = "9.8.7.6"
  private var request: HttpServletRequest = _
  private var response: HttpServletResponse = _
  private var containerConfigurationService: ContainerConfigurationService = _
  private var responseHeaderServiceImpl: ResponseHeaderServiceImpl = _

  override def beforeEach(): Unit = {
    request = mock[HttpServletRequest]
    response = mock[HttpServletResponse]
    containerConfigurationService = mock[ContainerConfigurationService]
    responseHeaderServiceImpl = new ResponseHeaderServiceImpl(containerConfigurationService, version)
  }

  describe("The Response Header Service Implementation") {
    val local = Math.abs(Random.nextInt)
    val shouldOrNot: Boolean => String = { boolean => if (boolean) "should" else "should not" }
    val prefixToString: String => String = { string => if (string != null && string.length > 0) string else "" }
    val versionString: Boolean => String = { boolean => if (boolean) s" (Repose/$version)" else "" }
    Seq("1.0", "1.1") foreach { protocol =>
      Seq("prefix", "", null) foreach { prefix =>
        Seq(true, false) foreach { includeVersion =>
          it(s"${shouldOrNot((prefix != null && prefix.length > 0) || includeVersion)} set the Via header with protocol '$protocol', prefix '$prefix', and includeVersion '$includeVersion'") {
            when(request.getLocalPort).thenReturn(local)
            when(request.getProtocol).thenReturn(s"HTTP/$protocol")
            when(containerConfigurationService.getResponseVia).thenReturn(Optional.ofNullable(prefix))
            when(containerConfigurationService.includeViaReposeVersion()).thenReturn(includeVersion)

            responseHeaderServiceImpl.setVia(request, response)

            if ((prefix != null && prefix.length > 0) || includeVersion) {
              verify(response).setHeader(CommonHttpHeader.VIA, s"$protocol ${prefixToString(prefix)}${versionString(includeVersion)}")
            } else {
              verify(response, never).setHeader(anyString, anyString)
            }
          }
        }
      }
    }
  }
}
