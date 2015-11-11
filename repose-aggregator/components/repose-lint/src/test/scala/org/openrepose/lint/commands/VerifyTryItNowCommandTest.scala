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

    it("should report if filters are not listed in the system-model") {
      val configDir = new File(getClass.getResource("/configs/nofilters/").toURI)
      val config = new LintConfig(configDir = configDir)

      val out = new ByteArrayOutputStream()
      val err = new ByteArrayOutputStream()

      Console.setOut(out)
      Console.setErr(err)

      VerifyTryItNowCommand.perform(config)

      val outputString = new String(out.toByteArray)

      outputString should include regex "auth-n .* IS NOT listed in the system-model"
      outputString should include regex "auth-z .* IS NOT listed in the system-model"
      outputString should include regex "keystone-v2 .* IS NOT listed in the system-model"
      outputString should include regex "identity-v3 .* IS NOT listed in the system-model"
    }

    it("should report if filters are filtered by uri regex") {
      val configDir = new File(getClass.getResource("/configs/master/").toURI)
      val config = new LintConfig(configDir = configDir)

      val out = new ByteArrayOutputStream()
      val err = new ByteArrayOutputStream()

      Console.setOut(out)
      Console.setErr(err)

      VerifyTryItNowCommand.perform(config)

      val outputString = new String(out.toByteArray)

      outputString should include regex "auth-n .* IS filtered by uri-regex"
      outputString should include regex "auth-z .* IS filtered by uri-regex"
      outputString should include regex "keystone-v2 .* IS filtered by uri-regex"
      outputString should include regex "identity-v3 .* IS filtered by uri-regex"
    }

    it("should report if auth-n is not in tenanted mode") {
      val configDir = new File(getClass.getResource("/configs/master/").toURI)
      val config = new LintConfig(configDir = configDir)

      val out = new ByteArrayOutputStream()
      val err = new ByteArrayOutputStream()

      Console.setOut(out)
      Console.setErr(err)

      VerifyTryItNowCommand.perform(config)

      val outputString = new String(out.toByteArray)

      outputString should include regex "auth-n .* NOT IN tenanted mode"
    }

    it("should report if auth-n has foyer as a service admin role") {
      val configDir = new File(getClass.getResource("/configs/master/").toURI)
      val config = new LintConfig(configDir = configDir)

      val out = new ByteArrayOutputStream()
      val err = new ByteArrayOutputStream()

      Console.setOut(out)
      Console.setErr(err)

      VerifyTryItNowCommand.perform(config)

      val outputString = new String(out.toByteArray)

      outputString should include regex "auth-n .* DOES NOT HAVE service-admin role foyer"
    }

    it("should report if auth-n has foyer as an ignore tenant role") {
      val configDir = new File(getClass.getResource("/configs/master/").toURI)
      val config = new LintConfig(configDir = configDir)

      val out = new ByteArrayOutputStream()
      val err = new ByteArrayOutputStream()

      Console.setOut(out)
      Console.setErr(err)

      VerifyTryItNowCommand.perform(config)

      val outputString = new String(out.toByteArray)

      outputString should include regex "auth-n .* DOES NOT HAVE ignore-tenant role foyer"
    }

    it("should report if auth-z has foyer as a role bypass") {
      val configDir = new File(getClass.getResource("/configs/master/").toURI)
      val config = new LintConfig(configDir = configDir)

      val out = new ByteArrayOutputStream()
      val err = new ByteArrayOutputStream()

      Console.setOut(out)
      Console.setErr(err)

      VerifyTryItNowCommand.perform(config)

      val outputString = new String(out.toByteArray)

      outputString should include regex "auth-z .* DOES NOT HAVE ignore-tenant role foyer"
    }

    it("should report if keystone-v2 is not in tenanted mode") {
      val configDir = new File(getClass.getResource("/configs/master/").toURI)
      val config = new LintConfig(configDir = configDir)

      val out = new ByteArrayOutputStream()
      val err = new ByteArrayOutputStream()

      Console.setOut(out)
      Console.setErr(err)

      VerifyTryItNowCommand.perform(config)

      val outputString = new String(out.toByteArray)

      outputString should include regex "keystone-v2 .* NOT IN tenanted mode"
    }

    it("should report if keystone-v2 has foyer as a pre-authorized role") {
      val configDir = new File(getClass.getResource("/configs/master/").toURI)
      val config = new LintConfig(configDir = configDir)

      val out = new ByteArrayOutputStream()
      val err = new ByteArrayOutputStream()

      Console.setOut(out)
      Console.setErr(err)

      VerifyTryItNowCommand.perform(config)

      val outputString = new String(out.toByteArray)

      outputString should include regex "keystone-v2 .* DOES NOT HAVE pre-authorized role foyer"
    }

    it("should report if keystone-v2 authorization is present") {
      val configDir = new File(getClass.getResource("/configs/master/").toURI)
      val config = new LintConfig(configDir = configDir)

      val out = new ByteArrayOutputStream()
      val err = new ByteArrayOutputStream()

      Console.setOut(out)
      Console.setErr(err)

      VerifyTryItNowCommand.perform(config)

      val outputString = new String(out.toByteArray)

      outputString should include regex "keystone-v2 .* DOES NOT HAVE service catalog authorization"
    }

    it("should report if identity-v3 is not in tenanted mode") {
      val configDir = new File(getClass.getResource("/configs/master/").toURI)
      val config = new LintConfig(configDir = configDir)

      val out = new ByteArrayOutputStream()
      val err = new ByteArrayOutputStream()

      Console.setOut(out)
      Console.setErr(err)

      VerifyTryItNowCommand.perform(config)

      val outputString = new String(out.toByteArray)

      outputString should include regex "identity-v3 .* NOT IN tenanted mode"
    }

    it("should report if identity-v3 has foyer as a pre-authorized role") {
      val configDir = new File(getClass.getResource("/configs/master/").toURI)
      val config = new LintConfig(configDir = configDir)

      val out = new ByteArrayOutputStream()
      val err = new ByteArrayOutputStream()

      Console.setOut(out)
      Console.setErr(err)

      VerifyTryItNowCommand.perform(config)

      val outputString = new String(out.toByteArray)

      outputString should include regex "identity-v3 .* DOES NOT HAVE tenant bypass role foyer"
    }

    it("should report if identity-v3 authorization is present") {
      val configDir = new File(getClass.getResource("/configs/master/").toURI)
      val config = new LintConfig(configDir = configDir)

      val out = new ByteArrayOutputStream()
      val err = new ByteArrayOutputStream()

      Console.setOut(out)
      Console.setErr(err)

      VerifyTryItNowCommand.perform(config)

      val outputString = new String(out.toByteArray)

      outputString should include regex "identity-v3 .* DOES NOT HAVE service catalog authorization"
    }

    it("should check multiple clusters if present") {
      pending
    }

    it("should check multiple filters if present") {
      pending
    }

    List("2.8.x", "3.x", "4.x", "5.x", "6.x", "7.x") foreach { version =>
      it(s"should handle configs from Repose version $version") {
        pending
      }
    }
  }
}
