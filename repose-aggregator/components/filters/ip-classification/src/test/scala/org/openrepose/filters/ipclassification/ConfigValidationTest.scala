package org.openrepose.filters.ipclassification

import org.junit.runner.RunWith
import org.openrepose.filters.ipclassification.config.IpClassificationConfig
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class ConfigValidationTest extends FunSpec with Matchers {

  //Only testing that valid config is valid
  // That the default works
  // And that the XSD 1.1 assertions work

  describe("verification of XSD validations") {
    it("returns a config object when given a valid configuration") {
      val validConfig =
        """<?xml version="1.0" encoding="UTF-8"?>
          |<ip-classification xmlns="http://docs.openrepose.org/repose/ip-classification/v1.0">
          |    <header-name>x-pp-group</header-name>
          |
          |    <classifications>
          |        <classification label="sample-group" ipv4-cidr="192.168.1.0/24 192.168.0.1/32"/>
          |        <classification label="sample-ipv6-group" ipv6-cidr="2001:db8::/48"/>
          |        <classification label="bolth-group" ipv4-cidr="10.10.220.0/24" ipv6-cidr="2001:1938:80:bc::1/64"/>
          |        <classification label="ipv4-match-all" ipv4-cidr="0.0.0.0/0"/>
          |    </classifications>
          |</ip-classification>
        """.stripMargin
      val resultant = Marshaller.configFromString(validConfig)
      resultant shouldNot be(null)
      resultant.getClass should equal(classOf[IpClassificationConfig])
    }
    it("returns a default header name when one is not specified") {
      val validConfig =
        """<?xml version="1.0" encoding="UTF-8"?>
          |<ip-classification xmlns="http://docs.openrepose.org/repose/ip-classification/v1.0">
          |
          |    <classifications>
          |        <classification label="sample-group" ipv4-cidr="192.168.1.0/24 192.168.0.1/32"/>
          |        <classification label="sample-ipv6-group" ipv6-cidr="2001:db8::/48"/>
          |        <classification label="bolth-group" ipv4-cidr="10.10.220.0/24" ipv6-cidr="2001:1938:80:bc::1/64"/>
          |        <classification label="ipv4-match-all" ipv4-cidr="0.0.0.0/0"/>
          |    </classifications>
          |</ip-classification>
        """.stripMargin
      val resultant = Marshaller.configFromString(validConfig)
      resultant shouldNot be(null)

      resultant.getHeaderName should be("x-pp-group")
    }
    it("validation fails when not given an ipv4-cidr nor an ipv6-cidr") {
      val invalidConfig =
        """<?xml version="1.0" encoding="UTF-8"?>
          |<ip-classification xmlns="http://docs.openrepose.org/repose/ip-classification/v1.0">
          |
          |    <classifications>
          |        <classification label="sample-group"/>
          |    </classifications>
          |</ip-classification>
        """.stripMargin

      intercept[ClassCastException] {
        Marshaller.configFromString(invalidConfig)
      }
    }
  }
}
