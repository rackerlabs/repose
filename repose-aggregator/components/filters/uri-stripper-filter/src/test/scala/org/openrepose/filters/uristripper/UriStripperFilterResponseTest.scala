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

import java.io.ByteArrayInputStream
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletResponse

import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.openrepose.commons.utils.http.media.MimeType
import org.openrepose.commons.utils.servlet.http.{HttpServletRequestWrapper, HttpServletResponseWrapper}
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse}
import play.api.libs.json.{JsDefined, JsUndefined, Json}

import scala.xml.XML

@RunWith(classOf[JUnitRunner])
class UriStripperFilterResponseTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {
  var filter: UriStripperFilter = _
  var request: MockHttpServletRequest = _
  var response: MockHttpServletResponse = _
  var filterChain: FilterChain = _

  override def beforeEach() = {
    request = new MockHttpServletRequest
    response = new MockHttpServletResponse
    filterChain = mock[FilterChain]
    filter = new UriStripperFilter(null)
  }

  describe("response links handling") {
    describe("json") {
      it("should not alter the body if the uri does not match the configured regex") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex="/v1/[^/]+/bar">
            |        <response>
            |            <json>$.link</json>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """{
            |  "link": "http://example.com/v1/bar"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        response.getContentAsString shouldEqual respBody
      }

      it("should not alter the body if the method does not match the configured method") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*" http-methods="POST">
            |        <response>
            |            <json>$.link</json>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """{
            |  "link": "http://example.com/v1/foo"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setMethod("GET")
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        response.getContentAsString shouldEqual respBody
      }

      it("should not alter the body if the method does not match the configured resource type (request in lieu of response") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <request>
            |            <json>$.link</json>
            |        </request>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """{
            |  "link": "http://example.com/v1/foo"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        response.getContentAsString shouldEqual respBody
      }

      it("should not alter the body if the content-type is not supported (Text)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <json>$.link</json>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """The link is http://example.com/v1/foo
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.TEXT_PLAIN.toString)

        filter.doFilter(request, response, filterChain)

        response.getContentAsString shouldEqual respBody
      }

      it("should not alter the body if the content-type is not supported (XML)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <json>$.link</json>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<link>http://example.com/v2/12345/foo</link>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        response.getContentAsString shouldEqual respBody
      }

      it("should update a link by replacing the stripped token after the previous token from the request URI") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <json>$.link</json>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """{
            |  "link": "http://example.com/v1/bar"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        (Json.parse(response.getContentAsString) \ "link").as[String] shouldEqual "http://example.com/v1/12345/bar"
      }

      it("should update a link by replacing the stripped token before the next token from the request URI") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <json>$.link</json>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """{
            |  "link": "http://example.com/v2/foo"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        (Json.parse(response.getContentAsString) \ "link").as[String] shouldEqual "http://example.com/v2/12345/foo"
      }

      it("should update a link by replacing the stripped token at the configured index") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <json token-index="2">$.link</json>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """{
            |  "link": "http://example.com/v1/bar"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        (Json.parse(response.getContentAsString) \ "link").as[String] shouldEqual "http://example.com/v1/bar/12345"
      }

      it("should not alter the body if the link cannot be located (continue)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <json link-mismatch-action="continue">$.dne</json>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """{
            |  "link": "http://example.com/v1/foo"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        response.getContentAsString shouldEqual Json.parse(respBody).toString()
      }

