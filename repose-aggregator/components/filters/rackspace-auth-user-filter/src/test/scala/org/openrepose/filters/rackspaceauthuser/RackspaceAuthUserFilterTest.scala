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

package org.openrepose.filters.rackspaceauthuser

import java.io.ByteArrayInputStream
import javax.servlet.FilterChain
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.openrepose.commons.utils.http.{OpenStackServiceHeader, PowerApiHeader}
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.services.config.ConfigurationService
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.mock.web.MockHttpServletRequest

@RunWith(classOf[JUnitRunner])
class RackspaceAuthUserFilterTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {

  var filter: RackspaceAuthUserFilter = _
  var servletRequest: MockHttpServletRequest = _
  var servletResponse: HttpServletResponse = _
  var filterChain: FilterChain = _

  override def beforeEach() = {
    servletRequest = new MockHttpServletRequest
    servletResponse = mock[HttpServletResponse]
    filterChain = mock[FilterChain]

    filter = new RackspaceAuthUserFilter(null)
  }

  describe("do work") {
    List("OPTIONS", "GET", "HEAD", "PUT", "DELETE", "TRACE", "CONNECT", "CUSTOM") foreach { httpMethod =>
      it(s"will not update the request for method $httpMethod") {
        filter.configurationUpdated(mock[RackspaceAuthUserConfig])
        servletRequest.setMethod(httpMethod)

        filter.doWork(servletRequest, servletResponse, filterChain)

        // verify the request was not wrapped and thus not updated
        verify(filterChain).doFilter(servletRequest, servletResponse)
      }
    }

    it("will wrap the request when it's a POST") {
      filter.configurationUpdated(mock[RackspaceAuthUserConfig])
      servletRequest.setMethod("POST")

      filter.doWork(servletRequest, servletResponse, filterChain)

      // verify the request was wrapped
      verify(filterChain).doFilter(any(classOf[HttpServletRequestWrapper]), any(classOf[HttpServletResponse]))
    }

    it("will write the correct headers") {
      filter.configurationUpdated(auth2_0Config())
      servletRequest.setMethod("POST")
      servletRequest.setContentType("application/json")
      servletRequest.setContent(
        """{
          |    "auth": {
          |        "RAX-AUTH:domain": {
          |            "name": "Rackspace"
          |        },
          |        "passwordCredentials": {
          |            "username": "jqsmith",
          |            "password": "mypassword"
          |        }
          |    }
          |}""".stripMargin.getBytes)

      filter.doWork(servletRequest, servletResponse, filterChain)

      val captor = ArgumentCaptor.forClass(classOf[HttpServletRequest])
      verify(filterChain).doFilter(captor.capture(), any(classOf[HttpServletResponse]))
      val request = captor.getValue
      request.getHeader(PowerApiHeader.DOMAIN.toString) shouldBe "Rackspace"
      request.getHeader(OpenStackServiceHeader.USER_NAME.toString) shouldBe "Racker:jqsmith"
      request.getHeader(PowerApiHeader.USER.toString) shouldBe "Racker:jqsmith;q=0.6"
      request.getHeader(PowerApiHeader.GROUPS.toString) shouldBe "GROUP;q=0.6"
    }
  }

  def auth1_1Config(): RackspaceAuthUserConfig = {
    val conf = new RackspaceAuthUserConfig

    val v11 = new IdentityV11()
    v11.setContentBodyReadLimit(BigInt(4096 * 1024).bigInteger)
    v11.setGroup("GROUP")
    v11.setQuality("0.6")
    conf.setV11(v11)

    conf
  }

  def auth2_0Config(): RackspaceAuthUserConfig = {
    val conf = new RackspaceAuthUserConfig

    val v20 = new IdentityV2()
    v20.setContentBodyReadLimit(BigInt(4096 * 1024).bigInteger)
    v20.setGroup("GROUP")
    v20.setQuality("0.6")
    conf.setV20(v20)

    conf
  }

  describe("Auth 1.1 requests") {
    filter = new RackspaceAuthUserFilter(mock[ConfigurationService])
    filter.configurationUpdated(auth1_1Config())
    describe("XML") {
      it("Parses the XML credentials payload into a username") {
        val payload =
          """
            |<?xml version="1.0" encoding="UTF-8"?>
            |
            |<credentials xmlns="http://docs.rackspacecloud.com/auth/api/v1.1"
            |             username="hub_cap"
            |             key="a86850deb2742ec3cb41518e26aa2d89"/>
          """.stripMargin.trim()

        val (domain, username) = filter.username1_1XML(new ByteArrayInputStream(payload.getBytes))
        domain shouldBe None
        username shouldBe Some("hub_cap")
      }
    }
    describe("JSON") {
      it("Parses the JSON credentials payload into a username") {
        val payload =
          """
            |{
            |    "credentials" : {
            |        "username" : "hub_cap",
            |        "key"  : "a86850deb2742ec3cb41518e26aa2d89"
            |    }
            |}
          """.stripMargin.trim()

        val (domain, username) = filter.username1_1JSON(new ByteArrayInputStream(payload.getBytes))
        domain shouldBe None
        username shouldBe Some("hub_cap")
      }
    }
  }

