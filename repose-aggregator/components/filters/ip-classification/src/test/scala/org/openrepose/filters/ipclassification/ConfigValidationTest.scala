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
          |    <group-header-name quality="0.5">group-header</group-header-name>
          |    <user-header-name quality="0.5">user-header</user-header-name>
          |    <classifications>
          |        <classification label="sample-group">
          |            <cidr-ip>192.168.1.0/24</cidr-ip>
          |            <cidr-ip>192.168.0.1/32</cidr-ip>
          |        </classification>
          |        <classification label="sample-ipv6-group">
          |            <cidr-ip>2001:db8::/48</cidr-ip>
          |        </classification>
          |        <classification label="bolth-group">
          |            <cidr-ip>10.10.220.0/24</cidr-ip>
          |            <cidr-ip>2001:1938:80:bc::1/64</cidr-ip>
          |        </classification>
          |        <classification label="ipv4-match-all">
          |            <cidr-ip>0.0.0.0/0</cidr-ip>
          |        </classification>
          |    </classifications>
          |</ip-classification>
        """.stripMargin
      val resultant = Marshaller.configFromString(validConfig)
      resultant shouldNot be(null)
      resultant.getClass should equal(classOf[IpClassificationConfig])

      //Verify the group settings and such are not the defaults
      resultant.getUserHeaderName.getValue should equal("user-header")
      resultant.getUserHeaderName.getQuality should equal(0.5D)
      resultant.getGroupHeaderName.getValue should equal("group-header")
      resultant.getGroupHeaderName.getQuality should equal(0.5D)
    }
    //TODO: IF we want default true values, THEN we have to re-work where the defaults are specified; because XSD
    //      IF the element is present but empty, THEN you get default values.
    //      IF the element is NOT present, THEN you get a NULL value back.
    it("returns a default group header name when only an empty element is specified") {
      val validConfig =
        """<?xml version="1.0" encoding="UTF-8"?>
          |<ip-classification xmlns="http://docs.openrepose.org/repose/ip-classification/v1.0">
          |    <group-header-name/>
          |    <classifications>
          |        <classification label="ipv4-match-all">
          |            <cidr-ip>0.0.0.0/0</cidr-ip>
          |        </classification>
          |    </classifications>
          |</ip-classification>
        """.stripMargin
      val resultant = Marshaller.configFromString(validConfig)
      resultant shouldNot be(null)

      //Cannot actually get a default value when it's an element :(
      resultant.getGroupHeaderName.getValue should be("x-pp-groups")
      resultant.getGroupHeaderName.getQuality should be(0.4)
    }
    //TODO: IF we want default true values, THEN we have to re-work where the defaults are specified; because XSD
    //      IF the element is present but empty, THEN you get default values.
    //      IF the element is NOT present, THEN you get a NULL value back.
    it("returns a default user header name when only an empty element is specified") {
      val validConfig =
        """<?xml version="1.0" encoding="UTF-8"?>
          |<ip-classification xmlns="http://docs.openrepose.org/repose/ip-classification/v1.0">
          |    <user-header-name/>
          |    <classifications>
          |        <classification label="ipv4-match-all">
          |            <cidr-ip>0.0.0.0/0</cidr-ip>
          |        </classification>
          |    </classifications>
          |</ip-classification>
        """.stripMargin
      val resultant = Marshaller.configFromString(validConfig)
      resultant shouldNot be(null)

      //Cannot actually get a default value when it's an element :(
      resultant.getUserHeaderName.getValue should be("x-pp-user")
      resultant.getUserHeaderName.getQuality should be(0.4)
    }

    it("validation fails when not given any cidr-ip elements") {
      val invalidConfig =
        """<?xml version="1.0" encoding="UTF-8"?>
          |<ip-classification xmlns="http://docs.openrepose.org/repose/ip-classification/v1.0">
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
