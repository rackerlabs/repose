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
package org.openrepose.filters.ipuser.config

import java.net.URL

import org.junit.runner.RunWith
import org.openrepose.commons.test.ConfigurationTest
import org.scalatestplus.junit.JUnitRunner
import org.xml.sax.SAXParseException

@RunWith(classOf[JUnitRunner])
class IpUserSchemaTest extends ConfigurationTest {
  override val schema: URL = getClass.getResource("/META-INF/schema/config/ip-user.xsd")
  override val exampleConfig: URL = getClass.getResource("/META-INF/schema/examples/ip-user.cfg.xml")
  override val jaxbContextPath: String = classOf[ObjectFactory].getPackage.getName

  describe("schema validation") {
    it("should successfully validate when given a complex configuration") {
      val config =
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
      validator.validateConfigString(config)
    }

    //TODO: IF we want default true values, THEN we have to re-work where the defaults are specified; because XSD
    //      IF the element is present but empty, THEN you get default values.
    //      IF the element is NOT present, THEN you get a NULL value back.
    it("should successfully validate when an empty group header element is specified") {
      val config =
        """<?xml version="1.0" encoding="UTF-8"?>
          |<ip-user xmlns="http://docs.openrepose.org/repose/ip-user/v1.0">
          |    <group-header/>
          |    <group name="ipv4-match-all">
          |        <cidr-ip>0.0.0.0/0</cidr-ip>
          |    </group>
          |</ip-user>
        """.stripMargin
      validator.validateConfigString(config)
    }

    //TODO: IF we want default true values, THEN we have to re-work where the defaults are specified; because XSD
    //      IF the element is present but empty, THEN you get default values.
    //      IF the element is NOT present, THEN you get a NULL value back.
    it("should successfully validate when an empty user header element is specified") {
      val config =
        """<?xml version="1.0" encoding="UTF-8"?>
          |<ip-user xmlns="http://docs.openrepose.org/repose/ip-user/v1.0">
          |    <user-header/>
          |    <group name="ipv4-match-all">
          |        <cidr-ip>0.0.0.0/0</cidr-ip>
          |    </group>
          |</ip-user>
        """.stripMargin
      validator.validateConfigString(config)
    }

    it("should fail to validate when not given any group elements") {
      val config =
        """<?xml version="1.0" encoding="UTF-8"?>
          |<ip-user xmlns="http://docs.openrepose.org/repose/ip-user/v1.0">
          |</ip-user>
        """.stripMargin
      val exception = intercept[SAXParseException] {
        validator.validateConfigString(config)
      }
      val exceptionMessage = exception.getLocalizedMessage
      exceptionMessage should include("is not complete")
      exceptionMessage should include("group")
      exceptionMessage should include("is expected")
    }

    it("should fail to validate when not given any cidr-ip elements") {
      val config =
        """<?xml version="1.0" encoding="UTF-8"?>
          |<ip-user xmlns="http://docs.openrepose.org/repose/ip-user/v1.0">
          |    <group name="sample-group"/>
          |</ip-user>
        """.stripMargin
      val exception = intercept[SAXParseException] {
        validator.validateConfigString(config)
      }
      val exceptionMessage = exception.getLocalizedMessage
      exceptionMessage should include("is not complete")
      exceptionMessage should include("cidr-ip")
      exceptionMessage should include("is expected")
    }
  }
}
