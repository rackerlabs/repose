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
import java.util.concurrent.TimeUnit
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.servlet.{FilterChain, ServletRequest, ServletResponse}

import org.junit.runner.RunWith
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.{ArgumentCaptor, Mockito}
import org.openrepose.commons.test.MockitoAnswers
import org.openrepose.commons.utils.http.{OpenStackServiceHeader, PowerApiHeader}
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.DatastoreService
import org.openrepose.core.services.datastore.distributed.DistributedDatastore
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.mock.web.MockHttpServletRequest

@RunWith(classOf[JUnitRunner])
class RackspaceAuthUserFilterTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar with MockitoAnswers {

  var filter: RackspaceAuthUserFilter = _
  var servletRequest: MockHttpServletRequest = _
  var servletResponse: HttpServletResponse = _
  var filterChain: FilterChain = _
  var datastore: DistributedDatastore = _

  override def beforeEach() = {
    servletRequest = new MockHttpServletRequest
    servletResponse = mock[HttpServletResponse]
    filterChain = mock[FilterChain]
    datastore = mock[DistributedDatastore]
    val datastoreService = mock[DatastoreService]
    when(datastoreService.getDistributedDatastore).thenReturn(datastore)

    filter = new RackspaceAuthUserFilter(null, datastoreService)
  }

