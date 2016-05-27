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
import org.openrepose.filters.ipuser.config.IpUserConfig
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
          |<ip-user xmlns="http://docs.openrepose.org/repose/ip-user/v1.0">
          |    <user-header name="user-header" quality="0.5"/>
          |    <group-header name="group-header" quality="0.5"/>
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
      val resultant = Marshaller.configFromString(validConfig)
      resultant shouldNot be(null)
      resultant.getClass should equal(classOf[IpUserConfig])

      //Verify the group settings and such are not the defaults
      resultant.getUserHeader.getName should equal("user-header")
      resultant.getUserHeader.getQuality should equal(0.5D)
      resultant.getGroupHeader.getName should equal("group-header")
      resultant.getGroupHeader.getQuality should equal(0.5D)
    }
    //TODO: IF we want default true values, THEN we have to re-work where the defaults are specified; because XSD
    //      IF the element is present but empty, THEN you get default values.
    //      IF the element is NOT present, THEN you get a NULL value back.
    it("returns a default group header name when only an empty element is specified") {
      val validConfig =
        """<?xml version="1.0" encoding="UTF-8"?>
          |<ip-user xmlns="http://docs.openrepose.org/repose/ip-user/v1.0">
          |    <group-header/>
          |    <group name="ipv4-match-all">
          |        <cidr-ip>0.0.0.0/0</cidr-ip>
          |    </group>
          |</ip-user>
        """.stripMargin
      val resultant = Marshaller.configFromString(validConfig)
      resultant shouldNot be(null)

      //Cannot actually get a default value when it's an element :(
      resultant.getGroupHeader.getName should be("x-pp-groups")
      resultant.getGroupHeader.getQuality should be(0.4)
    }
    //TODO: IF we want default true values, THEN we have to re-work where the defaults are specified; because XSD
    //      IF the element is present but empty, THEN you get default values.
    //      IF the element is NOT present, THEN you get a NULL value back.
    it("returns a default user header name when only an empty element is specified") {
      val validConfig =
        """<?xml version="1.0" encoding="UTF-8"?>
          |<ip-user xmlns="http://docs.openrepose.org/repose/ip-user/v1.0">
          |    <user-header/>
          |    <group name="ipv4-match-all">
          |        <cidr-ip>0.0.0.0/0</cidr-ip>
          |    </group>
          |</ip-user>
        """.stripMargin
      val resultant = Marshaller.configFromString(validConfig)
      resultant shouldNot be(null)

      //Cannot actually get a default value when it's an element :(
      resultant.getUserHeader.getName should be("x-pp-user")
      resultant.getUserHeader.getQuality should be(0.4)
    }

    it("validation fails when not given any cidr-ip elements") {
      val invalidConfig =
        """<?xml version="1.0" encoding="UTF-8"?>
          |<ip-user xmlns="http://docs.openrepose.org/repose/ip-user/v1.0">
          |    <group name="sample-group"/>
          |</ip-user>
        """.stripMargin

      intercept[ClassCastException] {
        Marshaller.configFromString(invalidConfig)
      }
    }
  }
}
