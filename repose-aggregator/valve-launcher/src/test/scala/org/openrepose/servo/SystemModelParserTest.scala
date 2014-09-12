package org.openrepose.servo

import org.junit.runner.RunWith
import org.scalatest.{Matchers, FunSpec}
import org.scalatest.junit.JUnitRunner

import scala.io.Source
import scala.util.{Failure, Success}

@RunWith(classOf[JUnitRunner])
class SystemModelParserTest extends FunSpec with Matchers with TestUtils {

  def shouldFailWith(content: String, f: Throwable => Unit) = {
    val smp = new SystemModelParser(content)
    val result = smp.localNodes

    result match {
      case Failure(x) => {
        f(x)
      }
      case Success(x) => {
        fail("Processing should have failed!")
      }
    }
  }

  describe("A simpler parser for the system model") {
    it("returns a list of the local nodes when the XML is correct") {
      val smp = new SystemModelParser(resourceContent("/system-model-test/valid-system-model.cfg.xml"))
      val result = smp.localNodes

      result match {
        case Failure(x) => {
          fail("Should not have failed to parse! Reason: " + x.getMessage)
        }
        case Success(x) => {
          x.isEmpty should be(false)
        }
      }
    }
    describe("provides detailed failure messages for nodes in a cluster") {

      it("requires that the node IDs be unique") {
        shouldFailWith(resourceContent("/system-model-test/conflicting-nodes.xml"), failure => {
          failure should equal(SystemModelParseException("Conflicting local node IDs found!"))
        })
      }
      it("requires an http port or an https port") {
        shouldFailWith(resourceContent("/system-model-test/no-port-at-all.xml"), failure => {
          failure should equal(SystemModelParseException("No port configured on a local node!"))
        })
      }
      it("requires that at least one node is local") {
        shouldFailWith(resourceContent("/system-model-test/no-local-node.xml"), failure => {
          failure should equal(SystemModelParseException("No local node(s) found!"))
        })
      }
      it("requires that local nodes don't conflict ports") {
        shouldFailWith(resourceContent("/system-model-test/duplicated-port.xml"), failure => {
          failure should equal(SystemModelParseException("Conflicting local node ports found!"))
        })
      }
    }
    describe("provides detailed failure messages for clusters and inter node conflicts") {
      it("requires that all local ports be unique across clusters") {
        shouldFailWith(resourceContent("/system-model-test/cluster-conflicting-ports.xml"), failure => {
          failure should equal(SystemModelParseException("Conflicting local node ports found!"))
        })
      }
      it("requires that cluster IDs be unique") {
        shouldFailWith(resourceContent("/system-model-test/cluster-conflicting-ids.xml"), failure => {
          failure should equal(SystemModelParseException("Conflicting cluster IDs found!"))
        })
      }
    }
    it("returns a failure when it's not valid xml") {
      shouldFailWith(resourceContent("/system-model-test/not-valid-xml.xml"), failure => {
        failure.getMessage should equal("Unable to parse the system-model")
      })
    }
  }
}
