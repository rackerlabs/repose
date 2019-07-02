/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.filters.ipuser

import org.junit.runner.RunWith
import org.scalatest.{FunSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class LabelApplicationTest extends FunSpec with Matchers {
  describe("A configured filter with a catch all ipv4 rule") {
    val validConfig =
      """<?xml version="1.0" encoding="UTF-8"?>
        |<ip-user xmlns="http://docs.openrepose.org/repose/ip-user/v1.0">
        |    <group name="sample-group">
        |        <cidr-ip>192.168.1.0/24</cidr-ip>
        |        <cidr-ip>192.168.0.1/32</cidr-ip>
        |    </group>
        |    <group name="sample-ipv6-group">
        |        <cidr-ip>2001:db8::/48</cidr-ip>
        |    </group>
        |    <group name="bolth-group">
        |        <cidr-ip>10.10.220.0/24</cidr-ip>
        |        <cidr-ip>2001:1938:80:bc::1/64</cidr-ip>
        |    </group>
        |    <group name="ipv4-match-all">
        |        <cidr-ip>0.0.0.0/0</cidr-ip>
        |    </group>
        |</ip-user>
      """.stripMargin
    val filter = new IpUserFilter(null) //Not going to use the config Service
    filter.configurationUpdated(Marshaller.configFromString(validConfig))

    it("returns the correct label for an IPv4 address in 10.10.220.0/24") {
      filter.getClassificationLabel("10.10.220.101") shouldEqual Some("bolth-group")
    }
    it("returns the correct label for an IPv4 address in 192.168.1.0/24") {
      filter.getClassificationLabel("192.168.1.1") shouldEqual Some("sample-group")
    }
    it("returns the correct label for an IPv4 address in 192.168.0.1/32") {
      filter.getClassificationLabel("192.168.0.1") shouldEqual Some("sample-group")
    }
    it("returns the correct label for an IPv6 address in 2001:1938:80:bc::1/64") {
      filter.getClassificationLabel("2001:1938:80:bc::DEAD:BEEF") shouldEqual Some("bolth-group")
    }
    it("returns the correct label for the catch all IPv4 entry") {
      filter.getClassificationLabel("8.8.8.8") shouldEqual Some("ipv4-match-all")
    }
    it("Will not return a catch all for an IPv6 address") {
      filter.getClassificationLabel("2002::1") shouldEqual None
    }
  }
  describe("A configured filter with an ipv6 catch all rule") {
    val validConfig =
      """<?xml version="1.0" encoding="UTF-8"?>
        |<ip-user xmlns="http://docs.openrepose.org/repose/ip-user/v1.0">
        |    <group name="ipv6-match-all">
        |        <cidr-ip>0::0/0</cidr-ip>
        |    </group>
        |</ip-user>
      """.stripMargin
    val filter = new IpUserFilter(null) //Not going to use the config Service
    filter.configurationUpdated(Marshaller.configFromString(validConfig))

    it("returns the IpV6 catch all, since the servlet filter doesn't know that it's IPv6 or IPv4") {
      filter.getClassificationLabel("8.8.8.8") shouldEqual Some("ipv6-match-all")
    }
    it("returns the ipv6 catchall label") {
      filter.getClassificationLabel("2002::1") shouldEqual Some("ipv6-match-all")
    }
  }
}
