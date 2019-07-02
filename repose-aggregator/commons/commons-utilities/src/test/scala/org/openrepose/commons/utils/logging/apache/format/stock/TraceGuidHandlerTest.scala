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

package org.openrepose.commons.utils.logging.apache.format.stock

import java.util.StringTokenizer
import javax.servlet.http.HttpServletRequest

import org.junit.runner.RunWith
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class TraceGuidHandlerTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {
  import org.mockito.Mockito.when

  var mockRequest: HttpServletRequest = _
  var traceGuidHandler: TraceGuidHandler = _

  override def beforeEach() = {
    mockRequest = mock[HttpServletRequest]
    traceGuidHandler = new TraceGuidHandler
  }

  describe("the trace GUID handler") {
    List(
      ("panda", "panda"),
      ("f229a177-8bd4-4842-889f-1ff8aa70e8da", "f229a177-8bd4-4842-889f-1ff8aa70e8da"),
      ("eyJyZXF1ZXN0SWQiOiI1Zjg1NDE4OC02OGEyLTQ2N2YtODc0ZS02ZTA4OTM1OTI4MTgifQ==", "5f854188-68a2-467f-874e-6e0893592818")
    ).foreach { case (tracingHeader, expectedMessage) =>
        it(s"should convert header $tracingHeader to $expectedMessage") {
          when(mockRequest.getHeaders(CommonHttpHeader.TRACE_GUID))
            .thenReturn(new StringTokenizer(tracingHeader).asInstanceOf[java.util.Enumeration[String]])

          val actualMessage = traceGuidHandler.handle(mockRequest, null)

          actualMessage shouldEqual expectedMessage
        }
    }
  }
}
