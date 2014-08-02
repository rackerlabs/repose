package org.openrepose.servo

import org.junit.runner.RunWith
import org.scalatest.{Matchers, FunSpec}
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SystemModelParserTest extends FunSpec with Matchers {
  describe("A simpler parser for the system model") {
    it("returns a list of the local nodes when the XML is correct") {
      pending
    }
    describe("provides detailed failure messages"){
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
  }
}
