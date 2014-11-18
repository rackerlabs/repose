package org.openrepose.core.services.logging

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.appender.ConsoleAppender
import org.apache.logging.log4j.test.appender.ListAppender
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}
import org.slf4j.{Logger, LoggerFactory}

import scala.io.Source

@RunWith(classOf[JUnitRunner])
class LoggingServiceImpl2Test extends FunSpec with Matchers {

  import scala.collection.JavaConversions._

  private val LOG: Logger = LoggerFactory.getLogger(classOf[LoggingServiceImpl2Test].getName)

  describe("On Startup") {
    it("Loads a fully qualified URL.") {
      val loggingService = new LoggingServiceImpl()
      val context = LogManager.getContext(false).asInstanceOf[LoggerContext]

      val content = Source.fromInputStream(this.getClass.getResourceAsStream("/LoggingServiceImpl2Test/log4j2-List1.xml")).mkString
      val file = File.createTempFile("log4j2-", ".xml")
      Files.write(file.toPath, content.getBytes(StandardCharsets.UTF_8))

      loggingService.updateLoggingConfiguration(file.toURI.toURL.toString)

      val config = context.getConfiguration
      val app = ((config.getAppender("List1")).asInstanceOf[ListAppender])
      app should not be (null)
      app.clear

      LOG.error("ERROR LEVEL LOG STATEMENT 1")
      LOG.warn("WARN  LEVEL LOG STATEMENT 1")
      LOG.info("INFO  LEVEL LOG STATEMENT 1")
      LOG.debug("DEBUG LEVEL LOG STATEMENT 1")
      LOG.trace("TRACE LEVEL LOG STATEMENT 1")

      val events = app.getEvents.toList.map(_.getMessage.getFormattedMessage)

      events should contain("ERROR LEVEL LOG STATEMENT 1")
      events should contain("WARN  LEVEL LOG STATEMENT 1")
      events should not contain ("INFO  LEVEL LOG STATEMENT 1")
      events should not contain ("DEBUG LEVEL LOG STATEMENT 1")
      events should not contain ("TRACE LEVEL LOG STATEMENT 1")
    }

    it("Loads a relative pathed file.") {
      pending
    }

    it("Falls back to known good configuration on failure.") {
      pending
    }
  }

  describe("From know good state") {
    it("Switches to another known configuration.") {
      pending
    }

    it("Fails to switch when provided a remote URL.") {
      val loggingService = new LoggingServiceImpl()
      val loc = "http://www.example.com/log4j2.xml";
      loggingService.updateLoggingConfiguration(loc);
      val context = LogManager.getContext(false).asInstanceOf[LoggerContext]
      val config = context.getConfiguration
      val app = ((config.getAppender("Console")).asInstanceOf[ConsoleAppender])
      app should not be (null)

      //val events = app.getEvents.toList.map(_.getMessage.getFormattedMessage)

      //events should contain("FAILED LOUDLY")
    }
  }
}
