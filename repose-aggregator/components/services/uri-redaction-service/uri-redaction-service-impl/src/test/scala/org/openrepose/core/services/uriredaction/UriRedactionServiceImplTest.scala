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
package org.openrepose.core.services.uriredaction

import java.net.URL
import java.util.regex.PatternSyntaxException

import org.junit.runner.RunWith
import org.mockito.Matchers.{eq => isEq, _}
import org.mockito.Mockito.verify
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.uriredaction.UriRedactionServiceImpl._
import org.openrepose.core.services.uriredaction.config.UriRedactionConfig
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class UriRedactionServiceImplTest extends FunSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  var configurationService: ConfigurationService = _
  var uriRedactionService: UriRedactionServiceImpl = _

  override def beforeEach(): Unit = {
    configurationService = mock[ConfigurationService]

    uriRedactionService = new UriRedactionServiceImpl(configurationService)
  }

  describe("init") {
    it("should subscribe an configuration listener") {
      uriRedactionService.init()

      verify(configurationService).subscribeTo(
        isEq(s"$ServiceName.cfg.xml"),
        any[URL](),
        isA(classOf[UpdateListener[UriRedactionConfig]]),
        isA(classOf[Class[UriRedactionConfig]]))
    }
  }

  describe("destroy") {
    it("should unsubscribe a configuration listener") {
      uriRedactionService.destroy()

      verify(configurationService).unsubscribeFrom(
        isEq(s"$ServiceName.cfg.xml"),
        isA(classOf[UpdateListener[UriRedactionConfig]]))
    }
  }

  describe("isInitialized") {
    it("should return false if the service has not been configured") {
      uriRedactionService.isInitialized shouldBe false
    }

    it("should return true if the service has been configured") {
      val uriRedactionConfig = new UriRedactionConfig()
      uriRedactionConfig.getRedact.addAll(Seq.empty[String].asJava)

      uriRedactionService.configurationUpdated(uriRedactionConfig)

      uriRedactionService.isInitialized shouldBe true
    }
  }

  describe("configurationUpdated") {
    it("should not initialize if the configuration had malformed regex's") {
      val uriRedactionConfig = new UriRedactionConfig()
      uriRedactionConfig.getRedact.addAll(Seq("^/should/fail/[^/").asJava)

      intercept[PatternSyntaxException] {
        uriRedactionService.configurationUpdated(uriRedactionConfig)
      }

      uriRedactionService.isInitialized shouldBe false
    }

    it("should continue to use old configuration if the new configuration has a malformed regex") {
      val uriRedactionConfig = new UriRedactionConfig()
      uriRedactionConfig.getRedact.addAll(Seq("^/([^/]+).*").asJava)

      uriRedactionService.configurationUpdated(uriRedactionConfig)
      uriRedactionService.redact("/redactMe/optional") shouldBe s"/$RedactedString/optional"

      uriRedactionConfig.getRedact.addAll(Seq("^/should/fail/[^/").asJava)
      intercept[PatternSyntaxException] {
        uriRedactionService.configurationUpdated(uriRedactionConfig)
      }

      uriRedactionService.isInitialized shouldBe true
      uriRedactionService.redact("/redactMe/optional") shouldBe s"/$RedactedString/optional"
    }
  }

  describe("redact example One") {
    val uriRedactionConfig = new UriRedactionConfig()
    uriRedactionConfig.getRedact.addAll(Seq(
      "^/v1/[^/]+/([^/]+)/[^/]+.*",
      "^/v2/admin/([^/]+)/[^/]+.*"
    ).asJava)

    Seq(
      ("v1", "/v1/anything/redactMe/required", s"/v1/anything/$RedactedString/required"),
      ("v1 with trailing", "/v1/anything/redactMe/required/", s"/v1/anything/$RedactedString/required/"),
      ("v2", "/v2/admin/redactMe/required", s"/v2/admin/$RedactedString/required"),
      ("v2 with trailing", "/v2/admin/redactMe/required/", s"/v2/admin/$RedactedString/required/")
    ).foreach { case (name, origUri, expected) =>
      it(s"should redact $name call") {
        uriRedactionService.configurationUpdated(uriRedactionConfig)

        uriRedactionService.redact(origUri) shouldBe expected
      }
    }
  }

  describe("redact example Two") {
    val uriRedactionConfig = new UriRedactionConfig()
    uriRedactionConfig.getRedact.addAll(Seq(
      "^/[^/]+/([^/]+)/[^/]+.*",
      "^/[^/]+/[^/]+/admin/([^/]+).*",
      s"^/[^/]+/[^/]+/[^/]+/$RedactedString/secret/([^/]+).*"
    ).asJava)

    Seq(
      ("only one", "/one/redactMe/required", s"/one/$RedactedString/required"),
      ("only one with trailing", "/one/redactMe/required/", s"/one/$RedactedString/required/"),
      ("only one leaving extra", "/one/redactMe/required/extra", s"/one/$RedactedString/required/extra"),
      ("only one leaving extras", "/one/redactMe/required/extra/extra", s"/one/$RedactedString/required/extra/extra"),
      ("only one match", "/one/redactMe/required/redactMe", s"/one/$RedactedString/required/redactMe"),
      ("admin", "/one/two/admin/redactMe", s"/one/$RedactedString/admin/$RedactedString"),
      ("admin with trailing", "/one/two/admin/redactMe/", s"/one/$RedactedString/admin/$RedactedString/"),
      ("admin secret", "/one/two/admin/redactMe/secret/meToo", s"/one/$RedactedString/admin/$RedactedString/secret/$RedactedString"),
      ("admin secret with trailing", "/one/two/admin/redactMe/secret/meToo/", s"/one/$RedactedString/admin/$RedactedString/secret/$RedactedString/")
    ).foreach { case (name, origUri, expected) =>
      it(s"should redact $name") {
        uriRedactionService.configurationUpdated(uriRedactionConfig)

        uriRedactionService.redact(origUri) shouldBe expected
      }
    }
  }

  describe("redact example Three") {
    val uriRedactionConfig = new UriRedactionConfig()
    uriRedactionConfig.getRedact.addAll(Seq(
      "^/([^/]*)/([^/]*)/([^/]*)/[^/]+.*"
    ).asJava)

    Seq(
      ("first three", "/redactMe/redactMe/redactMe/required", s"/$RedactedString/$RedactedString/$RedactedString/required"),
      ("first three with trailing", "/redactMe/redactMe/redactMe/required/", s"/$RedactedString/$RedactedString/$RedactedString/required/")
    ).foreach { case (name, origUri, expected) =>
      it(s"should redact $name call") {
        uriRedactionService.configurationUpdated(uriRedactionConfig)

        uriRedactionService.redact(origUri) shouldBe expected
      }
    }
  }
}
