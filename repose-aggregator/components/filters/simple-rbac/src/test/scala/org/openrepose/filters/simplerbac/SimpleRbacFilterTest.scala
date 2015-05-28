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
package org.openrepose.filters.simplerbac

import javax.servlet.http.HttpServletResponse.{SC_FORBIDDEN, SC_METHOD_NOT_ALLOWED, SC_NOT_FOUND, SC_OK}

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.junit.runner.RunWith
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.simplerbac.config.SimpleRbacConfig
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.springframework.mock.web.{MockFilterChain, MockHttpServletRequest, MockHttpServletResponse}

@RunWith(classOf[JUnitRunner])
class SimpleRbacFilterTest extends FunSpec with BeforeAndAfterAll with BeforeAndAfter with GivenWhenThen with Matchers with MockitoSugar with LazyLogging {
  var filter: SimpleRbacFilter = _
  var config: SimpleRbacConfig = _
  var servletRequest: MockHttpServletRequest = _
  var servletResponse: MockHttpServletResponse = _
  var filterChain: MockFilterChain = _
  var mockConfigService: ConfigurationService = _

  override def beforeAll() {
    System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
      "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl")
  }

  before {
    servletRequest = new MockHttpServletRequest
    servletResponse = new MockHttpServletResponse
    filterChain = new MockFilterChain
    mockConfigService = mock[ConfigurationService]
    filter = new SimpleRbacFilter(mockConfigService)
    config = new SimpleRbacConfig
    config.setResources(
      """
        |/path/to/this  GET       role1,role2,role3,role4
        |/path/to/this  PUT       role1,role2,role3
        |/path/to/this  POST      role1,role2
        |/path/to/this  DELETE    role1
        |/path/to/that  GET,PUT   ALL
        |/path/to/that  ALL       role1
        | """.stripMargin.trim()
    )
  }

  List(false, true).foreach { case (isMasked) =>
    val maskWith: Boolean => String = { boolean => if (boolean) "with" else "without" }
    describe(s"Simple RBAC ${maskWith(isMasked)} masked roles") {
      val resultShould: Int => String = { int => if (int == SC_OK) "should" else "should not" }
      val conditionsThis: List[(String, String, Int, Int)] = List(
        //Method    Role      Result        Masked
        ("GET",     "role1",  SC_OK,        SC_OK),
        ("PUT",     "role1",  SC_OK,        SC_OK),
        ("POST",    "role1",  SC_OK,        SC_OK),
        ("DELETE",  "role1",  SC_OK,        SC_OK),
        ("GET",     "role2",  SC_OK,        SC_OK),
        ("PUT",     "role2",  SC_OK,        SC_OK),
        ("POST",    "role2",  SC_OK,        SC_OK),
        ("DELETE",  "role2",  SC_FORBIDDEN, SC_METHOD_NOT_ALLOWED),
        ("GET",     "role3",  SC_OK,        SC_OK),
        ("PUT",     "role3",  SC_OK,        SC_OK),
        ("POST",    "role3",  SC_FORBIDDEN, SC_METHOD_NOT_ALLOWED),
        ("DELETE",  "role3",  SC_FORBIDDEN, SC_METHOD_NOT_ALLOWED),
        ("GET",     "role4",  SC_OK,        SC_OK),
        ("PUT",     "role4",  SC_FORBIDDEN, SC_METHOD_NOT_ALLOWED),
        ("POST",    "role4",  SC_FORBIDDEN, SC_METHOD_NOT_ALLOWED),
        ("DELETE",  "role4",  SC_FORBIDDEN, SC_METHOD_NOT_ALLOWED),
        ("GET",     "role5",  SC_FORBIDDEN, SC_NOT_FOUND),
        ("PUT",     "role5",  SC_FORBIDDEN, SC_NOT_FOUND),
        ("POST",    "role5",  SC_FORBIDDEN, SC_NOT_FOUND),
        ("DELETE",  "role5",  SC_FORBIDDEN, SC_NOT_FOUND)
      )
      conditionsThis.foreach { case (method, role, result, masked) =>
        it(s"${resultShould(result)} allow the request to THIS resource when using HTTP method ${method} and having role ${role}.") {
          Given(s"a request using HTTP method $method and having role $role")
          servletRequest.setRequestURI("/path/to/this")
          servletRequest.setMethod(method)
          servletRequest.addHeader(config.getRolesHeaderName, role)
          filter.configurationUpdated(config)

          When("the protected resource is requested")
          filter.doFilter(servletRequest, servletResponse, filterChain)

          Then(s"the request ${resultShould(result)} be allowed access")
          if (isMasked) {
            servletResponse.getStatus shouldBe result
          } else {
            servletResponse.getStatus shouldBe masked
          }
        }
      }

      val conditionsThat: List[(String, String, Int, Int)] = List(
        //Method    Role      Result        Masked
        ("GET",     "role1",  SC_OK,        SC_OK),
        ("PUT",     "role1",  SC_OK,        SC_OK),
        ("POST",    "role1",  SC_OK,        SC_OK),
        ("DELETE",  "role1",  SC_OK,        SC_OK),
        ("GET",     "role2",  SC_OK,        SC_OK),
        ("PUT",     "role2",  SC_OK,        SC_OK),
        ("POST",    "role2",  SC_FORBIDDEN, SC_METHOD_NOT_ALLOWED),
        ("DELETE",  "role2",  SC_FORBIDDEN, SC_METHOD_NOT_ALLOWED),
        ("GET",     "role3",  SC_OK,        SC_OK),
        ("PUT",     "role3",  SC_OK,        SC_OK),
        ("POST",    "role3",  SC_FORBIDDEN, SC_NOT_FOUND),
        ("DELETE",  "role3",  SC_FORBIDDEN, SC_NOT_FOUND)
      )
      conditionsThat.foreach { case (method, role, result, masked) =>
        it(s"${resultShould(result)} allow the request to THAT resource when using HTTP method ${method} and having role ${role}.") {
          Given("a request without proper roles")
          servletRequest.setRequestURI("/path/to/that")
          servletRequest.setMethod(method)
          servletRequest.addHeader(config.getRolesHeaderName, role)
          filter.configurationUpdated(config)

          When("the request is to access a protected resource")
          filter.doFilter(servletRequest, servletResponse, filterChain)

          Then(s"the request ${resultShould(result)} be allowed access")
          if (isMasked) {
            servletResponse.getStatus shouldBe result
          } else {
            servletResponse.getStatus shouldBe masked
          }
        }
      }
    }
  }
}
