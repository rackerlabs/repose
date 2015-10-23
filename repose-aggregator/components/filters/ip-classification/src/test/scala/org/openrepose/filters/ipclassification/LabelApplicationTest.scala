package org.openrepose.filters.ipclassification

import org.junit.runner.RunWith
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class LabelApplicationTest extends FunSpec with Matchers {
  describe("A configured filter with a catch all ipv4 rule") {
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
    val filter = new IPClassificationFilter(null) //Not going to use the config Service
    filter.configurationUpdated(Marshaller.configFromString(validConfig))

    it("returns the correct label for an IPv4 address in 10.10.220.0/24") {
      filter.getClassificationLabel("10.10.220.101") should equal(Some("bolth-group"))
    }
    it("returns the correct label for an IPv4 address in 192.168.1.0/24") {
      filter.getClassificationLabel("192.168.1.1") should equal(Some("sample-group"))
    }
    it("returns the correct label for an IPv4 address in 192.168.0.1/32") {
      filter.getClassificationLabel("192.168.0.1") should equal(Some("sample-group"))
    }
    it("returns the correct label for an IPv6 address in 2001:1938:80:bc::1/64") {
      filter.getClassificationLabel("2001:1938:80:bc::DEAD:BEEF") should equal(Some("bolth-group"))
    }
    it("returns the correct label for the catch all IPv4 entry") {
      filter.getClassificationLabel("8.8.8.8") should equal(Some("ipv4-match-all"))
    }
    it("Will not return a catch all for an IPv6 address") {
      filter.getClassificationLabel("2002::1") should equal(None)
    }
  }
  describe("A configured filter without a catch all ipv4 rule") {
    val validConfig =
      """<?xml version="1.0" encoding="UTF-8"?>
        |<ip-classification xmlns="http://docs.openrepose.org/repose/ip-classification/v1.0">
        |    <header-name>x-pp-group</header-name>
        |
        |    <classifications>
        |        <classification label="sample-group" ipv4-cidr="192.168.1.0/24 192.168.0.1/32"/>
        |        <classification label="sample-ipv6-group" ipv6-cidr="2001:db8::/48"/>
        |        <classification label="bolth-group" ipv4-cidr="10.10.220.0/24" ipv6-cidr="2001:1938:80:bc::1/64"/>
        |        <classification label="ipv6-match-all" ipv4-cidr="0::0/0"/>
        |    </classifications>
        |</ip-classification>
      """.stripMargin
    val filter = new IPClassificationFilter(null) //Not going to use the config Service
    filter.configurationUpdated(Marshaller.configFromString(validConfig))
    //TODO: is this desireable behavior? More logic is needed if not.
    it("returns the IpV6 catch all, since the servlet filter doesn't know that it's IPv6 or IPv4") {
      filter.getClassificationLabel("8.8.8.8") should equal(Some("ipv6-match-all"))
    }
    it("returns the ipv6 catchall label") {
      filter.getClassificationLabel("2002::1") should equal(Some("ipv6-match-all"))
    }
  }

}
