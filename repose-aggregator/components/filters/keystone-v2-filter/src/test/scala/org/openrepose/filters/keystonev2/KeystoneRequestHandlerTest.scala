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

import java.util.GregorianCalendar

import javax.servlet.http.HttpServletResponse.SC_OK
import javax.ws.rs.core.HttpHeaders.RETRY_AFTER
import org.apache.http.HttpVersion
import org.apache.http.message.BasicHttpResponse
import org.junit.runner.RunWith
import org.openrepose.commons.utils.http.HttpDate
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class KeystoneRequestHandlerTest extends FunSpec with Matchers with MockitoSugar {
  describe("buildRetryValue") {
    Seq("retry-after", "retry-After", "Retry-After", "RETRY-AFTER", "rETRY-aFTER").foreach { headerName =>
      it(s"should retrieve the provided Retry After header value given $headerName") {
        val retryString = new HttpDate(new GregorianCalendar().getTime).toRFC1123
        val response = new BasicHttpResponse(
          HttpVersion.HTTP_1_1,
          SC_OK,
          null)
        response.addHeader(RETRY_AFTER, retryString)
        KeystoneRequestHandler.buildRetryValue(response) shouldBe retryString
      }
    }

    it("should build a new Retry After header value if one is not provided") {
      val response = new BasicHttpResponse(
        HttpVersion.HTTP_1_1,
        SC_OK,
        null)
      KeystoneRequestHandler.buildRetryValue(response) shouldNot be(null)
    }
  }
}
