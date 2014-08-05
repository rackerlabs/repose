package org.openrepose.servo

import org.junit.runner.RunWith
import org.scalatest.{Matchers, FunSpec}
import org.scalatest.junit.JUnitRunner

import scala.io.Source

@RunWith(classOf[JUnitRunner])
class SystemModelParserTest extends FunSpec with Matchers {

  def resourceContent(resource:String) = {
    Source.fromInputStream(this.getClass.getResourceAsStream(resource)).mkString
  }

  describe("A simpler parser for the system model") {
    it("returns a list of the local nodes when the XML is correct") {
      val smp = new SystemModelParser(resourceContent("/system-model-test/valid-system-model.cfg.xml"))
      val result = smp.localNodes

      result match {
        case Right(x) => {
          fail("Should not have failed to parse! Errors:\n" + x)
        }
        case Left(x) => {
          x.isEmpty should be(false)
        }
      }
    }
    describe("provides detailed failure messages for nodes in a cluster"){
      def shouldFailWith(content:String, f:String => Unit) = {
        val smp = new SystemModelParser(content)
        val result = smp.localNodes

        result match {
          case Right(x) => {
            f(x)
          }
          case Left(x) => {
            fail("Processing should have failed!")
          }
        }
      }

      it("requires that the node IDs be unique") {
        shouldFailWith(resourceContent("/system-model-test/conflicting-nodes.xml"), failure => {
          failure should equal("Conflicting local node IDs found!")
        })
      }
      it("requires an http port or an https port") {
        shouldFailWith(resourceContent("/system-model-test/no-port-at-all.xml"), failure => {
          failure should equal("No port configured on a local node!")
        })
      }
      it("requires that at least one node is local") {
        shouldFailWith(resourceContent("/system-model-test/no-local-node.xml"), failure => {
          failure should equal("No local node(s) found!")
        })
      }
      it("requires that local nodes don't conflict ports") {
        shouldFailWith(resourceContent("/system-model-test/duplicated-port.xml"), failure => {
          failure should equal("Conflicting local node ports found!")
        })
      }
    }
    describe("provides detailed failure messages for clusters and inter node conflicts") {
      it("requires that all local ports be unique across clusters") {
        pending
      }
      it("requires that cluster IDs be unique") {
        pending
      }
    }
  }
}
