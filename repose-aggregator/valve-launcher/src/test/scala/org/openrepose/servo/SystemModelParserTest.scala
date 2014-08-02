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

  val validModel = resourceContent("/system-model-test/valid-system-model.cfg.xml")

  describe("A simpler parser for the system model") {
    it("returns a list of the local nodes when the XML is correct") {
      val smp = new SystemModelParser(validModel)
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
      it("requires that the node IDs be unique") {
        pending
      }
      it("requires an http port or an https port") {
        pending
      }
      it("requires that at least one node is local") {
        pending
      }
      it("requires that local nodes don't conflict ports") {
        pending
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
