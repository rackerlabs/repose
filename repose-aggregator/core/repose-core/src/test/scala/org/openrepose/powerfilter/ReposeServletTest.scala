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
package org.openrepose.powerfilter

import java.io.IOException
import java.net.URISyntaxException

import javax.servlet.ServletException
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponse._
import org.junit.runner.RunWith
import org.mockito.Matchers.{any, isA}
import org.mockito.Mockito.{verify, when}
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse}

@RunWith(classOf[JUnitRunner])
class ReposeServletTest extends FunSpec with BeforeAndAfterEach with MockitoSugar with Matchers {

  var router: PowerFilterRouter = _
  var reposeServlet: ReposeServlet = _


  override def beforeEach(): Unit = {
    super.beforeEach()

    router = mock[PowerFilterRouter]
    reposeServlet = new ReposeServlet(router)
  }

  describe("service") {
    it(s"should route the request") {
      val req = new MockHttpServletRequest()
      val resp = new MockHttpServletResponse()

      reposeServlet.service(req, resp)

      verify(router).route(isA(classOf[HttpServletRequestWrapper]), isA(classOf[HttpServletResponse]))
    }

    Set(
      new IOException(),
      new ServletException(),
      new URISyntaxException("://example.com", "Invalid protocol", 0),
      new RuntimeException()
    ) foreach { exception =>
      it(s"should return a 500 response if routing throws a[n] ${exception.getClass.getSimpleName} and the response is not committed") {
        when(router.route(any[HttpServletRequestWrapper], any[HttpServletResponse]))
          .thenThrow(exception)

        val req = new MockHttpServletRequest()
        val resp = new MockHttpServletResponse()

        reposeServlet.service(req, resp)

        resp.getStatus shouldBe SC_INTERNAL_SERVER_ERROR
      }

      it(s"should return the response if routing throws a[n] ${exception.getClass.getSimpleName} and the response is committed") {
        when(router.route(any[HttpServletRequestWrapper], any[HttpServletResponse]))
          .thenThrow(exception)

        val req = new MockHttpServletRequest()
        val resp = new MockHttpServletResponse()

        resp.sendError(SC_NOT_ACCEPTABLE)

        reposeServlet.service(req, resp)

        resp.getStatus shouldBe SC_NOT_ACCEPTABLE
      }
    }
  }
}