  describe("construction") {
    it("should work with the distributed datastore") {
      val datastoreService = mock[DatastoreService]
      when(datastoreService.getDistributedDatastore).thenReturn(datastore)
      new RackspaceAuthUserFilter(null, datastoreService)

      verify(datastoreService).getDistributedDatastore
      verify(datastoreService, times(0)).getDefaultDatastore
    }

    it("should work without the distributed datastore") {
      val datastoreService = mock[DatastoreService]
      new RackspaceAuthUserFilter(null, datastoreService)

      val inOrder = Mockito.inOrder(datastoreService)
      inOrder.verify(datastoreService).getDistributedDatastore
      inOrder.verify(datastoreService).getDefaultDatastore
    }
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

    it("will write the headers when the user is present in the datastore") {
      filter.configurationUpdated(auth2_0Config())
      servletRequest.setMethod("POST")
      servletRequest.setContentType("application/json")
      servletRequest.setContent(
        """{
          |  "auth": {
          |    "RAX-AUTH:passcodeCredentials": {
          |      "passcode": "123456"
          |    }
          |  }
          |}""".stripMargin.getBytes)
      servletRequest.addHeader(RackspaceAuthUserFilter.sessionIdHeader, "foo-bar")
      when(datastore.get(s"${RackspaceAuthUserFilter.ddKey}:foo-bar")).thenReturn(Option(RackspaceAuthUserGroup(Option("Rackspace"), "Racker:jqsmith", "GROUP", 0.6)), Nil: _*)

      filter.doWork(servletRequest, servletResponse, filterChain)

      val captor = ArgumentCaptor.forClass(classOf[HttpServletRequest])
      verify(filterChain).doFilter(captor.capture(), any(classOf[HttpServletResponse]))
      val request = captor.getValue
      request.getHeader(PowerApiHeader.DOMAIN.toString) shouldBe "Rackspace"
      request.getHeader(OpenStackServiceHeader.USER_NAME.toString) shouldBe "Racker:jqsmith"
      request.getHeader(PowerApiHeader.USER.toString) shouldBe "Racker:jqsmith;q=0.6"
      request.getHeader(PowerApiHeader.GROUPS.toString) shouldBe "GROUP;q=0.6"
    }

    it("will only search the datastore till it finds the session") {
      filter.configurationUpdated(auth2_0Config())
      servletRequest.setMethod("POST")
      servletRequest.setContentType("application/json")
      servletRequest.setContent(
        """{
          |  "auth": {
          |    "RAX-AUTH:passcodeCredentials": {
          |      "passcode": "123456"
          |    }
          |  }
          |}""".stripMargin.getBytes)
      servletRequest.addHeader(RackspaceAuthUserFilter.sessionIdHeader, "banana;q=0.5,foo-bar;q=0.7")
      when(datastore.get(s"${RackspaceAuthUserFilter.ddKey}:foo-bar")).thenReturn(Option(RackspaceAuthUserGroup(Option("Rackspace"), "Racker:jqsmith", "GROUP", 0.6)), Nil: _*)

      filter.doWork(servletRequest, servletResponse, filterChain)

      val captor = ArgumentCaptor.forClass(classOf[HttpServletRequest])
      verify(filterChain).doFilter(captor.capture(), any(classOf[HttpServletResponse]))
      val request = captor.getValue
      request.getHeader(PowerApiHeader.DOMAIN.toString) shouldBe "Rackspace"
      request.getHeader(OpenStackServiceHeader.USER_NAME.toString) shouldBe "Racker:jqsmith"
      request.getHeader(PowerApiHeader.USER.toString) shouldBe "Racker:jqsmith;q=0.6"
      request.getHeader(PowerApiHeader.GROUPS.toString) shouldBe "GROUP;q=0.6"

      verify(datastore, times(0)).get(s"${RackspaceAuthUserFilter.ddKey}:banana")
    }

    it("will search the datastore till it finds the session") {
      filter.configurationUpdated(auth2_0Config())
      servletRequest.setMethod("POST")
      servletRequest.setContentType("application/json")
      servletRequest.setContent(
        """{
          |  "auth": {
          |    "RAX-AUTH:passcodeCredentials": {
          |      "passcode": "123456"
          |    }
          |  }
          |}""".stripMargin.getBytes)
      servletRequest.addHeader(RackspaceAuthUserFilter.sessionIdHeader, "banana;q=0.7,foo-bar;q=0.5")
      when(datastore.get(s"${RackspaceAuthUserFilter.ddKey}:foo-bar")).thenReturn(Option(RackspaceAuthUserGroup(Option("Rackspace"), "Racker:jqsmith", "GROUP", 0.6)), Nil: _*)

      filter.doWork(servletRequest, servletResponse, filterChain)

      val captor = ArgumentCaptor.forClass(classOf[HttpServletRequest])
      verify(filterChain).doFilter(captor.capture(), any(classOf[HttpServletResponse]))
      val request = captor.getValue
      request.getHeader(PowerApiHeader.DOMAIN.toString) shouldBe "Rackspace"
      request.getHeader(OpenStackServiceHeader.USER_NAME.toString) shouldBe "Racker:jqsmith"
      request.getHeader(PowerApiHeader.USER.toString) shouldBe "Racker:jqsmith;q=0.6"
      request.getHeader(PowerApiHeader.GROUPS.toString) shouldBe "GROUP;q=0.6"

      verify(datastore).get(s"${RackspaceAuthUserFilter.ddKey}:banana")
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
    filter = new RackspaceAuthUserFilter(mock[ConfigurationService], mock[DatastoreService])
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
    filter = new RackspaceAuthUserFilter(mock[ConfigurationService], mock[DatastoreService])
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

  describe("mfa responses") {
    val requestBody =
      """
      {
        "auth": {
          "passwordCredentials": {
            "username": "bob",
            "password": "butts"
          }
        }
      }
      """.stripMargin

    List(
      (List("OS-MF sessionId='123456', factor='PASSCODE'"),
        Asserter({ verify(datastore).put(meq(RackspaceAuthUserFilter.ddKey + ":123456"),
                                meq(Option(RackspaceAuthUserGroup(None, "bob", "GROUP", 0.6))),
                                meq(5),
                                meq(TimeUnit.MINUTES)) })),
      (List("OS-MF sessionId='green', factor='PASSCODE'", "Keystone uri=https://some.identity.com"),
        Asserter({ verify(datastore).put(meq(RackspaceAuthUserFilter.ddKey + ":green"),
                                meq(Option(RackspaceAuthUserGroup(None, "bob", "GROUP", 0.6))),
                                meq(5),
                                meq(TimeUnit.MINUTES)) })),
      (List("Keystone uri=https://some.identity.com", "OS-MF sessionId='banana', factor='PASSCODE'"),
        Asserter({ verify(datastore).put(meq(RackspaceAuthUserFilter.ddKey + ":banana"),
                                meq(Option(RackspaceAuthUserGroup(None, "bob", "GROUP", 0.6))),
                                meq(5),
                                meq(TimeUnit.MINUTES)) })),
      (List.empty,
        Asserter({ verify(datastore, times(0)).put(anyString,
                                                   any(classOf[Option[RackspaceAuthUserGroup]]),
                                                   anyInt,
                                                   any(classOf[TimeUnit])) })),
      (List("OS-MF factor='PASSCODE'"),
        Asserter({ verify(datastore, times(0)).put(anyString,
                                                   any(classOf[Option[RackspaceAuthUserGroup]]),
                                                   anyInt,
                                                   any(classOf[TimeUnit])) })),
      (List("OS-MF sessionId='123456"),
        Asserter({ verify(datastore, times(0)).put(anyString,
                                                   any(classOf[Option[RackspaceAuthUserGroup]]),
                                                   anyInt,
                                                   any(classOf[TimeUnit])) }))
    ) foreach { case(headers: List[String], asserter: Asserter) =>
      it(s"should write to the dd as appropriate with headers: $headers") {
        filter.configurationUpdated(auth2_0Config())
        servletRequest.setMethod("POST")
        servletRequest.setContentType("application/json")
        servletRequest.setContent(requestBody.getBytes)
        doAnswer(answer({ invocation: InvocationOnMock =>
          val response: HttpServletResponse = invocation.getArguments()(1).asInstanceOf[HttpServletResponse]
          headers foreach { response.addHeader("WWW-Authenticate", _) }
        })).when(filterChain).doFilter(any(classOf[ServletRequest]), any(classOf[ServletResponse]))

        filter.doWork(servletRequest, servletResponse, filterChain)

        asserter.assert()
      }
    }
  }

  //I couldn't get the typing and evaluation time to work right without this, if you can solve it, please do and share
  class Asserter(assertion: => Unit) {
    def assert(): Unit = {
      assertion
    }
  }

  object Asserter {
    def apply(someCode: => Unit): Asserter = {
      new Asserter(someCode)
    }
  }
}
