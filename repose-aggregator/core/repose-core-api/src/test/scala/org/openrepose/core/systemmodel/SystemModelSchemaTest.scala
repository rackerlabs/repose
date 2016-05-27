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

package org.openrepose.core.systemmodel

import org.junit.runner.RunWith
import org.openrepose.commons.test.ConfigValidator
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.xml.sax.SAXParseException

@RunWith(classOf[JUnitRunner])
class SystemModelSchemaTest extends FunSpec with BeforeAndAfterEach with Matchers {
  var validator: ConfigValidator = _

  override def beforeEach() = {
    // for some reason this class requires it to be reinitialized for each test, otherwise it complains about ID reuse
    validator = ConfigValidator("/META-INF/schema/system-model/system-model.xsd")
  }

  describe("schema validation") {
    it("should successfully validate the sample config") {
      validator.validateConfigFile("/META-INF/schema/examples/system-model.cfg.xml")
    }

    it("should successfully validate the config when each node has a unique ID") {
      val config = """<system-model xmlns="http://docs.openrepose.org/repose/system-model/v2.0">
                     |    <repose-cluster id="repose">
                     |        <nodes>
                     |            <node id="node1" hostname="10.0.0.1" http-port="8088"/>
                     |            <node id="node2" hostname="10.0.0.2" http-port="8088"/>
                     |        </nodes>
                     |        <destinations>
                     |            <endpoint id="openrepose" protocol="http" hostname="www.openrepose.org" root-path="/" port="80"
                     |                      default="true"/>
                     |        </destinations>
                     |    </repose-cluster>
                     |</system-model>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should reject the config when two nodes have the same ID") {
      val config = """<system-model xmlns="http://docs.openrepose.org/repose/system-model/v2.0">
                     |    <repose-cluster id="repose">
                     |        <nodes>
                     |            <node id="node1" hostname="10.0.0.1" http-port="8088"/>
                     |            <node id="node1" hostname="10.0.0.2" http-port="8088"/>
                     |        </nodes>
                     |        <destinations>
                     |            <endpoint id="openrepose" protocol="http" hostname="www.openrepose.org" root-path="/" port="80"
                     |                      default="true"/>
                     |        </destinations>
                     |    </repose-cluster>
                     |</system-model>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("Each node should have a unique id")
    }

    it("should successfully validate the config if each node either has an http-port or https-poort defined") {
      val config = """<system-model xmlns="http://docs.openrepose.org/repose/system-model/v2.0">
                     |    <repose-cluster id="repose">
                     |        <nodes>
                     |            <node id="node1" hostname="10.0.0.1" http-port="8088"/>
                     |            <node id="node2" hostname="10.0.0.2" https-port="8088"/>
                     |        </nodes>
                     |        <destinations>
                     |            <endpoint id="openrepose" protocol="http" hostname="www.openrepose.org" root-path="/" port="80"
                     |                      default="true"/>
                     |        </destinations>
                     |    </repose-cluster>
                     |</system-model>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should reject the config if a node has neither an http-port nor an https-port defined") {
      val config = """<system-model xmlns="http://docs.openrepose.org/repose/system-model/v2.0">
                     |    <repose-cluster id="repose">
                     |        <nodes>
                     |            <node id="node1" hostname="10.0.0.1"/>
                     |        </nodes>
                     |        <destinations>
                     |            <endpoint id="openrepose" protocol="http" hostname="www.openrepose.org" root-path="/" port="80"
                     |                      default="true"/>
                     |        </destinations>
                     |    </repose-cluster>
                     |</system-model>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("You must specify an http-port and/or an https-port")
    }

    it("should successfully validate the config if all of the filters have unique IDs") {
      val config = """<system-model xmlns="http://docs.openrepose.org/repose/system-model/v2.0">
                     |    <repose-cluster id="repose">
                     |        <nodes>
                     |            <node id="node1" hostname="10.0.0.1" http-port="8088"/>
                     |        </nodes>
                     |        <filters>
                     |            <filter name="ip-user"/>
                     |            <filter name="keystone-v2" id="foo"/>
                     |            <filter name="uri-user" id="bar"/>
                     |        </filters>
                     |        <destinations>
                     |            <endpoint id="openrepose" protocol="http" hostname="www.openrepose.org" root-path="/" port="80"
                     |                      default="true"/>
                     |        </destinations>
                     |    </repose-cluster>
                     |</system-model>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should reject the config if two filters have the same ID") {
      val config = """<system-model xmlns="http://docs.openrepose.org/repose/system-model/v2.0">
                     |    <repose-cluster id="repose">
                     |        <nodes>
                     |            <node id="node1" hostname="10.0.0.1" http-port="8088"/>
                     |        </nodes>
                     |        <filters>
                     |            <filter name="keystone-v2" id="foo"/>
                     |            <filter name="uri-user" id="foo"/>
                     |        </filters>
                     |        <destinations>
                     |            <endpoint id="openrepose" protocol="http" hostname="www.openrepose.org" root-path="/" port="80"
                     |                      default="true"/>
                     |        </destinations>
                     |    </repose-cluster>
                     |</system-model>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("Filters must have ids unique within their containing filter list")
    }

    it("should successfully validate the config if all of the services have unique names") {
      val config = """<system-model xmlns="http://docs.openrepose.org/repose/system-model/v2.0">
                     |    <repose-cluster id="repose">
                     |        <nodes>
                     |            <node id="node1" hostname="10.0.0.1" http-port="8088"/>
                     |        </nodes>
                     |        <services>
                     |            <service name="atom-feed-service"/>
                     |            <service name="dist-datastore"/>
                     |        </services>
                     |        <destinations>
                     |            <endpoint id="openrepose" protocol="http" hostname="www.openrepose.org" root-path="/" port="80"
                     |                      default="true"/>
                     |        </destinations>
                     |    </repose-cluster>
                     |</system-model>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should reject the config if two services have the same name") {
      val config = """<system-model xmlns="http://docs.openrepose.org/repose/system-model/v2.0">
                     |    <repose-cluster id="repose">
                     |        <nodes>
                     |            <node id="node1" hostname="10.0.0.1" http-port="8088"/>
                     |        </nodes>
                     |        <services>
                     |            <service name="dist-datastore"/>
                     |            <service name="dist-datastore"/>
                     |        </services>
                     |        <destinations>
                     |            <endpoint id="openrepose" protocol="http" hostname="www.openrepose.org" root-path="/" port="80"
                     |                      default="true"/>
                     |        </destinations>
                     |    </repose-cluster>
                     |</system-model>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("Services must have name unique within their containing service list")
    }

    it("should successfully validate the config when a destination endpoint is defined") {
      val config = """<system-model xmlns="http://docs.openrepose.org/repose/system-model/v2.0">
                     |    <repose-cluster id="repose">
                     |        <nodes>
                     |            <node id="node1" hostname="10.0.0.1" http-port="8088"/>
                     |            <node id="node2" hostname="10.0.0.2" http-port="8088"/>
                     |        </nodes>
                     |        <destinations>
                     |            <endpoint id="openrepose" protocol="http" hostname="www.openrepose.org" root-path="/" port="80"
                     |                      default="true"/>
                     |        </destinations>
                     |    </repose-cluster>
                     |</system-model>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should successfully validate the config when a destination target is defined") {
      val config = """<system-model xmlns="http://docs.openrepose.org/repose/system-model/v2.0">
                     |    <repose-cluster id="repose">
                     |        <nodes>
                     |            <node id="node1" hostname="10.0.0.1" http-port="8088"/>
                     |            <node id="node2" hostname="10.0.0.2" http-port="8088"/>
                     |        </nodes>
                     |        <destinations>
                     |            <target id="foo" cluster="bar" default="true"/>
                     |        </destinations>
                     |    </repose-cluster>
                     |    <service-cluster id="bar">
                     |        <nodes>
                     |            <node id="mock-node" hostname="localhost" http-port="8080"/>
                     |        </nodes>
                     |    </service-cluster>
                     |</system-model>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should reject the config if there are no defined destination endpoints nor targets") {
      val config = """<system-model xmlns="http://docs.openrepose.org/repose/system-model/v2.0">
                     |    <repose-cluster id="repose">
                     |        <nodes>
                     |            <node id="node1" hostname="10.0.0.1" http-port="8088"/>
                     |        </nodes>
                     |        <destinations>
                     |        </destinations>
                     |    </repose-cluster>
                     |</system-model>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("Must have at least one destination defined.")
    }

    it("should reject the config if two destinations are marked as the default") {
      val config = """<system-model xmlns="http://docs.openrepose.org/repose/system-model/v2.0">
                     |    <repose-cluster id="repose">
                     |        <nodes>
                     |            <node id="node1" hostname="10.0.0.1" http-port="8088"/>
                     |        </nodes>
                     |        <destinations>
                     |            <endpoint id="openrepose" protocol="http" hostname="www.openrepose.org" root-path="/" port="80"
                     |                      default="true"/>
                     |            <target id="foo" cluster="bar" default="true"/>
                     |        </destinations>
                     |    </repose-cluster>
                     |</system-model>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("There should only be one default destination")
    }

    it("should reject the config if two destinations have the same ID") {
      val config = """<system-model xmlns="http://docs.openrepose.org/repose/system-model/v2.0">
                     |    <repose-cluster id="repose">
                     |        <nodes>
                     |            <node id="node1" hostname="10.0.0.1" http-port="8088"/>
                     |        </nodes>
                     |        <destinations>
                     |            <endpoint id="foo" protocol="http" hostname="www.openrepose.org" root-path="/" port="80"
                     |                      default="true"/>
                     |            <target id="foo" cluster="bar"/>
                     |        </destinations>
                     |    </repose-cluster>
                     |</system-model>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("Destinations must have ids unique within their containing list")
    }
  }
}
