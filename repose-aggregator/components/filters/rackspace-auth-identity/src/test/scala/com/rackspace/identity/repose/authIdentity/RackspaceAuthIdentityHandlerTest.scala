package com.rackspace.identity.repose.authIdentity

import com.mockrunner.mock.web.{MockHttpServletResponse, MockHttpServletRequest}
import com.rackspace.papi.commons.util.http.PowerApiHeader
import com.rackspace.papi.commons.util.http.header.HeaderName
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

class RackspaceAuthIdentityHandlerTest extends FunSpec with Matchers with MockitoSugar {

  import scala.collection.JavaConverters._

  val mockRequest = new MockHttpServletRequest()
  val mockResponse = mock[ReadableHttpServletResponse]

  val bothConfig = {
    val top = new RackspaceAuthIdentityConfig()
    val v11 = new IdentityV11()
    v11.setGroup("V11Group")
    v11.setQuality("0.6")
    v11.setContentBodyReadLimit(BigInt(4096).bigInteger)
    top.setV11(v11)

    val v20 = new IdentityV2()
    v20.setGroup("V20Group")
    v20.setQuality("0.7")
    v20.setContentBodyReadLimit(BigInt(4096).bigInteger)
    top.setV20(v20)

    top
  }

  val handler = new RackspaceAuthIdentityHandler(bothConfig)


  describe("Sets the correct header for") {
    describe("for auth 2.0") {
      val groupsHeader = Set("0.7", "V20Group")
      it("user/APIKEY payload in json") {
        mockRequest.resetAll()
        mockRequest.setBodyContent(
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
          """.stripMargin)
        mockRequest.setContentType("application/json")

        val director = handler.handleRequest(mockRequest, mockResponse)

        director.requestHeaderManager().hasHeaders shouldBe true
        val headersToAdd = director.requestHeaderManager().headersToAdd()

        val pp_userHeader = headersToAdd.get(HeaderName.wrap(PowerApiHeader.USER.toString))
        pp_userHeader.asScala shouldBe Set("demoauthor", "0.7")

        val pp_groupsHeader = headersToAdd.get(HeaderName.wrap(PowerApiHeader.GROUPS.toString))
        pp_groupsHeader.asScala shouldBe groupsHeader
      }

      it("user/pass payload in xml") {
        mockRequest.setBodyContent(
          """
            |<?xml version="1.0" encoding="UTF-8"?>
            |<auth xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            | xmlns="http://docs.openstack.org/identity/api/v2.0">
            |  <passwordCredentials username="demoauthor" password="theUsersPassword" tenantId="1100111"/>
            |</auth>
          """.stripMargin.trim())
        mockRequest.setContentType("application/xml")

        val director = handler.handleRequest(mockRequest, mockResponse)

        director.requestHeaderManager().hasHeaders shouldBe true
        val headersToAdd = director.requestHeaderManager().headersToAdd()

        val pp_userHeader = headersToAdd.get(HeaderName.wrap(PowerApiHeader.USER.toString))
        pp_userHeader.asScala shouldBe Set("demoauthor", "0.7")

        val pp_groupsHeader = headersToAdd.get(HeaderName.wrap(PowerApiHeader.GROUPS.toString))
        pp_groupsHeader.asScala shouldBe groupsHeader
      }
    }
    describe("for auth 1.1") {
      val groupsHeader = Set("V11Group", "0.6")
      it("payload in xml") {
        mockRequest.setBodyContent(
          """
            |<?xml version="1.0" encoding="UTF-8"?>
            |
            |<credentials xmlns="http://docs.rackspacecloud.com/auth/api/v1.1"
            |             username="hub_cap"
            |             key="a86850deb2742ec3cb41518e26aa2d89"/>
          """.
            stripMargin.trim())
        mockRequest.setContentType("application/xml")

        val director = handler.handleRequest(mockRequest, mockResponse)

        director.requestHeaderManager().hasHeaders shouldBe true
        val headersToAdd = director.requestHeaderManager().headersToAdd()

        val pp_userHeader = headersToAdd.get(HeaderName.wrap(PowerApiHeader.USER.toString))
        pp_userHeader.asScala shouldBe Set("hub_cap", "0.6")

        val pp_groupsHeader = headersToAdd.get(HeaderName.wrap(PowerApiHeader.GROUPS.toString))
        pp_groupsHeader.asScala shouldBe groupsHeader
      }
      it("payload in json") {
        mockRequest.setBodyContent(
          """
            |{
            |    "credentials" : {
            |        "username" : "hub_cap",
            |        "key"  : "a86850deb2742ec3cb41518e26aa2d89"
            |    }
            |}
          """.stripMargin.trim())
        mockRequest.setContentType("application/json")

        val director = handler.handleRequest(mockRequest, mockResponse)

        director.requestHeaderManager().hasHeaders shouldBe true
        val headersToAdd = director.requestHeaderManager().headersToAdd()

        val pp_userHeader = headersToAdd.get(HeaderName.wrap(PowerApiHeader.USER.toString))
        pp_userHeader.asScala shouldBe Set("hub_cap", "0.6")

        val pp_groupsHeader = headersToAdd.get(HeaderName.wrap(PowerApiHeader.GROUPS.toString))
        pp_groupsHeader.asScala shouldBe groupsHeader

      }
    }
  }

}
