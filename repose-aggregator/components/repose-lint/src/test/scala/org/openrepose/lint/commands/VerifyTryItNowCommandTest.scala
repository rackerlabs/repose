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
package org.openrepose.lint.commands

import java.io.{ByteArrayOutputStream, File}

import org.junit.runner.RunWith
import org.openrepose.lint.LintConfig
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}
import play.api.libs.json.{JsArray, Json}

@RunWith(classOf[JUnitRunner])
class VerifyTryItNowCommandTest extends FunSpec with Matchers {

  describe("getCommandToken") {
    it("should return a String token for the command") {
      VerifyTryItNowCommand.getCommandToken shouldBe a[String]
    }
  }

  describe("getCommandDescription") {
    it("should return a String description of the command") {
      VerifyTryItNowCommand.getCommandDescription shouldBe a[String]
    }
  }

  describe("perform") {
    it("should fail if the system model file is not present") {
      val configDir = new File(getClass.getResource("/configs/").toURI)
      val config = new LintConfig(configDir = configDir)

      a[Exception] should be thrownBy VerifyTryItNowCommand.perform(config)
    }

    it("should fail if the system model file cannot be read") {
      val configDir = new File(getClass.getResource("/configs/invalidsystemmodel/").toURI)
      val config = new LintConfig(configDir = configDir)

      an[Exception] should be thrownBy VerifyTryItNowCommand.perform(config)
    }

    it("should report not ready if filters are not listed in the system-model") {
      val configDir = new File(getClass.getResource("/configs/nofilters/").toURI)
      val config = new LintConfig(configDir = configDir)

      val out = new ByteArrayOutputStream()

      Console.setOut(out)

      VerifyTryItNowCommand.perform(config)

      val outputString = new String(out.toByteArray)
      val parsedOutput = Json.parse(outputString)

      ((parsedOutput \ "clusters") (0) \ "authNCheck" \ "foyerStatus").as[String] should include("NotReady")
      ((parsedOutput \ "clusters") (0) \ "authZCheck" \ "foyerStatus").as[String] should include("NotReady")
      ((parsedOutput \ "clusters") (0) \ "keystoneV2Check" \ "foyerStatus").as[String] should include("NotReady")
      ((parsedOutput \ "clusters") (0) \ "identityV3Check" \ "foyerStatus").as[String] should include("NotReady")
    }

    it("should report if filters are filtered by uri regex") {
      val configDir = new File(getClass.getResource("/configs/master/").toURI)
      val config = new LintConfig(configDir = configDir)

      val out = new ByteArrayOutputStream()

      Console.setOut(out)

      VerifyTryItNowCommand.perform(config)

      val outputString = new String(out.toByteArray)
      val parsedOutput = Json.parse(outputString)

      ((parsedOutput \ "clusters") (0) \ "authNCheck" \\ "filteredByUriRegex").head.as[Boolean] shouldBe true
      ((parsedOutput \ "clusters") (0) \ "authZCheck" \\ "filteredByUriRegex").head.as[Boolean] shouldBe true
      ((parsedOutput \ "clusters") (0) \ "keystoneV2Check" \\ "filteredByUriRegex").head.as[Boolean] shouldBe true
      ((parsedOutput \ "clusters") (0) \ "identityV3Check" \\ "filteredByUriRegex").head.as[Boolean] shouldBe true
    }

    it("should report if auth-n is not in tenanted mode") {
      val configDir = new File(getClass.getResource("/configs/master/").toURI)
      val config = new LintConfig(configDir = configDir)

      val out = new ByteArrayOutputStream()

      Console.setOut(out)

      VerifyTryItNowCommand.perform(config)

      val outputString = new String(out.toByteArray)
      val parsedOutput = Json.parse(outputString)

      ((parsedOutput \ "clusters") (0) \ "authNCheck" \\ "inTenantedMode").head.as[Boolean] shouldBe false
    }

    it("should report if auth-n has foyer as a service admin role") {
      val configDir = new File(getClass.getResource("/configs/master/").toURI)
      val config = new LintConfig(configDir = configDir)

      val out = new ByteArrayOutputStream()

      Console.setOut(out)

      VerifyTryItNowCommand.perform(config)

      val outputString = new String(out.toByteArray)
      val parsedOutput = Json.parse(outputString)

      ((parsedOutput \ "clusters") (0) \ "authNCheck" \\ "foyerAsServiceAdmin").head.as[Boolean] shouldBe false
    }

    it("should report if auth-n has foyer as an ignore tenant role") {
      val configDir = new File(getClass.getResource("/configs/master/").toURI)
      val config = new LintConfig(configDir = configDir)

      val out = new ByteArrayOutputStream()

      Console.setOut(out)

      VerifyTryItNowCommand.perform(config)

      val outputString = new String(out.toByteArray)
      val parsedOutput = Json.parse(outputString)

      ((parsedOutput \ "clusters") (0) \ "authNCheck" \\ "foyerAsIgnoreTenant").head.as[Boolean] shouldBe false
    }

    it("should report if auth-z has foyer as a role bypass") {
      val configDir = new File(getClass.getResource("/configs/master/").toURI)
      val config = new LintConfig(configDir = configDir)

      val out = new ByteArrayOutputStream()

      Console.setOut(out)

      VerifyTryItNowCommand.perform(config)

      val outputString = new String(out.toByteArray)
      val parsedOutput = Json.parse(outputString)

      ((parsedOutput \ "clusters") (0) \ "authZCheck" \\ "foyerAsIgnoreTenant").head.as[Boolean] shouldBe false
    }

    it("should report if keystone-v2 is not in tenanted mode") {
      val configDir = new File(getClass.getResource("/configs/master/").toURI)
      val config = new LintConfig(configDir = configDir)

      val out = new ByteArrayOutputStream()

      Console.setOut(out)

      VerifyTryItNowCommand.perform(config)

      val outputString = new String(out.toByteArray)
      val parsedOutput = Json.parse(outputString)

      ((parsedOutput \ "clusters") (0) \ "keystoneV2Check" \\ "inTenantedMode").head.as[Boolean] shouldBe false
    }

    it("should report if keystone-v2 has foyer as a pre-authorized role") {
      val configDir = new File(getClass.getResource("/configs/master/").toURI)
      val config = new LintConfig(configDir = configDir)

      val out = new ByteArrayOutputStream()

      Console.setOut(out)

      VerifyTryItNowCommand.perform(config)

      val outputString = new String(out.toByteArray)
      val parsedOutput = Json.parse(outputString)

      ((parsedOutput \ "clusters") (0) \ "keystoneV2Check" \\ "foyerAsPreAuthorized").head.as[Boolean] shouldBe false
    }

    it("should report if keystone-v2 authorization is present") {
      val configDir = new File(getClass.getResource("/configs/master/").toURI)
      val config = new LintConfig(configDir = configDir)

      val out = new ByteArrayOutputStream()

      Console.setOut(out)

      VerifyTryItNowCommand.perform(config)

      val outputString = new String(out.toByteArray)
      val parsedOutput = Json.parse(outputString)

      ((parsedOutput \ "clusters") (0) \ "keystoneV2Check" \\ "catalogAuthorization").head.as[Boolean] shouldBe false
    }

    it("should report if identity-v3 is not in tenanted mode") {
      val configDir = new File(getClass.getResource("/configs/master/").toURI)
      val config = new LintConfig(configDir = configDir)

      val out = new ByteArrayOutputStream()

      Console.setOut(out)

      VerifyTryItNowCommand.perform(config)

      val outputString = new String(out.toByteArray)
      val parsedOutput = Json.parse(outputString)

      ((parsedOutput \ "clusters") (0) \ "identityV3Check" \\ "inTenantedMode").head.as[Boolean] shouldBe false
    }

    it("should report if identity-v3 has foyer as a pre-authorized role") {
      val configDir = new File(getClass.getResource("/configs/master/").toURI)
      val config = new LintConfig(configDir = configDir)

      val out = new ByteArrayOutputStream()

      Console.setOut(out)

      VerifyTryItNowCommand.perform(config)

      val outputString = new String(out.toByteArray)
      val parsedOutput = Json.parse(outputString)

      ((parsedOutput \ "clusters") (0) \ "identityV3Check" \\ "foyerAsBypassTenant").head.as[Boolean] shouldBe false
    }

    it("should report if identity-v3 authorization is present") {
      val configDir = new File(getClass.getResource("/configs/master/").toURI)
      val config = new LintConfig(configDir = configDir)

      val out = new ByteArrayOutputStream()

      Console.setOut(out)

      VerifyTryItNowCommand.perform(config)

      val outputString = new String(out.toByteArray)
      val parsedOutput = Json.parse(outputString)

      ((parsedOutput \ "clusters") (0) \ "identityV3Check" \\ "catalogAuthorization").head.as[Boolean] shouldBe false
    }

    it("should check multiple clusters if present") {
      val configDir = new File(getClass.getResource("/configs/duplicateclustersandfilters/").toURI)
      val config = new LintConfig(configDir = configDir)

      val out = new ByteArrayOutputStream()

      Console.setOut(out)

      VerifyTryItNowCommand.perform(config)

      val outputString = new String(out.toByteArray)
      val parsedOutput = Json.parse(outputString)

      (parsedOutput \\ "clusterId").map(_.as[String]) should (contain("cluster_1") and contain("cluster_2"))
    }

    it("should check multiple filers if present") {
      val configDir = new File(getClass.getResource("/configs/duplicateclustersandfilters/").toURI)
      val config = new LintConfig(configDir = configDir)

      val out = new ByteArrayOutputStream()

      Console.setOut(out)

      VerifyTryItNowCommand.perform(config)

      val outputString = new String(out.toByteArray)
      val parsedOutput = Json.parse(outputString)

      ((parsedOutput \ "clusters") (0) \ "authNCheck" \ "filters").as[JsArray].value should have size 2
      ((parsedOutput \ "clusters") (0) \ "authZCheck" \ "filters").as[JsArray].value should have size 2
      ((parsedOutput \ "clusters") (0) \ "keystoneV2Check" \ "filters").as[JsArray].value should have size 2
      ((parsedOutput \ "clusters") (0) \ "identityV3Check" \ "filters").as[JsArray].value should have size 2
    }
  }
}
