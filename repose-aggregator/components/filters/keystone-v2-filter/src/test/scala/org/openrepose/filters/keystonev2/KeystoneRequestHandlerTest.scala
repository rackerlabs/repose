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

import java.io.InputStream
import java.util.GregorianCalendar

import javax.servlet.http.HttpServletResponse.SC_OK
import javax.ws.rs.core.HttpHeaders.RETRY_AFTER
import org.apache.http.Header
import org.apache.http.message.BasicHeader
import org.junit.runner.RunWith
import org.openrepose.commons.utils.http.{HttpDate, ServiceClientResponse}
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar

@RunWith(classOf[JUnitRunner])
class KeystoneRequestHandlerTest extends FunSpec with Matchers with MockitoSugar {
  describe("buildRetryValue") {
    Seq("retry-after", "retry-After", "Retry-After", "RETRY-AFTER", "rETRY-aFTER").foreach { headerName =>
      it(s"should retrieve the provided Retry After header value given $headerName") {
        val retryString = new HttpDate(new GregorianCalendar().getTime).toRFC1123
        val serviceClientResponse = new ServiceClientResponse(
          SC_OK,
          Array(new BasicHeader(RETRY_AFTER, retryString)),
          mock[InputStream])
        KeystoneRequestHandler.buildRetryValue(serviceClientResponse) shouldBe retryString
      }
    }

    it("should build a new Retry After header value if one is not provided") {
      val serviceClientResponse = new ServiceClientResponse(
        SC_OK,
        Array.empty[Header],
        mock[InputStream])
      KeystoneRequestHandler.buildRetryValue(serviceClientResponse) shouldNot be(null)
    }
  }
}
