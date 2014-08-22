package org.openrepose.servo

import org.junit.runner.RunWith
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.junit.JUnitRunner

import scala.util.{Failure, Success}

@RunWith(classOf[JUnitRunner])
class ContainerConfigParserTest extends FunSpec with Matchers with TestUtils{
  describe("Successful parsing of the container.cfg.xml will extract") {
    it("provides the log file name") {
      val parser = new ContainerConfigParser(resourceContent("/container-config-test/no-keystore.xml"))

      val config = parser.config
      config match {
        case Success(containerConfig) => {
          containerConfig.logFileName shouldBe "log4j.properties"
        }
        case Failure(x) => {
          fail("Parsing should not have failed!", x)
        }
      }
    }
    it("provides me the keystore configuration") {
      val parser = new ContainerConfigParser(resourceContent("/container-config-test/with-keystore.xml"))

      val config = parser.config
      println(s"Config: $config")
      config match {
        case Success(ContainerConfig(_, Some(keystoreConfig))) => {
          keystoreConfig.filename shouldBe "someKeystore"
          keystoreConfig.keystorePassword shouldBe "lePassword"
          keystoreConfig.keyPassword shouldBe "leKeyPassword"
        }
        case Success(ContainerConfig(_, None)) => {
          fail("Should have gotten a keystore config back")
        }
        case Failure(x) => {
          fail("Parsing should not have failed!", x)
        }
      }
    }
    it("wraps failures nicely") {
      val parser = new ContainerConfigParser(resourceContent("/container-config-test/bad-config.xml"))

      val config = parser.config
      config match {
        case Success(_) =>
          fail("Should not have returned a successful parsing!")
        case Failure(x) =>
          x.getClass shouldBe classOf[ContainerConfigParseException]
      }

    }
  }
}
