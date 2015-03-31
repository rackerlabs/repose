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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class RackspaceAuthUserParsingTest extends FunSpec with Matchers {

  def auth1_1Config() = {
    val conf = new RackspaceAuthUserConfig

    val v11 = new IdentityV11()
    v11.setContentBodyReadLimit(BigInt(4096 * 1024).bigInteger)
    v11.setGroup("GROUP")
    v11.setQuality("0.6")
    conf.setV11(v11)

    conf
  }

  def auth2_0Config() = {
    val conf = new RackspaceAuthUserConfig

    val v20 = new IdentityV2()
    v20.setContentBodyReadLimit(BigInt(4096 * 1024).bigInteger)
    v20.setGroup("GROUP")
    v20.setQuality("0.6")
    conf.setV20(v20)

    conf
  }

  describe("Auth 1.1 Requests") {
    val handler = new RackspaceAuthUserHandler(auth1_1Config())
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


        val username = handler.username1_1XML(new ByteArrayInputStream(payload.getBytes))
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

        val username = handler.username1_1JSON(new ByteArrayInputStream(payload.getBytes))

        username shouldBe Some("hub_cap")
      }
    }
  }

  describe("Auth 2.0 requests") {
    val handler = new RackspaceAuthUserHandler(auth2_0Config())
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

        val username = handler.username2_0XML(new ByteArrayInputStream(payload.getBytes))
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


        val username = handler.username2_0XML(new ByteArrayInputStream(payload.getBytes))
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

        val username = handler.username2_0XML(new ByteArrayInputStream(payload.getBytes))
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

        val username = handler.username2_0XML(new ByteArrayInputStream(payload.getBytes))
        username shouldBe Some("nameOfTenant")
      }
      //TODO: don't care about the multifactor parts...?
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

        val username = handler.username2_0JSON(new ByteArrayInputStream(payload.getBytes))
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


        val username = handler.username2_0JSON(new ByteArrayInputStream(payload.getBytes))
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

        val username = handler.username2_0JSON(new ByteArrayInputStream(payload.getBytes))
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

        val username = handler.username2_0JSON(new ByteArrayInputStream(payload.getBytes))
        username shouldBe Some("nameOfTenant")
      }
      //TODO: don't care about the multifactor parts...?
    }

  }
}
