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
package org.openrepose.core.services.logging

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.ConfigurationFactory
import org.apache.logging.log4j.status.StatusLogger
import org.apache.logging.log4j.test.appender.ListAppender
import org.junit.runner.RunWith
import org.openrepose.core.services.config.ConfigurationService
import org.scalatest.concurrent.Eventually
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{FunSpec, Matchers}
import org.slf4j.{Logger, LoggerFactory}

import scala.io.Source

@RunWith(classOf[JUnitRunner])
class LoggingServiceImplTest extends FunSpec with Matchers with MockitoSugar with Eventually {

  import scala.collection.JavaConversions._

  val mockConfigService = mock[ConfigurationService]
  val validExtensions = List("xml", "json", "yaml")
  private val LOG: Logger = LoggerFactory.getLogger(classOf[LoggingServiceImplTest].getName)

  /**
   * This is here because the yaml backend in Log4j2 is currently borked.
   * When the tests suddenly start failing, that means that we'll need to just remove the yamlWrapper
   * around the test code and see if the tests pass. Until that time they'll be ignored.
   * @param extension the file extension
   * @param f the function to wrap
   * @return
   */
  def yamlWrapper(extension: String)(f: => Unit) = {
    if (extension == "yaml") {
      pendingUntilFixed(f)
    } else {
      f
    }
  }

