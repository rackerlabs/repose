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
package org.openrepose.filters.bodypatcher.config

import java.net.URL

import org.junit.runner.RunWith
import org.openrepose.commons.test.ConfigurationTest
import org.scalatestplus.junit.JUnitRunner
import org.xml.sax.SAXParseException

/**
  * Created by adrian on 4/29/16.
  */
@RunWith(classOf[JUnitRunner])
class BodyPatcherSchemaTest extends ConfigurationTest {
  override val schema: URL = getClass.getResource("/META-INF/schema/config/body-patcher.xsd")
  override val exampleConfig: URL = getClass.getResource("/META-INF/schema/examples/body-patcher.cfg.xml")
  override val jaxbContextPath: String = classOf[ObjectFactory].getPackage.getName

  describe("change") {
    it("should require at least request or response") {
      val config =
        """<body-patcher xmlns="http://docs.openrepose.org/repose/bodypatcher/v1.0">
          |    <change/>
          |</body-patcher>
        """.stripMargin
      val exception = intercept[SAXParseException] {
        validator.validateConfigString(config)
      }
      exception.getLocalizedMessage should include ("You must specify request and/or response behavior")
    }

    it("should allow both request and response") {
      val config =
        """<body-patcher xmlns="http://docs.openrepose.org/repose/bodypatcher/v1.0">
          |    <change>
          |        <request>
          |            <json>
          |                [
          |                    {
          |                        "op" : "replace",
          |                        "path" : "/a",
          |                        "value" : 6
          |                    }
          |                ]
          |            </json>
          |        </request>
          |        <response>
          |            <json>
          |                [
          |                    {
          |                        "op" : "replace",
          |                        "path" : "/a",
          |                        "value" : 6
          |                    }
          |                ]
          |            </json>
          |        </response>
          |    </change>
          |</body-patcher>
        """.stripMargin
      validator.validateConfigString(config)
    }

    it("should allow just request") {
      val config =
        """<body-patcher xmlns="http://docs.openrepose.org/repose/bodypatcher/v1.0">
          |    <change>
          |        <request>
          |            <json>
          |                [
          |                    {
          |                        "op" : "replace",
          |                        "path" : "/a",
          |                        "value" : 6
          |                    }
          |                ]
          |            </json>
          |        </request>
          |    </change>
          |</body-patcher>
        """.stripMargin
      validator.validateConfigString(config)
    }

    it("should allow just response") {
      val config =
        """<body-patcher xmlns="http://docs.openrepose.org/repose/bodypatcher/v1.0">
          |    <change>
          |        <response>
          |            <json>
          |                [
          |                    {
          |                        "op" : "replace",
          |                        "path" : "/a",
          |                        "value" : 6
          |                    }
          |                ]
          |            </json>
          |        </response>
          |    </change>
          |</body-patcher>
        """.stripMargin
      validator.validateConfigString(config)
    }
  }

  //todo: when we add xml support revert the commit this was added in
  describe("request") {
    ignore("should require at least json or xml") {
      val config =
        """<body-patcher xmlns="http://docs.openrepose.org/repose/bodypatcher/v1.0">
          |    <change>
          |        <request/>
          |    </change>
          |</body-patcher>
        """.stripMargin
      val exception = intercept[SAXParseException] {
        validator.validateConfigString(config)
      }
      exception.getLocalizedMessage should include ("You must specify a json and/or xml patch")
    }

    ignore("should allow both json and xml") {
      val config =
        """<body-patcher xmlns="http://docs.openrepose.org/repose/bodypatcher/v1.0">
          |    <change>
          |        <request>
          |            <json>
          |                [
          |                    {
          |                        "op" : "replace",
          |                        "path" : "/a",
          |                        "value" : 6
          |                    }
          |                ]
          |            </json>
          |            <xml>
          |                <![CDATA[
          |                <diff>
          |                    <replace sel="example/patchTool/@url">https://tools.ietf.org/html/rfc5261</replace>
          |                </diff>
          |                ]]>
          |            </xml>
          |        </request>
          |    </change>
          |</body-patcher>
        """.stripMargin
      validator.validateConfigString(config)
    }

    ignore("should allow just json") {
      val config =
        """<body-patcher xmlns="http://docs.openrepose.org/repose/bodypatcher/v1.0">
          |    <change>
          |        <request>
          |            <json>
          |                [
          |                    {
          |                        "op" : "replace",
          |                        "path" : "/a",
          |                        "value" : 6
          |                    }
          |                ]
          |            </json>
          |        </request>
          |    </change>
          |</body-patcher>
        """.stripMargin
      validator.validateConfigString(config)
    }

    ignore("should allow just xml") {
      val config =
        """<body-patcher xmlns="http://docs.openrepose.org/repose/bodypatcher/v1.0">
          |    <change>
          |        <request>
          |            <xml>
          |                <![CDATA[
          |                <diff>
          |                    <replace sel="example/patchTool/@url">https://tools.ietf.org/html/rfc5261</replace>
          |                </diff>
          |                ]]>
          |            </xml>
          |        </request>
          |    </change>
          |</body-patcher>
        """.stripMargin
      validator.validateConfigString(config)
    }
  }

  describe("response") {
    ignore("should require at least json or xml") {
      val config =
        """<body-patcher xmlns="http://docs.openrepose.org/repose/bodypatcher/v1.0">
          |    <change>
          |        <response/>
          |    </change>
          |</body-patcher>
        """.stripMargin
      val exception = intercept[SAXParseException] {
        validator.validateConfigString(config)
      }
      exception.getLocalizedMessage should include ("You must specify a json and/or xml patch")
    }

    ignore("should allow both json and xml") {
      val config =
        """<body-patcher xmlns="http://docs.openrepose.org/repose/bodypatcher/v1.0">
          |    <change>
          |        <response>
          |            <json>
          |                [
          |                    {
          |                        "op" : "replace",
          |                        "path" : "/a",
          |                        "value" : 6
          |                    }
          |                ]
          |            </json>
          |            <xml>
          |                <![CDATA[
          |                <diff>
          |                    <replace sel="example/patchTool/@url">https://tools.ietf.org/html/rfc5261</replace>
          |                </diff>
          |                ]]>
          |            </xml>
          |        </response>
          |    </change>
          |</body-patcher>
        """.stripMargin
      validator.validateConfigString(config)
    }

    ignore("should allow just json") {
      val config =
        """<body-patcher xmlns="http://docs.openrepose.org/repose/bodypatcher/v1.0">
          |    <change>
          |        <response>
          |            <json>
          |                [
          |                    {
          |                        "op" : "replace",
          |                        "path" : "/a",
          |                        "value" : 6
          |                    }
          |                ]
          |            </json>
          |        </response>
          |    </change>
          |</body-patcher>
        """.stripMargin
      validator.validateConfigString(config)
    }

    ignore("should allow just xml") {
      val config =
        """<body-patcher xmlns="http://docs.openrepose.org/repose/bodypatcher/v1.0">
          |    <change>
          |        <response>
          |            <xml>
          |                <![CDATA[
          |                <diff>
          |                    <replace sel="example/patchTool/@url">https://tools.ietf.org/html/rfc5261</replace>
          |                </diff>
          |                ]]>
          |            </xml>
          |        </response>
          |    </change>
          |</body-patcher>
        """.stripMargin
      validator.validateConfigString(config)
    }
  }
}
