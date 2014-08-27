package org.openrepose.servo

import org.junit.runner.RunWith
import org.scalatest.{Matchers, FunSpec}
import org.scalatest.junit.JUnitRunner

import scala.xml.XML

@RunWith(classOf[JUnitRunner])
class JettyConfigGeneratorTest extends FunSpec with Matchers with TestUtils {

  import scala.xml.Utility.trim

  //Reference: http://git.eclipse.org/c/jetty/org.eclipse.jetty.project.git/plain/jetty-server/src/main/config/etc/

  val httpNode = ReposeNode("clusterId", "nodeId", "localhost", Some(8080), None)
  val httpsNode = ReposeNode("clusterId", "nodeId", "localhost", None, Some(8081))
  val bothNode = ReposeNode("clusterId", "nodeId", "localhost", Some(8080), Some(8081))

  describe("Jetty configuration generator for a ReposeNode and a container.cfg.xml") {
    it("generates a ssl-config.xml when given a Keystore Config") {
      val jettyConfig = new JettyConfigGenerator("/config/root", httpNode, Some(KeystoreConfig("keystore", "testKeystorePassword", "testKeyPassword")))

      val expectedXml = XML.loadString(resourceContent("/jettyConfigs/jetty-ssl.xml"))

      jettyConfig.sslConfig.map { content =>
        val receivedXml = XML.loadString(content)
        trim(receivedXml) shouldBe trim(expectedXml)
      } getOrElse {
        fail("should have gotten back some xml content!")
      }
    }
    it("does not generate a ssl-config.xml when not given a Keystore Config") {
      val jettyConfig = new JettyConfigGenerator("/config/root", httpNode, keystoreConfig = None)

      jettyConfig.sslConfig shouldBe None
    }
    describe("with a keystore") {
      val keystoreConfig = Some(KeystoreConfig("keystore", "testKeystorePassword", "testKeyPassword"))

      it("generates a Jetty config.xml for HTTP") {
        val jettyConfig = new JettyConfigGenerator("/config/root", httpNode, keystoreConfig)

        val content = XML.loadString(jettyConfig.jettyConfig)
        val expectedXml = XML.loadString(resourceContent("/jettyConfigs/jetty-http.xml"))

        trim(content) shouldBe trim(expectedXml)
      }
      it("generates a Jetty config.xml for HTTPS") {
        pending
      }
    }
    describe("without a keystore") {
      val keystoreConfig = None
      it("generates a Jetty config.xml for HTTP") {
        pending
      }
      it("Fails to generate a Jetty config.xml for HTTPS") {
        pending
      }
    }
  }
}
