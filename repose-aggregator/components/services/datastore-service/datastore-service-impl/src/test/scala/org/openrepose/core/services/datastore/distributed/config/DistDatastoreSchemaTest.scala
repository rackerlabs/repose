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

package org.openrepose.core.services.datastore.distributed.config

import java.net.URL

import org.junit.runner.RunWith
import org.openrepose.commons.test.ConfigurationTest
import org.scalatest.junit.JUnitRunner
import org.xml.sax.SAXParseException

@RunWith(classOf[JUnitRunner])
class DistDatastoreSchemaTest extends ConfigurationTest {
  override val schema: URL = getClass.getResource("/META-INF/schema/dist-datastore/dist-datastore.xsd")
  override val exampleConfig: URL = getClass.getResource("/META-INF/schema/examples/dist-datastore.cfg.xml")
  override val jaxbContextPath: String = classOf[ObjectFactory].getPackage.getName

  describe("schema validation") {
    it("should successfully validate config with allow-all set to true and the allow list empty") {
      val config = """<distributed-datastore xmlns='http://docs.openrepose.org/repose/distributed-datastore/v1.0'>
                     |    <allowed-hosts allow-all="true"/>
                     |    <port-config>
                     |        <port port="3888"/>
                     |    </port-config>
                     |</distributed-datastore>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should successfully validate config with allow-all set to false and the allow list populated") {
      val config = """<distributed-datastore xmlns='http://docs.openrepose.org/repose/distributed-datastore/v1.0'>
                     |    <allowed-hosts allow-all="false">
                     |        <allow host="127.0.0.1"/>
                     |    </allowed-hosts>
                     |    <port-config>
                     |        <port port="3888"/>
                     |    </port-config>
                     |</distributed-datastore>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should reject config with allow-all set to true and the allow list populated") {
      val config = """<distributed-datastore xmlns='http://docs.openrepose.org/repose/distributed-datastore/v1.0'>
                     |    <allowed-hosts allow-all="true">
                     |        <allow host="127.0.0.1"/>
                     |    </allowed-hosts>
                     |    <port-config>
                     |        <port port="3888"/>
                     |    </port-config>
                     |</distributed-datastore>""".stripMargin
      val exception = intercept[SAXParseException] {
        validator.validateConfigString(config)
      }
      exception.getLocalizedMessage should include ("If allow-all is true then allow elements not allowed")
    }

    it("should successfully validate config with no default port") {
      val config = """<distributed-datastore xmlns='http://docs.openrepose.org/repose/distributed-datastore/v1.0'>
                     |    <allowed-hosts allow-all="true"/>
                     |    <port-config>
                     |        <port port="3888" node="1"/>
                     |        <port port="8333" node="2"/>
                     |    </port-config>
                     |</distributed-datastore>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should successfully validate config with one default port") {
      val config = """<distributed-datastore xmlns='http://docs.openrepose.org/repose/distributed-datastore/v1.0'>
                     |    <allowed-hosts allow-all="true"/>
                     |    <port-config>
                     |        <port port="3888"/>
                     |        <port port="8333" node="2"/>
                     |    </port-config>
                     |</distributed-datastore>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should reject config with more than one default port") {
      val config = """<distributed-datastore xmlns='http://docs.openrepose.org/repose/distributed-datastore/v1.0'>
                     |    <allowed-hosts allow-all="true"/>
                     |    <port-config>
                     |        <port port="3888"/>
                     |        <port port="8333"/>
                     |    </port-config>
                     |</distributed-datastore>""".stripMargin
      val exception = intercept[SAXParseException] {
        validator.validateConfigString(config)
      }
      exception.getLocalizedMessage should include ("At most one default port may be defined")
    }

    it("should reject config missing keystore-filename") {
      val config =
        """<distributed-datastore xmlns='http://docs.openrepose.org/repose/distributed-datastore/v1.0'
          |                       keystore-password="password"
          |                       key-password="secret"
          |                       truststore-filename="truststore.jks"
          |                       truststore-password="trusting">
          |    <allowed-hosts allow-all="true"/>
          |    <port-config>
          |        <port port="3888"/>
          |    </port-config>
          |</distributed-datastore>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include("IF a keystore filename, password, or key password is provided, THEN all must be provided")
    }

    it("should reject config missing keystore-password") {
      val config =
        """<distributed-datastore xmlns='http://docs.openrepose.org/repose/distributed-datastore/v1.0'
          |                       keystore-filename="keystore.jks"
          |                       key-password="secret"
          |                       truststore-filename="truststore.jks"
          |                       truststore-password="trusting">
          |    <allowed-hosts allow-all="true"/>
          |    <port-config>
          |        <port port="3888"/>
          |    </port-config>
          |</distributed-datastore>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include("IF a keystore filename, password, or key password is provided, THEN all must be provided")
    }

    it("should reject config missing key-password") {
      val config =
        """<distributed-datastore xmlns='http://docs.openrepose.org/repose/distributed-datastore/v1.0'
          |                       keystore-filename="keystore.jks"
          |                       keystore-password="password"
          |                       truststore-filename="truststore.jks"
          |                       truststore-password="trusting">
          |    <allowed-hosts allow-all="true"/>
          |    <port-config>
          |        <port port="3888"/>
          |    </port-config>
          |</distributed-datastore>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include("IF a keystore filename, password, or key password is provided, THEN all must be provided")
    }
  }
}
