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
package org.openrepose.core.services.httpclient.impl

import java.net.URL

import org.junit.runner.RunWith
import org.openrepose.commons.test.ConfigurationTest
import org.openrepose.core.service.httpclient.config.ObjectFactory
import org.scalatest.junit.JUnitRunner
import org.xml.sax.SAXParseException

@RunWith(classOf[JUnitRunner])
class HttpClientSchemaTest extends ConfigurationTest {
  override val schema: URL = getClass.getResource("/META-INF/schema/config/http-connection-pool.xsd")
  override val exampleConfig: URL = getClass.getResource("/META-INF/schema/examples/http-connection-pool.cfg.xml")
  override val jaxbContextPath: String = classOf[ObjectFactory].getPackage.getName

  describe("schema validation") {
    it("should not validate when config has non-unique pool IDs") {
      val xml =
        """<http-connection-pools xmlns='http://docs.openrepose.org/repose/http-connection-pool/v1.0'>
          |    <pool id='default' default='true'/>
          |    <pool id='default' default='false'/>
          |</http-connection-pools>
        """.stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(xml)
      }.getLocalizedMessage should include("Pools must have unique ids")
    }

    it("should not validate if more than one default pool exists") {
      val xml =
        """<http-connection-pools xmlns='http://docs.openrepose.org/repose/http-connection-pool/v1.0'>
          |    <pool id='default' default='true'/>
          |    <pool id='default2' default='true'/>
          |</http-connection-pools>
        """.stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(xml)
      }.getLocalizedMessage should include("One and only one default pool must be defined")
    }

    it("should not validate if no default pool") {
      val xml =
        """<http-connection-pools xmlns='http://docs.openrepose.org/repose/http-connection-pool/v1.0'>
          |    <pool id='default' default='false'/>
          |    <pool id='default2' default='false'/>
          |</http-connection-pools>
        """.stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(xml)
      }.getLocalizedMessage should include("One and only one default pool must be defined")
    }

    it("should not validate if max per-route is greater than max total") {
      val xml =
        """<http-connection-pools xmlns='http://docs.openrepose.org/repose/http-connection-pool/v1.0'>
          |    <pool id='default' default='true' http.conn-manager.max-per-route='200' http.conn-manager.max-total='199' />
          |</http-connection-pools>
        """.stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(xml)
      }.getLocalizedMessage should include("Max connections per route must be less than or equal to total max connections")
    }

    it("should validate if max per-route is equal to max total") {
      val xml =
        """<http-connection-pools xmlns='http://docs.openrepose.org/repose/http-connection-pool/v1.0'>
          |    <pool id='default' default='true' http.conn-manager.max-per-route='200' http.conn-manager.max-total='200' />
          |</http-connection-pools>
        """.stripMargin
      validator.validateConfigString(xml)
    }

    it("should not validate if chunked-encoding is configured wrong") {
      val xml =
        """<http-connection-pools xmlns='http://docs.openrepose.org/repose/http-connection-pool/v1.0'>
          |    <pool id='default' default='true' chunked-encoding='blah' http.conn-manager.max-per-route='200' http.conn-manager.max-total='199' />
          |</http-connection-pools>
        """.stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(xml)
      }.getLocalizedMessage should include("'blah' is not facet-valid with respect to enumeration")
    }

    it("should not validate if missing keystore filename") {
      val xml =
        """<http-connection-pools xmlns='http://docs.openrepose.org/repose/http-connection-pool/v1.0'>
          |    <pool id='clientAuthentication'
          |          default='false'
          |          keystore-password='password'
          |          key-password='secret' />
          |</http-connection-pools>
        """.stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(xml)
      }.getLocalizedMessage should include("IF a keystore filename, password, or key password is provided, THEN all must be provided")
    }

    it("should not validate if missing keystore password") {
      val xml =
        """<http-connection-pools xmlns='http://docs.openrepose.org/repose/http-connection-pool/v1.0'>
          |    <pool id='clientAuthentication'
          |          default='false'
          |          keystore-filename='keystore.jks'
          |          key-password='secret' />
          |</http-connection-pools>
        """.stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(xml)
      }.getLocalizedMessage should include("IF a keystore filename, password, or key password is provided, THEN all must be provided")
    }

    it("should not validate if missing key password") {
      val xml =
        """<http-connection-pools xmlns='http://docs.openrepose.org/repose/http-connection-pool/v1.0'>
          |    <pool id='clientAuthentication'
          |          default='false'
          |          keystore-filename='keystore.jks'
          |          keystore-password='password' />
          |</http-connection-pools>
        """.stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(xml)
      }.getLocalizedMessage should include("IF a keystore filename, password, or key password is provided, THEN all must be provided")
    }
  }
}
