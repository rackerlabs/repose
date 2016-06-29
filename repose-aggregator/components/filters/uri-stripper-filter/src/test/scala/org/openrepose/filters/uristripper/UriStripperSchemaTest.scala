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
package org.openrepose.filters.uristripper

import org.junit.runner.RunWith
import org.openrepose.commons.test.ConfigValidator
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}
import org.xml.sax.SAXParseException

@RunWith(classOf[JUnitRunner])
class UriStripperSchemaTest extends FunSpec with Matchers {
  val validator = ConfigValidator("/META-INF/schema/config/uri-stripper.xsd")

  describe("schema validation") {
    it("should successfully validate the sample config") {
      validator.validateConfigFile("/META-INF/schema/examples/uri-stripper.cfg.xml")
    }

    val methods = Set("GET", "DELETE", "POST", "PUT", "PATCH", "HEAD", "OPTIONS", "CONNECT", "TRACE", "ALL")
    methods.subsets.filter(_.nonEmpty).map(_.mkString(" ")) foreach { httpMethods =>
      it(s"should successfully validate if the HTTP Methods list is not empty ($httpMethods)") {
        val config =
          s"""<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="true" token-index="1">
              |    <link-resource uri-path-regex=".*" http-methods="$httpMethods">
              |        <response>
              |            <json>$$.service.link</json>
              |        </response>
              |    </link-resource>
              |</uri-stripper>""".stripMargin

        validator.validateConfigString(config)
      }
    }

    it(s"should fail to validate if the HTTP Methods list is empty") {
      val config =
        s"""<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="true" token-index="1">
            |    <link-resource uri-path-regex=".*" http-methods="">
            |        <response>
            |            <json>$$.service.link</json>
            |        </response>
            |    </link-resource>
            |</uri-stripper>""".stripMargin

      val exception = intercept[SAXParseException] {
        validator.validateConfigString(config)
      }
      exception.getLocalizedMessage should include("If the http-methods attribute is present, then it must not be empty.")
    }

    it(s"should successfully validate if the both request and response are present") {
      val config =
        s"""<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="true" token-index="1">
            |    <link-resource>
            |        <request>
            |            <json>$$.service.link</json>
            |        </request>
            |        <response>
            |            <json>$$.service.link</json>
            |        </response>
            |    </link-resource>
            |</uri-stripper>""".stripMargin

      validator.validateConfigString(config)
    }

    it(s"should fail to validate if the neither request nor response is present") {
      val config =
        s"""<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="true" token-index="1">
            |    <link-resource>
            |    </link-resource>
            |</uri-stripper>""".stripMargin

      val exception = intercept[SAXParseException] {
        validator.validateConfigString(config)
      }
      exception.getLocalizedMessage should include("Either a request or response element must be defined.")
    }
  }
}