  def cleanupLoggerContext(context: LoggerContext)() = {
    System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY)
    context.reconfigure
    StatusLogger.getLogger.reset
  }

  validExtensions.foreach { ext =>
    describe(s"On Startup with extension $ext, the system") {
      it("Loads a fully qualified URL.") {
        val context = LogManager.getContext(false).asInstanceOf[LoggerContext]
        try {
          yamlWrapper(ext) {
            val loggingService = new LoggingServiceImpl(mockConfigService)

            val content1 = Source.fromInputStream(this.getClass.getResourceAsStream(s"/LoggingServiceImplTest/log4j2-List1.$ext")).mkString
            val file1 = File.createTempFile("log4j2-", s".$ext")
            file1.deleteOnExit()
            Files.write(file1.toPath, content1.getBytes(StandardCharsets.UTF_8))

            loggingService.updateLoggingConfiguration(file1.toURI.toURL.toString)

            val config1 = context.getConfiguration
            val app1 = config1.getAppender("List1").asInstanceOf[ListAppender]
            app1 should not be null
            app1.clear

            LOG.error("ERROR LEVEL LOG STATEMENT 1")
            LOG.warn("WARN  LEVEL LOG STATEMENT 1")
            LOG.info("INFO  LEVEL LOG STATEMENT 1")
            LOG.debug("DEBUG LEVEL LOG STATEMENT 1")
            LOG.trace("TRACE LEVEL LOG STATEMENT 1")

            val events1 = app1.getEvents.toList.map(_.getMessage.getFormattedMessage)

            events1 should contain("ERROR LEVEL LOG STATEMENT 1")
            events1 should contain("WARN  LEVEL LOG STATEMENT 1")
            events1 should not contain "INFO  LEVEL LOG STATEMENT 1"
            events1 should not contain "DEBUG LEVEL LOG STATEMENT 1"
            events1 should not contain "TRACE LEVEL LOG STATEMENT 1"
          }
        } finally {
          cleanupLoggerContext(context)
        }
      }

      it("Loads a relative pathed file.") {
        val context = LogManager.getContext(false).asInstanceOf[LoggerContext]
        try {
          yamlWrapper(ext) {
            val loggingService = new LoggingServiceImpl(mockConfigService)

            val content1 = Source.fromInputStream(this.getClass.getResourceAsStream(s"/LoggingServiceImplTest/log4j2-List1.$ext")).mkString
            val file1 = File.createTempFile("log4j2-", s".$ext")
            file1.deleteOnExit()
            Files.write(file1.toPath, content1.getBytes(StandardCharsets.UTF_8))

            loggingService.updateLoggingConfiguration(file1.getCanonicalPath)

            val config1 = context.getConfiguration
            val app1 = config1.getAppender("List1").asInstanceOf[ListAppender]
            app1 should not be null
            app1.clear

            LOG.error("ERROR LEVEL LOG STATEMENT 1")
            LOG.warn("WARN  LEVEL LOG STATEMENT 1")
            LOG.info("INFO  LEVEL LOG STATEMENT 1")
            LOG.debug("DEBUG LEVEL LOG STATEMENT 1")
            LOG.trace("TRACE LEVEL LOG STATEMENT 1")

            val events = app1.getEvents.toList.map(_.getMessage.getFormattedMessage)

            events should contain("ERROR LEVEL LOG STATEMENT 1")
            events should contain("WARN  LEVEL LOG STATEMENT 1")
            events should not contain "INFO  LEVEL LOG STATEMENT 1"
            events should not contain "DEBUG LEVEL LOG STATEMENT 1"
            events should not contain "TRACE LEVEL LOG STATEMENT 1"
          }
        } finally {
          cleanupLoggerContext(context)
        }
      }

      it("Falls back to known good configuration on failure.") {
        val loggingService = new LoggingServiceImpl(mockConfigService)
        val context = LogManager.getContext(false).asInstanceOf[LoggerContext]

        loggingService.updateLoggingConfiguration(s"BAD_FILE_NAME.$ext")

        val config1 = context.getConfiguration
        val appDefault = config1.getAppender("ListDefault").asInstanceOf[ListAppender]
        appDefault should not be null

        val events = appDefault.getEvents.toList.map(_.getMessage.getFormattedMessage)
        events.find(_.contains("ERROR FALLING BACK TO THE DEFAULT LOGGING CONFIGURATION!!!")) should not be None

        cleanupLoggerContext(context)
      }
    }

    describe(s"From know good state with extension $ext, the system ") {
      it("Switches to another known configuration.") {
        val context = LogManager.getContext(false).asInstanceOf[LoggerContext]
        try {
          yamlWrapper(ext) {
            val loggingService = new LoggingServiceImpl(mockConfigService)

            val content1 = Source.fromInputStream(this.getClass.getResourceAsStream(s"/LoggingServiceImplTest/log4j2-List1.$ext")).mkString
            val file1 = File.createTempFile("log4j2-", s".$ext")
            file1.deleteOnExit()
            Files.write(file1.toPath, content1.getBytes(StandardCharsets.UTF_8))

            loggingService.updateLoggingConfiguration(file1.toURI.toURL.toString)

            val config1 = context.getConfiguration
            val app1 = config1.getAppender("List1").asInstanceOf[ListAppender]
            app1 should not be null
            app1.clear

            LOG.error("ERROR LEVEL LOG STATEMENT 1")
            LOG.warn("WARN  LEVEL LOG STATEMENT 1")
            LOG.info("INFO  LEVEL LOG STATEMENT 1")
            LOG.debug("DEBUG LEVEL LOG STATEMENT 1")
            LOG.trace("TRACE LEVEL LOG STATEMENT 1")

            val content2 = Source.fromInputStream(this.getClass.getResourceAsStream(s"/LoggingServiceImplTest/log4j2-List2.$ext")).mkString
            val file2 = File.createTempFile("log4j2-", s".$ext")
            file2.deleteOnExit()
            Files.write(file2.toPath, content2.getBytes(StandardCharsets.UTF_8))

            loggingService.updateLoggingConfiguration(file2.toURI.toURL.toString)

            LOG.error("ERROR LEVEL LOG STATEMENT 2")
            LOG.warn("WARN  LEVEL LOG STATEMENT 2")
            LOG.info("INFO  LEVEL LOG STATEMENT 2")
            LOG.debug("DEBUG LEVEL LOG STATEMENT 2")
            LOG.trace("TRACE LEVEL LOG STATEMENT 2")

            val events1 = app1.getEvents.toList.map(_.getMessage.getFormattedMessage)
            events1 should contain("ERROR LEVEL LOG STATEMENT 1")
            events1 should contain("WARN  LEVEL LOG STATEMENT 1")
            events1 should not contain "INFO  LEVEL LOG STATEMENT 1"
            events1 should not contain "DEBUG LEVEL LOG STATEMENT 1"
            events1 should not contain "TRACE LEVEL LOG STATEMENT 1"
            events1 should not contain "ERROR LEVEL LOG STATEMENT 2"
            events1 should not contain "WARN  LEVEL LOG STATEMENT 2"
            events1 should not contain "INFO  LEVEL LOG STATEMENT 2"
            events1 should not contain "DEBUG LEVEL LOG STATEMENT 2"
            events1 should not contain "TRACE LEVEL LOG STATEMENT 2"

            val config2 = context.getConfiguration
            val app2 = config2.getAppender("List2").asInstanceOf[ListAppender]
            app2 should not be null
            val events2 = app2.getEvents.toList.map(_.getMessage.getFormattedMessage)
            events2 should not contain "ERROR LEVEL LOG STATEMENT 1"
            events2 should not contain "WARN  LEVEL LOG STATEMENT 1"
            events2 should not contain "INFO  LEVEL LOG STATEMENT 1"
            events2 should not contain "DEBUG LEVEL LOG STATEMENT 1"
            events2 should not contain "TRACE LEVEL LOG STATEMENT 1"
            events2 should contain("ERROR LEVEL LOG STATEMENT 2")
            events2 should contain("WARN  LEVEL LOG STATEMENT 2")
            events2 should contain("INFO  LEVEL LOG STATEMENT 2")
            events2 should contain("DEBUG LEVEL LOG STATEMENT 2")
            events2 should not contain "TRACE LEVEL LOG STATEMENT 2"
          }
        } finally {
          cleanupLoggerContext(context)
        }
      }

      it("Fails to switch when provided NULL.") {
        val context = LogManager.getContext(false).asInstanceOf[LoggerContext]
        try {
          yamlWrapper(ext) {
            val loggingService = new LoggingServiceImpl(mockConfigService)

            val content2 = Source.fromInputStream(this.getClass.getResourceAsStream(s"/LoggingServiceImplTest/log4j2-List2.$ext")).mkString
            val file2 = File.createTempFile("log4j2-", s".$ext")
            file2.deleteOnExit()
            Files.write(file2.toPath, content2.getBytes(StandardCharsets.UTF_8))

            loggingService.updateLoggingConfiguration(file2.getAbsolutePath)

            val config2 = context.getConfiguration
            val app2 = config2.getAppender("List2").asInstanceOf[ListAppender]
            app2 should not be null
            app2.clear

            loggingService.updateLoggingConfiguration(null)

            val events2 = app2.getEvents.toList.map(_.getMessage.getFormattedMessage)
            events2 should contain("Requested to reload a NULL configuration.")
          }
        } finally {
          cleanupLoggerContext(context)
        }
      }

      it("Fails to switch when provided the same file.") {
        val context = LogManager.getContext(false).asInstanceOf[LoggerContext]
        try {
          yamlWrapper(ext) {
            val loggingService = new LoggingServiceImpl(mockConfigService)

            val content2 = Source.fromInputStream(this.getClass.getResourceAsStream(s"/LoggingServiceImplTest/log4j2-List2.$ext")).mkString
            val file2 = File.createTempFile("log4j2-", s".$ext")
            file2.deleteOnExit()
            Files.write(file2.toPath, content2.getBytes(StandardCharsets.UTF_8))

            loggingService.updateLoggingConfiguration(file2.getAbsolutePath)

            val config2 = context.getConfiguration
            val app2 = config2.getAppender("List2").asInstanceOf[ListAppender]
            app2 should not be null
            app2.clear

            loggingService.updateLoggingConfiguration(file2.getAbsolutePath)

            val events2 = app2.getEvents.toList.map(_.getMessage.getFormattedMessage)

            events2 should contain("Requested to reload the same configuration: " + file2.getAbsolutePath)
          }
        } finally {
          cleanupLoggerContext(context)
        }
      }

      val invalidUrls = List(s"http://www.example.com/log4j2.$ext",
        s"https://www.example.com/log4j2.$ext",
        s"ftp://www.example.com/log4j2.$ext",
        s"jar://log4j2.$ext",
        s"classpath:///log4j2.$ext"
      )
      invalidUrls.foreach { url =>
        it( s"""Fails to switch when provided the invalid URL: "$url".""") {
          val context = LogManager.getContext(false).asInstanceOf[LoggerContext]
          try {
            yamlWrapper(ext) {
              val loggingService = new LoggingServiceImpl(mockConfigService)

              val content1 = Source.fromInputStream(this.getClass.getResourceAsStream(s"/LoggingServiceImplTest/log4j2-List1.$ext")).mkString
              val file1 = File.createTempFile("log4j2-", s".$ext")
              file1.deleteOnExit()
              Files.write(file1.toPath, content1.getBytes(StandardCharsets.UTF_8))

              loggingService.updateLoggingConfiguration(file1.toURI.toURL.toString)

              val config = context.getConfiguration
              val app1 = config.getAppender("List1").asInstanceOf[ListAppender]
              app1 should not be null
              app1.clear

              loggingService.updateLoggingConfiguration(url)

              val events1 = app1.getEvents.toList.map(_.getMessage.getFormattedMessage)

              events1 should contain(s"An attempt was made to switch to an invalid Logging Configuration file: $url")
            }
          } finally {
            cleanupLoggerContext(context)
          }
        }
      }
    }

    describe(s"On configuration modification with extension $ext, the system") {
      it("reloads the configuration.") {
        val context = LogManager.getContext(false).asInstanceOf[LoggerContext]
        try {
          yamlWrapper(ext) {
            val loggingService = new LoggingServiceImpl(mockConfigService)

            val content1 = Source.fromInputStream(this.getClass.getResourceAsStream(s"/LoggingServiceImplTest/log4j2-List1.$ext")).mkString
            val file1 = File.createTempFile("log4j2-", s".$ext")
            file1.deleteOnExit()
            Files.write(file1.toPath, content1.getBytes(StandardCharsets.UTF_8))

            loggingService.updateLoggingConfiguration(file1.toURI.toURL.toString)

            val config1 = context.getConfiguration
            val app1 = config1.getAppender("List1").asInstanceOf[ListAppender]
            app1 should not be null
            app1.clear

            LOG.error("ERROR LEVEL LOG STATEMENT 1")
            LOG.warn("WARN  LEVEL LOG STATEMENT 1")
            LOG.info("INFO  LEVEL LOG STATEMENT 1")
            LOG.debug("DEBUG LEVEL LOG STATEMENT 1")
            LOG.trace("TRACE LEVEL LOG STATEMENT 1")

            //Assert existing config works.
            val events1 = app1.getEvents.toList.map(_.getMessage.getFormattedMessage)
            events1 should contain("ERROR LEVEL LOG STATEMENT 1")
            events1 should contain("WARN  LEVEL LOG STATEMENT 1")
            events1 should not contain "INFO  LEVEL LOG STATEMENT 1"
            events1 should not contain "DEBUG LEVEL LOG STATEMENT 1"
            events1 should not contain "TRACE LEVEL LOG STATEMENT 1"

            //Have the logs be silent for like 6 seconds before changing the config, this lets Log4j flush itself so
            // it doesn't get upset about changing configs
            Thread.sleep(6000)

            //Change the configuration
            val content2 = Source.fromInputStream(this.getClass.getResourceAsStream(s"/LoggingServiceImplTest/log4j2-List2.$ext")).mkString
            Files.write(file1.toPath, content2.getBytes(StandardCharsets.UTF_8))

            //Assert that new stuff ended up in the right logs using an eventually
            implicit val patienceConfig = PatienceConfig(
              timeout = scaled(Span(40, Seconds)),
              interval = scaled(Span(1, Seconds))
            )

            eventually {
              app1.clear()

              LOG.error("ERROR LEVEL LOG STATEMENT 2")
              LOG.warn("WARN  LEVEL LOG STATEMENT 2")
              LOG.info("INFO  LEVEL LOG STATEMENT 2")
              LOG.debug("DEBUG LEVEL LOG STATEMENT 2")
              LOG.trace("TRACE LEVEL LOG STATEMENT 2")

              events1 should not contain "ERROR LEVEL LOG STATEMENT 2"
              events1 should not contain "WARN  LEVEL LOG STATEMENT 2"
              events1 should not contain "INFO  LEVEL LOG STATEMENT 2"
              events1 should not contain "DEBUG LEVEL LOG STATEMENT 2"
              events1 should not contain "TRACE LEVEL LOG STATEMENT 2"

              val config2 = context.getConfiguration
              val app2 = config2.getAppender("List2").asInstanceOf[ListAppender]
              app2 should not be null
              val events2 = app2.getEvents.toList.map(_.getMessage.getFormattedMessage)
              events2 should not contain "ERROR LEVEL LOG STATEMENT 1"
              events2 should not contain "WARN  LEVEL LOG STATEMENT 1"
              events2 should not contain "INFO  LEVEL LOG STATEMENT 1"
              events2 should not contain "DEBUG LEVEL LOG STATEMENT 1"
              events2 should not contain "TRACE LEVEL LOG STATEMENT 1"
              events2 should contain("ERROR LEVEL LOG STATEMENT 2")
              events2 should contain("WARN  LEVEL LOG STATEMENT 2")
              events2 should contain("INFO  LEVEL LOG STATEMENT 2")
              events2 should contain("DEBUG LEVEL LOG STATEMENT 2")
              events2 should not contain "TRACE LEVEL LOG STATEMENT 2"
            }
          }
        } finally {
          cleanupLoggerContext(context)
        }
      }
    }
  }
}