      it("should not alter the body if the link's token index is out of bounds (continue)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <json link-mismatch-action="continue" token-index="5">$.link</json>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """{
            |  "link": "http://example.com/v1/foo"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        response.getContentAsString shouldEqual Json.parse(respBody).toString()
      }

      it("should not alter the body if the stripped path segment cannot be re-inserted in the link (continue)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <json link-mismatch-action="continue">$.link</json>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """{
            |  "link": "http://example.com/v2/bar"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        response.getContentAsString shouldEqual Json.parse(respBody).toString()
      }

      it("should not alter the body if the link cannot be located (remove)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <json link-mismatch-action="remove">$.dne</json>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """{
            |  "link": "http://example.com/v1/foo"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        response.getContentAsString shouldEqual Json.parse(respBody).toString()
      }

      it("should remove the field if the token index is out of bounds (remove)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <json link-mismatch-action="remove" token-index="5">$.link</json>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """{
            |  "link": "http://example.com/v1/foo"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        response.getContentAsString shouldEqual "{}"
      }

      it("should remove the field if the stripped path segment cannot be re-inserted in the link (remove)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <json link-mismatch-action="remove">$.link</json>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """{
            |  "link": "http://example.com/v2/bar"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        response.getContentAsString shouldEqual "{}"
      }

      it("should fail if the link cannot be located (fail)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <json link-mismatch-action="fail">$.dne</json>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """{
            |  "link": "http://example.com/v1/foo"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        response.getStatus shouldEqual HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        response.getContentLength shouldEqual 0
      }

      it("should fail if the link's token index is out of bounds (fail)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <json link-mismatch-action="fail" token-index="5">$.link</json>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """{
            |  "link": "http://example.com/v1/foo"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        response.getStatus shouldEqual HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        response.getContentLength shouldEqual 0
      }

      it("should fail if the stripped path segment cannot be re-inserted in the link (fail)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <json link-mismatch-action="fail">$.link</json>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """{
            |  "link": "http://example.com/v2/bar"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        response.getStatus shouldEqual HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        response.getContentLength shouldEqual 0
      }

      it("should update multiple links given multiple JSON paths") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <json>$.link</json>
            |            <json>$.linktwo</json>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """{
            |  "link": "http://example.com/v1/foo",
            |  "linktwo": "http://example.com/v1/foo"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        val jsonBody = Json.parse(response.getContentAsString)
        (jsonBody \ "link").asInstanceOf[JsDefined].value.toString() shouldEqual """"http://example.com/v1/12345/foo""""
        (jsonBody \ "linktwo").asInstanceOf[JsDefined].value.toString() shouldEqual """"http://example.com/v1/12345/foo""""
      }

      it("should update multiple links given multiple JSON paths with independent failure behaviors (continue, remove)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <json>$.link</json>
            |            <json link-mismatch-action="remove" token-index="5">$.linknumerodos</json>
            |            <json link-mismatch-action="continue">$.dne</json>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """{
            |  "link": "http://example.com/v1/foo",
            |  "linknumerodos": "http://example.com/v1/foo"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        val jsonBody = Json.parse(response.getContentAsString)
        (jsonBody \ "link").asInstanceOf[JsDefined].value.toString() shouldEqual """"http://example.com/v1/12345/foo""""
        (jsonBody \ "linknumerodos").isInstanceOf[JsUndefined] shouldEqual true
      }
    }

    describe("xml") {
      it("should not alter the body if the uri does not match the configured regex") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex="/v1/[^/]+/bar">
            |        <response>
            |            <xml>
            |                <xpath>/link</xpath>
            |            </xml>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<link>http://example.com/v1/bar</link>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        response.getContentAsString shouldEqual respBody
      }

      it("should not alter the body if the method does not match the configured method") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*" http-methods="POST">
            |        <response>
            |            <xml>
            |                <xpath>/link</xpath>
            |            </xml>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<link>http://example.com/v1/foo</link>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setMethod("GET")
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        response.getContentAsString shouldEqual respBody
      }

      it("should not alter the body if the method does not match the configured resource type (request in lieu of response") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <request>
            |            <xml>
            |                <xpath>/link</xpath>
            |            </xml>
            |        </request>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<link>http://example.com/v1/foo</link>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        response.getContentAsString shouldEqual respBody
      }

      it("should not alter the body if the content-type is not supported (Text)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <xml>
            |                <xpath>/link</xpath>
            |            </xml>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """
            |The link is http://example.com/v1/foo
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.TEXT_PLAIN.toString)

        filter.doFilter(request, response, filterChain)

        response.getContentAsString shouldEqual respBody
      }

      it("should not alter the body if the content-type is not supported (JSON)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <xml>
            |                <xpath>/link</xpath>
            |            </xml>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """{
            |  "link": "http://example.com/v1/foo"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        response.getContentAsString shouldEqual respBody
      }

      it("should update a link by replacing the stripped token after the previous token from the request URI") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <xml>
            |                <xpath>/link</xpath>
            |            </xml>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<link>http://example.com/v1/bar</link>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        (XML.loadString(response.getContentAsString) \\ "link").text shouldEqual "http://example.com/v1/12345/bar"
      }

      it("should update a link by replacing the stripped token before the next token from the request URI") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <xml>
            |                <xpath>/link</xpath>
            |            </xml>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<link>http://example.com/v2/foo</link>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        (XML.loadString(response.getContentAsString) \\ "link").text shouldEqual "http://example.com/v2/12345/foo"
      }

      it("should update a link by replacing the stripped token at the configured index") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <xml>
            |                <xpath token-index="2">/link</xpath>
            |            </xml>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<link>http://example.com/v1/bar</link>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        (XML.loadString(response.getContentAsString) \\ "link").text shouldEqual "http://example.com/v1/bar/12345"
      }

      it("should not alter the body if the link cannot be located (continue)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <xml>
            |                <xpath link-mismatch-action="continue">/dne</xpath>
            |            </xml>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<link>http://example.com/v1/foo</link>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        XML.loadString(response.getContentAsString).toString() shouldEqual XML.loadString(respBody).toString()
      }

      it("should not alter the body if the link's token index is out of bounds (continue)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <xml>
            |                <xpath link-mismatch-action="continue" token-index="5">/link</xpath>
            |            </xml>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<link>http://example.com/v1/foo</link>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        XML.loadString(response.getContentAsString).toString() shouldEqual XML.loadString(respBody).toString()
      }

      it("should not alter the body if the stripped path segment cannot be re-inserted in the link (continue)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <xml>
            |                <xpath link-mismatch-action="continue">/link</xpath>
            |            </xml>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<link>http://example.com/v2/bar</link>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        XML.loadString(response.getContentAsString).toString() shouldEqual XML.loadString(respBody).toString()
      }

      it("should not alter the body if the link cannot be located (remove)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <xml>
            |                <xpath link-mismatch-action="remove">/dne</xpath>
            |            </xml>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<link>http://example.com/v1/foo</link>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        (XML.loadString(response.getContentAsString) \\ "link").text shouldEqual "http://example.com/v1/foo"
      }

      it("should remove the field if the token index is out of bounds (remove)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <xml>
            |                <xpath link-mismatch-action="remove" token-index="5">/link</xpath>
            |            </xml>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<link>http://example.com/v1/foo</link>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        response.getContentAsString shouldEqual """<?xml version="1.0" encoding="UTF-8"?>"""
      }

      it("should remove the field if the stripped path segment cannot be re-inserted in the link (remove)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <xml>
            |                <xpath link-mismatch-action="remove">/link</xpath>
            |            </xml>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<link>http://example.com/v2/bar</link>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        response.getContentAsString shouldEqual """<?xml version="1.0" encoding="UTF-8"?>"""
      }

      it("should fail if the link cannot be located (fail)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <xml>
            |                <xpath link-mismatch-action="fail">/dne</xpath>
            |            </xml>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<link>http://example.com/v1/foo</link>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        response.getStatus shouldEqual HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        response.getContentLength shouldEqual 0
      }

      it("should fail if the link's token index is out of bounds (fail)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <xml>
            |                <xpath link-mismatch-action="fail" token-index="5">/link</xpath>
            |            </xml>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<link>http://example.com/v1/foo</link>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        response.getStatus shouldEqual HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        response.getContentLength shouldEqual 0
      }

      it("should update multiple links given multiple xpaths") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <xml>
            |                <xpath>/root/link</xpath>
            |            </xml>
            |            <xml>
            |                <xpath>/root/linktwo</xpath>
            |            </xml>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<root>
            | <link>http://example.com/v1/foo</link>
            | <linktwo>http://example.com/v1/foo</linktwo>
            |</root>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        val xmlBody = XML.loadString(response.getContentAsString)
        (xmlBody \\ "link").text shouldEqual "http://example.com/v1/12345/foo"
        (xmlBody \\ "linktwo").text shouldEqual "http://example.com/v1/12345/foo"
      }

      it("should update multiple links given multiple xpaths with independent failure behaviors (continue, remove)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <xml>
            |                <xpath>/root/link</xpath>
            |            </xml>
            |            <xml>
            |                <xpath link-mismatch-action="remove" token-index="5">/root/linknumerodos</xpath>
            |            </xml>
            |            <xml>
            |                <xpath link-mismatch-action="continue">/dne</xpath>
            |            </xml>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<root>
            |  <link>http://example.com/v1/foo</link>
            |  <linknumerodos>http://example.com/v1/foo</linknumerodos>
            |</root>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        val xmlBody = XML.loadString(response.getContentAsString)
        (xmlBody \\ "link").text shouldEqual "http://example.com/v1/12345/foo"
        (xmlBody \\ "linknumerodos").text shouldEqual ""
      }


      it("should pass if the namespaces used in the response are specified in the config") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <xml>
            |                <namespace name="foo" url="bar"/>
            |                <xpath>/root/foo:service/foo:link</xpath>
            |            </xml>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """<?xml version="1.0" encoding="UTF-8"?><root xmlns:foo="bar">
            |  <foo:service>
            |    <foo:link>http://example.com/v1/foo</foo:link>
            |  </foo:service>
            |</root>""".stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        response.getContentAsString contains "<foo:link>http://example.com/v1/12345/foo</foo:link>"
      }

      it("should fail if the stripped path segment cannot be re-inserted in the link (fail)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <xml>
            |                <xpath link-mismatch-action="fail">/link</xpath>
            |            </xml>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<link>http://example.com/v2/bar</link>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        response.getStatus shouldEqual HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        response.getContentLength shouldEqual 0
      }

      it("should fail if the namespaces used in the response are not specified in the config (fail)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <response>
            |            <xml>
            |                <namespace name="foo" url="bar"/>
            |                <xpath link-mismatch-action="fail">/service/link</xpath>
            |            </xml>
            |        </response>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val respBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<badnamespace:service xmlns:badnamespace="bar">
            |  <badnamespace:link>http://example.com/v1/foo</badnamespace:link>
            |</badnamespace:service>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        setResponseBody(respBody, MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        response.getStatus shouldEqual HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        response.getContentLength shouldEqual 0
      }
    }
  }

  /**
    * Grabs the wrapped response that was passed in to the filterChain and sets the body
    */
  def setResponseBody(body: String, contentType: String): Unit = {
    val responseCaptor = ArgumentCaptor.forClass(classOf[HttpServletResponseWrapper])
    doAnswer(new Answer[Unit]() {
      def answer(invocation: InvocationOnMock): Unit = {
        val response = responseCaptor.getValue
        val bodyStream = new ByteArrayInputStream(body.getBytes)

        response.setContentType(contentType)
        response.setContentLength(bodyStream.available)
        response.setOutput(bodyStream)
      }
    }).when(filterChain).doFilter(any(classOf[HttpServletRequestWrapper]), responseCaptor.capture())
  }
}