  describe("Auth 2.0 requests") {
    filter = new RackspaceAuthUserFilter(mock[ConfigurationService])
    filter.configurationUpdated(auth2_0Config())
    describe("XML") {
      it("parses the username out of a User/Password request") {
        val payload =
          """
            |<?xml version="1.0" encoding="UTF-8"?>
            |<auth xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            | xmlns="http://docs.openstack.org/identity/api/v2.0">
            |  <passwordCredentials username="demoauthor" password="theUsersPassword" tenantId="1100111"/>
            |</auth>
          """.stripMargin.trim()

        val (domain, username) = filter.username2_0XML(new ByteArrayInputStream(payload.getBytes))
        domain shouldBe None
        username shouldBe Some("demoauthor")
      }
      it("parses the username out of an APIKey request") {
        val payload =
          """
            |<?xml version="1.0" encoding="UTF-8"?>
            |<auth>
            |  <apiKeyCredentials
            |    xmlns="http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0"
            |    username="demoauthor"
            |    apiKey="aaaaa-bbbbb-ccccc-12345678"
            |    tenantId="1100111" />
            |</auth>
          """.stripMargin.trim()

        val (domain, username) = filter.username2_0XML(new ByteArrayInputStream(payload.getBytes))
        domain shouldBe None
        username shouldBe Some("demoauthor")
      }
      it("parses the tenant ID out of a tenant token request") {
        val payload =
          """
            |<?xml version="1.0" encoding="UTF-8"?>
            |<auth xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            | xmlns="http://docs.openstack.org/identity/api/v2.0"
            | tenantId="1100111">
            |  <token id="vvvvvvvv-wwww-xxxx-yyyy-zzzzzzzzzzzz" />
            |</auth>
          """.stripMargin.trim()

        val (domain, username) = filter.username2_0XML(new ByteArrayInputStream(payload.getBytes))
        domain shouldBe None
        username shouldBe Some("1100111")
      }
      it("parses the tenant name out of a tenant token request") {
        val payload =
          """
            |<?xml version="1.0" encoding="UTF-8"?>
            |<auth xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            | xmlns="http://docs.openstack.org/identity/api/v2.0"
            | tenantName="nameOfTenant">
            |  <token id="vvvvvvvv-wwww-xxxx-yyyy-zzzzzzzzzzzz" />
            |</auth>
          """.stripMargin.trim()

        val (domain, username) = filter.username2_0XML(new ByteArrayInputStream(payload.getBytes))
        domain shouldBe None
        username shouldBe Some("nameOfTenant")
      }

      it("parses scope/username out of an MFA request") {
        // The example this was taken from is currently incorrect.
        // This is the best guess at what it should be until it is corrected.
        // The error will only effect the outcome if/when we start differentiating Scope'd items.
        // TODO: Have the API documentation updated and update this payload.
        val payload =
        """<?xml version="1.0" encoding="UTF-8"?>
          |<auth xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          |      xmlns="http://docs.openstack.org/identity/api/v2.0">
          |    <RAX-AUTH:scope>SETUP-MFA</RAX-AUTH:scope>
          |    <passwordCredentials username="demoAuthor" password="myPassword01"/>
          |</auth>""".stripMargin

        val (domain, username) = filter.username2_0XML(new ByteArrayInputStream(payload.getBytes))
        domain shouldBe None
        username shouldBe Some("demoAuthor")
      }

      it("parses the domain/username out of a Domain'd User/Password request") {
        val payload =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<auth xmlns="http://docs.openstack.org/identity/api/v2.0"
            |      xmlns:OS-KSADM="http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0"
            |      xmlns:RAX-AUTH="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0"
            |      xmlns:atom="http://www.w3.org/2005/Atom">
            |    <passwordCredentials password="mypassword" username="jqsmith"/>
            |    <RAX-AUTH:domain name="Rackspace"/>
            |</auth>""".stripMargin

        val (domain, username) = filter.username2_0XML(new ByteArrayInputStream(payload.getBytes))
        domain shouldBe Some("Rackspace")
        username shouldBe Some("Racker:jqsmith")
      }

      it("parses the domain/username out of a RSA User/Token request") {
        val payload =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<auth xmlns="http://docs.openstack.org/identity/api/v2.0"
            |      xmlns:OS-KSADM="http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0"
            |      xmlns:RAX-AUTH="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0"
            |      xmlns:atom="http://www.w3.org/2005/Atom">
            |    <RAX-AUTH:rsaCredentials tokenKey="8723984574" username="jqsmith"/>
            |    <RAX-AUTH:domain name="Rackspace"/>
            |</auth>""".stripMargin

        val (domain, username) = filter.username2_0XML(new ByteArrayInputStream(payload.getBytes))
        domain shouldBe Some("Rackspace")
        username shouldBe Some("Racker:jqsmith")
      }
    }

    describe("JSON") {
      it("parses the username out of a User/Password request") {
        val payload =
          """
            |{
            |    "auth":{
            |        "passwordCredentials":{
            |            "username":"demoauthor",
            |            "password":"theUsersPassword"
            |        },
            |        "tenantId": "12345678"
            |    }
            |}
          """.stripMargin

        val (domain, username) = filter.username2_0JSON(new ByteArrayInputStream(payload.getBytes))
        domain shouldBe None
        username shouldBe Some("demoauthor")

      }
      it("parses the username out of an APIKey request") {
        val payload =
          """
            |{
            |    "auth": {
            |        "RAX-KSKEY:apiKeyCredentials": {
            |            "username": "demoauthor",
            |            "apiKey": "aaaaa-bbbbb-ccccc-12345678"
            |        },
            |        "tenantId": "1100111"
            |    }
            |}
          """.stripMargin

        val (domain, username) = filter.username2_0JSON(new ByteArrayInputStream(payload.getBytes))
        domain shouldBe None
        username shouldBe Some("demoauthor")
      }
      it("parses the tenant ID out of a tenant token request") {
        val payload =
          """
            |{
            |    "auth": {
            |        "tenantId": "1100111",
            |        "token": {
            |            "id": "vvvvvvvv-wwww-xxxx-yyyy-zzzzzzzzzzzz"
            |        }
            |    }
            |}
          """.stripMargin

        val (domain, username) = filter.username2_0JSON(new ByteArrayInputStream(payload.getBytes))
        domain shouldBe None
        username shouldBe Some("1100111")
      }
      it("parses the tenant name out of a tenant token request") {
        val payload =
          """
            |{
            |    "auth": {
            |        "tenantName": "nameOfTenant",
            |        "token": {
            |            "id": "vvvvvvvv-wwww-xxxx-yyyy-zzzzzzzzzzzz"
            |        }
            |    }
            |}
          """.stripMargin

        val (domain, username) = filter.username2_0JSON(new ByteArrayInputStream(payload.getBytes))
        domain shouldBe None
        username shouldBe Some("nameOfTenant")
      }

      it("parses scope/username out of an MFA request") {
        val payload =
          """{
            |    "auth":{
            |        "RAX-AUTH:scope": "SETUP-MFA",
            |        "passwordCredentials": {
            |            "username": "demoAuthor",
            |            "password": "myPassword01"
            |        }
            |    }
            |}""".stripMargin

        val (domain, username) = filter.username2_0JSON(new ByteArrayInputStream(payload.getBytes))
        domain shouldBe None
        username shouldBe Some("demoAuthor")
      }

      it("parses the domain/username out of a Domain'd User/Password request") {
        val payload =
          """{
            |    "auth": {
            |        "RAX-AUTH:domain": {
            |            "name": "Rackspace"
            |        },
            |        "passwordCredentials": {
            |            "username": "jqsmith",
            |            "password": "mypassword"
            |        }
            |    }
            |}""".stripMargin

        val (domain, username) = filter.username2_0JSON(new ByteArrayInputStream(payload.getBytes))
        domain shouldBe Some("Rackspace")
        username shouldBe Some("Racker:jqsmith")
      }

      it("parses the domain/username out of a RSA User/Token request") {
        val payload =
          """{
            |    "auth": {
            |        "RAX-AUTH:domain": {
            |            "name": "Rackspace"
            |        },
            |        "RAX-AUTH:rsaCredentials": {
            |            "tokenKey": "8723984574",
            |            "username": "jqsmith"
            |        }
            |    }
            |}""".stripMargin

        val (domain, username) = filter.username2_0JSON(new ByteArrayInputStream(payload.getBytes))
        domain shouldBe Some("Rackspace")
        username shouldBe Some("Racker:jqsmith")
      }
    }
  }
}
