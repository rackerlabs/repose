package org.openrepose.servo

import org.junit.runner.RunWith
import org.scalatest.{Matchers, FunSpec}
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class JettyConfigGeneratorTest extends FunSpec with Matchers{

  //Reference: http://git.eclipse.org/c/jetty/org.eclipse.jetty.project.git/plain/jetty-server/src/main/config/etc/

  describe("Jetty configuration generator for a ReposeNode and a container.cfg.xml") {
    it("generates a ssl-config.xml when given a Keystore Config") {
      pending
    }
    it("does not generate a ssl-config.xml when not given a Keystore Config") {
      pending
    }
    describe("with a keystore") {
      it("generates a Jetty config.xml for HTTP") {
        pending
      }
      it("generates a Jetty config.xml for HTTPS") {
        pending
      }
    }
    describe("without a keystore") {
      it("generates a Jetty config.xml for HTTP") {
        pending
      }
      it("Fails to generate a Jetty config.xml for HTTPS") {
        pending
      }
    }
  }
}
