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
package org.openrepose.core.services.httplogging

import org.junit.runner.RunWith
import org.openrepose.core.services.httplogging.config.{Format, HttpLoggingConfig, Message}
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class HttpLoggingConfigListenerTest extends FunSpec with BeforeAndAfterEach with Matchers {

  import HttpLoggingConfigListenerTest._

  var httpLoggingConfigListener: HttpLoggingConfigListener = _

  override def beforeEach(): Unit = {
    httpLoggingConfigListener = new HttpLoggingConfigListener()
  }

  describe("configurationUpdated") {
    it("should set the listener to initialized") {
      httpLoggingConfigListener.isInitialized shouldBe false

      httpLoggingConfigListener.configurationUpdated(createConfig())

      httpLoggingConfigListener.isInitialized shouldBe true
    }

    it("should create a template for each configured message") {
      httpLoggingConfigListener.configurationUpdated(createConfig(
        createMessage(
          "this.logger.name",
          "{ \"this\": \"message\" }"
        ),
        createMessage(
          "that.logger.name",
          "{ \"that\": \"message\" }"
        )
      ))

      httpLoggingConfigListener.loggableTemplates should have size 2
    }

    it("should prefetch the configured logger for each message") {
      httpLoggingConfigListener.configurationUpdated(createConfig(
        createMessage(
          "this.logger.name",
          "{ \"this\": \"message\" }"
        ),
        createMessage(
          "that.logger.name",
          "{ \"that\": \"message\" }"
        )
      ))

      httpLoggingConfigListener.loggableTemplates.map(_.logger.getName) should contain only("this.logger.name", "that.logger.name")
    }

    it("should not remove a message if it fails to validate") {
      httpLoggingConfigListener.configurationUpdated(createConfig(
        createMessage(
          "this.logger.name",
          "{ \"this\": message }",
          Format.JSON
        )
      ))

      httpLoggingConfigListener.loggableTemplates should have size 1
    }
  }
}

object HttpLoggingConfigListenerTest {
  def createConfig(messages: Message*): HttpLoggingConfig = {
    val config = new HttpLoggingConfig()
    config.getMessage.addAll(messages.asJava)
    config
  }

  def createMessage(logTo: String,
                    value: String,
                    format: Format = Format.PLAIN): Message = {
    val message = new Message()
    message.setLogTo(logTo)
    message.setValue(value)
    message.setFormat(format)
    message
  }
}
