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

package org.openrepose.core.service.httpclient.config

import java.net.URL

import org.junit.runner.RunWith
import org.openrepose.commons.test.ConfigurationTest
import org.scalatest.junit.JUnitRunner
import org.xml.sax.SAXParseException

@RunWith(classOf[JUnitRunner])
class HttpConnectionPoolSchemaTest extends ConfigurationTest {
  override val schema: URL = getClass.getResource("/META-INF/schema/config/http-connection-pool.xsd")
  override val exampleConfig: URL = getClass.getResource("/META-INF/schema/examples/http-connection-pool.cfg.xml")
  override val jaxbContextPath: String = classOf[ObjectFactory].getPackage.getName

  describe("schema validation") {
    it("should successfully validate config containing pools with unique IDs and one default pool") {
      val config = """<http-connection-pools xmlns="http://docs.openrepose.org/repose/http-connection-pool/v1.0">
                     |    <pool id="apple" default="true"/>
                     |    <pool id="banana" default="false"/>
                     |    <pool id="orange"/>
                     |</http-connection-pools>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should reject config if any of the pools have the same ID") {
      val config = """<http-connection-pools xmlns="http://docs.openrepose.org/repose/http-connection-pool/v1.0">
                     |    <pool id="apple" default="true"/>
                     |    <pool id="banana" default="false"/>
                     |    <pool id="apple"/>
                     |</http-connection-pools>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("Pools must have unique ids")
    }

    it("should reject config if there are no default pools") {
      val config = """<http-connection-pools xmlns="http://docs.openrepose.org/repose/http-connection-pool/v1.0">
                     |    <pool id="apple" default="false"/>
                     |    <pool id="banana" default="false"/>
                     |    <pool id="orange"/>
                     |</http-connection-pools>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("One and only one default pool must be defined")
    }

    it("should reject config if there is more than one default pool") {
      val config = """<http-connection-pools xmlns="http://docs.openrepose.org/repose/http-connection-pool/v1.0">
                     |    <pool id="apple" default="true"/>
                     |    <pool id="banana" default="true"/>
                     |    <pool id="orange"/>
                     |</http-connection-pools>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("One and only one default pool must be defined")
    }

    it("should successfully validate config with max connections per route set to less than total max connections") {
      val config = """<http-connection-pools xmlns="http://docs.openrepose.org/repose/http-connection-pool/v1.0">
                     |    <pool id="apple"
                     |        default="true"
                     |        http.conn-manager.max-per-route="8"
                     |        http.conn-manager.max-total="10"/>
                     |</http-connection-pools>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should successfully validate config with max connections per route set to the same as total max connections") {
      val config = """<http-connection-pools xmlns="http://docs.openrepose.org/repose/http-connection-pool/v1.0">
                     |    <pool id="apple"
                     |        default="true"
                     |        http.conn-manager.max-per-route="10"
                     |        http.conn-manager.max-total="10"/>
                     |</http-connection-pools>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should reject config with max connections per route set to more than total max connections") {
      val config = """<http-connection-pools xmlns="http://docs.openrepose.org/repose/http-connection-pool/v1.0">
                     |    <pool id="apple"
                     |        default="true"
                     |        http.conn-manager.max-per-route="12"
                     |        http.conn-manager.max-total="10"/>
                     |</http-connection-pools>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("Max connections per route must be less than or equal to total max connections")
    }

    it("should reject config missing keystore-filename") {
      val config = """<http-connection-pools xmlns="http://docs.openrepose.org/repose/http-connection-pool/v1.0">
                     |    <pool id="clientAuthentication"
                     |          default="false"
                     |          keystore-password="password"
                     |          key-password="secret"
                     |          truststore-filename="truststore.jks"
                     |          truststore-password="trusting"/>
                     |</http-connection-pools>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("IF a keystore filename, password, or key password is provided, THEN all must be provided")
    }

    it("should reject config missing keystore-password") {
      val config = """<http-connection-pools xmlns="http://docs.openrepose.org/repose/http-connection-pool/v1.0">
                     |    <pool id="clientAuthentication"
                     |          default="false"
                     |          keystore-filename="keystore.jks"
                     |          key-password="secret"
                     |          truststore-filename="truststore.jks"
                     |          truststore-password="trusting"/>
                     |</http-connection-pools>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("IF a keystore filename, password, or key password is provided, THEN all must be provided")
    }

    it("should reject config missing key-password") {
      val config = """<http-connection-pools xmlns="http://docs.openrepose.org/repose/http-connection-pool/v1.0">
                     |    <pool id="clientAuthentication"
                     |          default="false"
                     |          keystore-filename="keystore.jks"
                     |          keystore-password="password"
                     |          truststore-filename="truststore.jks"
                     |          truststore-password="trusting"/>
                     |</http-connection-pools>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("IF a keystore filename, password, or key password is provided, THEN all must be provided")
    }

    it("should not validate if chunked-encoding is configured wrong") {
      val xml = """<http-connection-pools xmlns='http://docs.openrepose.org/repose/http-connection-pool/v1.0'>
                  |    <pool id='default'
                  |          default='true'
                  |          chunked-encoding='blah'
                  |          http.conn-manager.max-per-route='200'
                  |          http.conn-manager.max-total='199'/>
                  |</http-connection-pools>
                """.stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(xml)
      }.getLocalizedMessage should include("'blah' is not facet-valid with respect to enumeration")
    }
  }
}
