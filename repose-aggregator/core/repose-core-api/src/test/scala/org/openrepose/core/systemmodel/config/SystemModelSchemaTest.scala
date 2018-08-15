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

package org.openrepose.core.systemmodel.config

import java.net.URL

import org.junit.runner.RunWith
import org.openrepose.commons.test.ConfigurationTest
import org.scalatest.junit.JUnitRunner
import org.xml.sax.SAXParseException

@RunWith(classOf[JUnitRunner])
class SystemModelSchemaTest extends ConfigurationTest {
  override val schema: URL = getClass.getResource("/META-INF/schema/system-model/system-model.xsd")
  override val exampleConfig: URL = getClass.getResource("/META-INF/schema/examples/system-model.cfg.xml")
  override val jaxbContextPath: String = classOf[ObjectFactory].getPackage.getName

  describe("schema validation") {
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

    it("should successfully validate the config if each node either has an http-port or https-port defined") {
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
      }.getLocalizedMessage should include ("There should be one and only one default destination")
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

    it("should successfully validate the config if a filter has a uri conditional") {
      val config = """<system-model xmlns="http://docs.openrepose.org/repose/system-model/v2.0">
                     |    <repose-cluster id="repose">
                     |        <nodes>
                     |            <node id="node1" hostname="10.0.0.1" http-port="8088"/>
                     |        </nodes>
                     |        <filters>
                     |            <filter name="ip-user">
                     |                <uri regex="foo"/>
                     |            </filter>
                     |        </filters>
                     |        <destinations>
                     |            <endpoint id="openrepose" protocol="http" hostname="www.openrepose.org" root-path="/" port="80"
                     |                      default="true"/>
                     |        </destinations>
                     |    </repose-cluster>
                     |</system-model>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should successfully validate the config if a filter has a header conditional without a value") {
      val config = """<system-model xmlns="http://docs.openrepose.org/repose/system-model/v2.0">
                     |    <repose-cluster id="repose">
                     |        <nodes>
                     |            <node id="node1" hostname="10.0.0.1" http-port="8088"/>
                     |        </nodes>
                     |        <filters>
                     |            <filter name="ip-user">
                     |                <header name="bar"/>
                     |            </filter>
                     |        </filters>
                     |        <destinations>
                     |            <endpoint id="openrepose" protocol="http" hostname="www.openrepose.org" root-path="/" port="80"
                     |                      default="true"/>
                     |        </destinations>
                     |    </repose-cluster>
                     |</system-model>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should successfully validate the config if a filter has a header conditional with a value") {
      val config = """<system-model xmlns="http://docs.openrepose.org/repose/system-model/v2.0">
                     |    <repose-cluster id="repose">
                     |        <nodes>
                     |            <node id="node1" hostname="10.0.0.1" http-port="8088"/>
                     |        </nodes>
                     |        <filters>
                     |            <filter name="ip-user">
                     |                <header name="bar" value="baz"/>
                     |            </filter>
                     |        </filters>
                     |        <destinations>
                     |            <endpoint id="openrepose" protocol="http" hostname="www.openrepose.org" root-path="/" port="80"
                     |                      default="true"/>
                     |        </destinations>
                     |    </repose-cluster>
                     |</system-model>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should successfully validate the config if a filter has a methods conditional") {
      val config = """<system-model xmlns="http://docs.openrepose.org/repose/system-model/v2.0">
                     |    <repose-cluster id="repose">
                     |        <nodes>
                     |            <node id="node1" hostname="10.0.0.1" http-port="8088"/>
                     |        </nodes>
                     |        <filters>
                     |            <filter name="ip-user">
                     |                <methods value="POST"/>
                     |            </filter>
                     |        </filters>
                     |        <destinations>
                     |            <endpoint id="openrepose" protocol="http" hostname="www.openrepose.org" root-path="/" port="80"
                     |                      default="true"/>
                     |        </destinations>
                     |    </repose-cluster>
                     |</system-model>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should successfully validate the config if a filter has a not conditional") {
      val config = """<system-model xmlns="http://docs.openrepose.org/repose/system-model/v2.0">
                     |    <repose-cluster id="repose">
                     |        <nodes>
                     |            <node id="node1" hostname="10.0.0.1" http-port="8088"/>
                     |        </nodes>
                     |        <filters>
                     |            <filter name="ip-user">
                     |                <not><methods value="POST PUT"/></not>
                     |            </filter>
                     |        </filters>
                     |        <destinations>
                     |            <endpoint id="openrepose" protocol="http" hostname="www.openrepose.org" root-path="/" port="80"
                     |                      default="true"/>
                     |        </destinations>
                     |    </repose-cluster>
                     |</system-model>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should successfully validate the config if a filter has and'd conditionals") {
      val config = """<system-model xmlns="http://docs.openrepose.org/repose/system-model/v2.0">
                     |    <repose-cluster id="repose">
                     |        <nodes>
                     |            <node id="node1" hostname="10.0.0.1" http-port="8088"/>
                     |        </nodes>
                     |        <filters>
                     |            <filter name="ip-user">
                     |                <and>
                     |                    <uri regex="foo"/>
                     |                    <header name="bar" value="baz"/>
                     |                    <not><methods value="POST PUT GET"/></not>
                     |                </and>
                     |            </filter>
                     |        </filters>
                     |        <destinations>
                     |            <endpoint id="openrepose" protocol="http" hostname="www.openrepose.org" root-path="/" port="80"
                     |                      default="true"/>
                     |        </destinations>
                     |    </repose-cluster>
                     |</system-model>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should successfully validate the config if a filter has or'd conditionals") {
      val config = """<system-model xmlns="http://docs.openrepose.org/repose/system-model/v2.0">
                     |    <repose-cluster id="repose">
                     |        <nodes>
                     |            <node id="node1" hostname="10.0.0.1" http-port="8088"/>
                     |        </nodes>
                     |        <filters>
                     |            <filter name="ip-user">
                     |                <or>
                     |                    <uri regex="foo"/>
                     |                    <header name="bar" value="baz"/>
                     |                </or>
                     |            </filter>
                     |        </filters>
                     |        <destinations>
                     |            <endpoint id="openrepose" protocol="http" hostname="www.openrepose.org" root-path="/" port="80"
                     |                      default="true"/>
                     |        </destinations>
                     |    </repose-cluster>
                     |</system-model>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should successfully validate the config if a filter has nested or'd conditionals") {
      val config = """<system-model xmlns="http://docs.openrepose.org/repose/system-model/v2.0">
                     |    <repose-cluster id="repose">
                     |        <nodes>
                     |            <node id="node1" hostname="10.0.0.1" http-port="8088"/>
                     |        </nodes>
                     |        <filters>
                     |            <filter name="ip-user">
                     |                <and>
                     |                    <or>
                     |                        <uri regex="foo"/>
                     |                        <header name="bar" value="baz"/>
                     |                    </or>
                     |                    <not><methods value="POST PUT GET HEAD"/></not>
                     |                </and>
                     |            </filter>
                     |        </filters>
                     |        <destinations>
                     |            <endpoint id="openrepose" protocol="http" hostname="www.openrepose.org" root-path="/" port="80"
                     |                      default="true"/>
                     |        </destinations>
                     |    </repose-cluster>
                     |</system-model>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should successfully validate the config if a filter has nested and'd conditionals") {
      val config = """<system-model xmlns="http://docs.openrepose.org/repose/system-model/v2.0">
                     |    <repose-cluster id="repose">
                     |        <nodes>
                     |            <node id="node1" hostname="10.0.0.1" http-port="8088"/>
                     |        </nodes>
                     |        <filters>
                     |            <filter name="ip-user">
                     |                <or>
                     |                    <and>
                     |                        <uri regex="foo"/>
                     |                        <not><methods value="POST PUT GET HEAD PATCH"/></not>
                     |                    </and>
                     |                    <header name="bar" value="baz"/>
                     |                </or>
                     |            </filter>
                     |        </filters>
                     |        <destinations>
                     |            <endpoint id="openrepose" protocol="http" hostname="www.openrepose.org" root-path="/" port="80"
                     |                      default="true"/>
                     |        </destinations>
                     |    </repose-cluster>
                     |</system-model>""".stripMargin
      validator.validateConfigString(config)
    }

    it("should reject the config if more than one top level conditional is present") {
      val config = """<system-model xmlns="http://docs.openrepose.org/repose/system-model/v2.0">
                     |    <repose-cluster id="repose">
                     |        <nodes>
                     |            <node id="node1" hostname="10.0.0.1" http-port="8088"/>
                     |        </nodes>
                     |        <filters>
                     |            <filter name="ip-user">
                     |                <uri regex="foo"/>
                     |                <methods value="POST PUT GET HEAD PATCH TRACE"/>
                     |            </filter>
                     |        </filters>
                     |        <destinations>
                     |            <endpoint id="openrepose" protocol="http" hostname="www.openrepose.org" root-path="/" port="80"
                     |                      default="true"/>
                     |        </destinations>
                     |    </repose-cluster>
                     |</system-model>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include ("Invalid content was found")
    }

    Seq(("methods", "value"), ("header", "name"), ("uri", "regex")).foreach { case (conditional, attribute) =>
      it(s"should reject the config if the $conditional conditional is not empty") {
        val config =
          s"""<system-model xmlns="http://docs.openrepose.org/repose/system-model/v2.0">
             |    <repose-cluster id="repose">
             |        <nodes>
             |            <node id="node1" hostname="10.0.0.1" http-port="8088"/>
             |        </nodes>
             |        <filters>
             |            <filter name="ip-user">
             |                <$conditional $attribute="foo">Not Empty</$conditional>
             |            </filter>
             |        </filters>
             |        <destinations>
             |            <endpoint id="openrepose" protocol="http" hostname="www.openrepose.org" root-path="/" port="80"
             |                      default="true"/>
             |        </destinations>
             |    </repose-cluster>
             |</system-model>""".stripMargin
        intercept[SAXParseException] {
          validator.validateConfigString(config)
        }.getLocalizedMessage should include("must have no character or element information item")
      }

      it(s"should reject the config if both the uri-regex and $conditional conditional are used") {
        val config =
          s"""<system-model xmlns="http://docs.openrepose.org/repose/system-model/v2.0">
             |    <repose-cluster id="repose">
             |        <nodes>
             |            <node id="node1" hostname="10.0.0.1" http-port="8088"/>
             |        </nodes>
             |        <filters>
             |            <filter name="ip-user" uri-regex="foo">
             |                <$conditional $attribute="foo"/>
             |            </filter>
             |        </filters>
             |        <destinations>
             |            <endpoint id="openrepose" protocol="http" hostname="www.openrepose.org" root-path="/" port="80"
             |                      default="true"/>
             |        </destinations>
             |    </repose-cluster>
             |</system-model>""".stripMargin
        intercept[SAXParseException] {
          validator.validateConfigString(config)
        }.getLocalizedMessage should include("Cannot define both a deprecated uri-regex attribute and any of the new conditional elements")
      }
    }

    it("should reject the config if both the uri-regex and not conditional are used") {
      val config =
        s"""<system-model xmlns="http://docs.openrepose.org/repose/system-model/v2.0">
           |    <repose-cluster id="repose">
           |        <nodes>
           |            <node id="node1" hostname="10.0.0.1" http-port="8088"/>
           |        </nodes>
           |        <filters>
           |            <filter name="ip-user" uri-regex="foo">
           |                <not><header name="foo"/></not>
           |            </filter>
           |        </filters>
           |        <destinations>
           |            <endpoint id="openrepose" protocol="http" hostname="www.openrepose.org" root-path="/" port="80"
           |                      default="true"/>
           |        </destinations>
           |    </repose-cluster>
           |</system-model>""".stripMargin
      intercept[SAXParseException] {
        validator.validateConfigString(config)
      }.getLocalizedMessage should include("Cannot define both a deprecated uri-regex attribute and any of the new conditional elements")
    }

    Seq("and", "or").foreach { conditional =>
      it(s"should reject the config if both the uri-regex and $conditional conditional are used") {
        val config =
          s"""<system-model xmlns="http://docs.openrepose.org/repose/system-model/v2.0">
             |    <repose-cluster id="repose">
             |        <nodes>
             |            <node id="node1" hostname="10.0.0.1" http-port="8088"/>
             |        </nodes>
             |        <filters>
             |            <filter name="ip-user" uri-regex="foo">
             |                <$conditional>
             |                    <uri regex="foo"/>
             |                    <header name="bar"/>
             |                </$conditional>
             |            </filter>
             |        </filters>
             |        <destinations>
             |            <endpoint id="openrepose" protocol="http" hostname="www.openrepose.org" root-path="/" port="80"
             |                      default="true"/>
             |        </destinations>
             |    </repose-cluster>
             |</system-model>""".stripMargin
        intercept[SAXParseException] {
          validator.validateConfigString(config)
        }.getLocalizedMessage should include ("Cannot define both a deprecated uri-regex attribute and any of the new conditional elements")
      }
    }
  }
}
