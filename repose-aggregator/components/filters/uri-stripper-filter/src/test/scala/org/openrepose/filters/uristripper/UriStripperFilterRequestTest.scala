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

import java.nio.charset.Charset
import javax.servlet.http.HttpServletResponse
import javax.servlet.{FilterChain, ServletResponse}

import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.openrepose.commons.utils.http.media.MimeType
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse}
import play.api.libs.json.{JsDefined, JsUndefined, Json}

import scala.xml.XML

@RunWith(classOf[JUnitRunner])
class UriStripperFilterRequestTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {

  import UriStripperFilterRequestTest._

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

  describe("request links handling") {
    describe("json") {
      it("should not alter the body if the uri does not match the configured regex") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex="/v1/[^/]+/bar">
            |        <request>
            |            <json>$.link</json>
            |        </request>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val requestBody =
          """{
            |  "link": "http://example.com/v1/12345/bar"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        getPostFilterRequestBody shouldEqual requestBody
      }

      it("should not alter the body if the method does not match the configured method") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*" http-methods="POST">
            |        <request>
            |            <json>$.link</json>
            |        </request>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val requestBody =
          """{
            |  "link": "http://example.com/v1/12345/foo"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setMethod("GET")
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        getPostFilterRequestBody shouldEqual requestBody
      }

      it("should not alter the body if the method does not match the configured resource type (response in lieu of request") {
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

        val requestBody =
          """{
            |  "link": "http://example.com/v1/12345/foo"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        getPostFilterRequestBody shouldEqual requestBody
      }

      it("should not alter the body if the content-type is not supported (Text)") {
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

        val requestBody =
          """
            |The link is http://example.com/v1/12345/foo
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.TEXT_PLAIN.toString)

        filter.doFilter(request, response, filterChain)

        getPostFilterRequestBody shouldEqual requestBody
      }

      it("should not alter the body if the content-type is not supported (XML)") {
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

        val requestBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<link>http://example.com/v2/12345/foo</link>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.TEXT_XML.toString)

        filter.doFilter(request, response, filterChain)

        getPostFilterRequestBody shouldEqual requestBody
      }

      it("should update a link by stripping the token at the parent index from the request body") {
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

        val requestBody =
          """{
            |  "link": "http://example.com/v2/12345/foo"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        (Json.parse(getPostFilterRequestBody) \ "link").as[String] shouldEqual "http://example.com/v2/foo"
      }

      it("should update a link by stripping the token at the configured index from the request body") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <request>
            |            <json token-index="2">$.link</json>
            |        </request>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val requestBody =
          """{
            |  "link": "http://example.com/v1/bar/12345"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        (Json.parse(getPostFilterRequestBody) \ "link").as[String] shouldEqual "http://example.com/v1/bar"
      }

      it("should not alter the body if the link cannot be located (continue)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <request>
            |            <json link-mismatch-action="continue">$.dne</json>
            |        </request>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val requestBody =
          """{
            |  "link": "http://example.com/v1/12345/foo"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        getPostFilterRequestBody shouldEqual Json.parse(requestBody).toString()
      }

      it("should not alter the body if the link's token index is out of bounds (continue)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <request>
            |            <json link-mismatch-action="continue" token-index="5">$.link</json>
            |        </request>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val requestBody =
          """{
            |  "link": "http://example.com/v1/12345/foo"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        getPostFilterRequestBody shouldEqual Json.parse(requestBody).toString()
      }

      it("should not alter the body if the link cannot be located (remove)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <request>
            |            <json link-mismatch-action="remove">$.dne</json>
            |        </request>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val requestBody =
          """{
            |  "link": "http://example.com/v1/12345/foo"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        getPostFilterRequestBody shouldEqual Json.parse(requestBody).toString()
      }

      it("should remove the field if the token index is out of bounds (remove)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <request>
            |            <json link-mismatch-action="remove" token-index="5">$.link</json>
            |        </request>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val requestBody =
          """{
            |  "link": "http://example.com/v1/12345/foo"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        getPostFilterRequestBody shouldEqual "{}"
      }

      it("should fail if the link cannot be located (fail)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <request>
            |            <json link-mismatch-action="fail">$.dne</json>
            |        </request>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val requestBody =
          """{
            |  "link": "http://example.com/v1/12345/foo"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        response.getStatus shouldEqual HttpServletResponse.SC_BAD_REQUEST
        response.getContentLength shouldEqual 0
      }

      it("should fail if the link's token index is out of bounds (fail)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <request>
            |            <json link-mismatch-action="fail" token-index="5">$.link</json>
            |        </request>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val requestBody =
          """{
            |  "link": "http://example.com/v1/12345/foo"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        response.getStatus shouldEqual HttpServletResponse.SC_BAD_REQUEST
        response.getContentLength shouldEqual 0
      }

      it("should update multiple links given multiple JSON paths") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <request>
            |            <json>$.link</json>
            |            <json>$.linktwo</json>
            |        </request>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val requestBody =
          """{
            |  "link": "http://example.com/v1/12345/foo",
            |  "linktwo": "http://example.com/v1/12345/foo"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        val jsonBody = Json.parse(getPostFilterRequestBody)
        (jsonBody \ "link").asInstanceOf[JsDefined].value.toString() shouldEqual """"http://example.com/v1/foo""""
        (jsonBody \ "linktwo").asInstanceOf[JsDefined].value.toString() shouldEqual """"http://example.com/v1/foo""""
      }

      it("should update multiple links given multiple JSON paths with independent failure behaviors (continue, remove)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <request>
            |            <json>$.link</json>
            |            <json link-mismatch-action="remove" token-index="5">$.linknumerodos</json>
            |            <json link-mismatch-action="continue">$.dne</json>
            |        </request>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val requestBody =
          """{
            |  "link": "http://example.com/v1/12345/foo",
            |  "linknumerodos": "http://example.com/v1/12345/foo"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        val jsonBody = Json.parse(getPostFilterRequestBody)
        (jsonBody \ "link").asInstanceOf[JsDefined].value.toString() shouldEqual """"http://example.com/v1/foo""""
        (jsonBody \ "linknumerodos").isInstanceOf[JsUndefined] shouldEqual true
      }
    }

    describe("xml") {
      it("should not alter the body if the uri does not match the configured regex") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex="/v1/[^/]+/bar">
            |        <request>
            |            <xml>
            |                <xpath>/link</xpath>
            |            </xml>
            |        </request>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val requestBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<link>http://example.com/v1/12345/bar</link>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        getPostFilterRequestBody shouldEqual requestBody
      }

      it("should not alter the body if the method does not match the configured method") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*" http-methods="POST">
            |        <request>
            |            <xml>
            |                <xpath>/link</xpath>
            |            </xml>
            |        </request>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val requestBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<link>http://example.com/v1/12345/foo</link>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setMethod("GET")
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        getPostFilterRequestBody shouldEqual requestBody
      }

      it("should not alter the body if the method does not match the configured resource type (response in lieu of request") {
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

        val requestBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<link>http://example.com/v1/12345/foo</link>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        getPostFilterRequestBody shouldEqual requestBody
      }

      it("should not alter the body if the content-type is not supported (Text)") {
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

        val requestBody =
          """
            |The link is http://example.com/v1/12345/foo
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.TEXT_PLAIN.toString)

        filter.doFilter(request, response, filterChain)

        getPostFilterRequestBody shouldEqual requestBody
      }

      it("should not alter the body if the content-type is not supported (JSON)") {
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

        val requestBody =
          """{
            |  "link": "http://example.com/v1/12345/foo"
            |}
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.APPLICATION_JSON.toString)

        filter.doFilter(request, response, filterChain)

        getPostFilterRequestBody shouldEqual requestBody
      }

      it("should update a link by stripping the token at the parent index from the request body") {
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

        val requestBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<link>http://example.com/v2/12345/foo</link>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        (XML.loadString(getPostFilterRequestBody) \\ "link").text shouldEqual "http://example.com/v2/foo"
      }

      it("should update a link by stripping the token at the configured index from the request body") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <request>
            |            <xml>
            |                <xpath token-index="2">/link</xpath>
            |            </xml>
            |        </request>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val requestBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<link>http://example.com/v1/bar/12345</link>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        (XML.loadString(getPostFilterRequestBody) \\ "link").text shouldEqual "http://example.com/v1/bar"
      }

      it("should not alter the body if the link cannot be located (continue)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <request>
            |            <xml>
            |                <xpath link-mismatch-action="continue">/dne</xpath>
            |            </xml>
            |        </request>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val requestBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<link>http://example.com/v1/12345/foo</link>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        XML.loadString(getPostFilterRequestBody).toString() shouldEqual XML.loadString(requestBody).toString()
      }

      it("should not alter the body if the link's token index is out of bounds (continue)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <request>
            |            <xml>
            |                <xpath link-mismatch-action="continue" token-index="5">/link</xpath>
            |            </xml>
            |        </request>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val requestBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<link>http://example.com/v1/12345/foo</link>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        XML.loadString(getPostFilterRequestBody).toString() shouldEqual XML.loadString(requestBody).toString()
      }

      it("should not alter the body if the link cannot be located (remove)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <request>
            |            <xml>
            |                <xpath link-mismatch-action="remove">/dne</xpath>
            |            </xml>
            |        </request>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val requestBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<link>http://example.com/v1/12345/foo</link>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        (XML.loadString(getPostFilterRequestBody) \\ "link").text shouldEqual "http://example.com/v1/12345/foo"
      }

      it("should remove the field if the token index is out of bounds (remove)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <request>
            |            <xml>
            |                <xpath link-mismatch-action="remove" token-index="5">/link</xpath>
            |            </xml>
            |        </request>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val requestBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<link>http://example.com/v1/12345/foo</link>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        getPostFilterRequestBody shouldEqual """<?xml version="1.0" encoding="UTF-8"?>"""
      }

      it("should fail if the link cannot be located (fail)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <request>
            |            <xml>
            |                <xpath link-mismatch-action="fail">/dne</xpath>
            |            </xml>
            |        </request>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val requestBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<link>http://example.com/v1/12345/foo</link>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        response.getStatus shouldEqual HttpServletResponse.SC_BAD_REQUEST
        response.getContentLength shouldEqual 0
      }

      it("should fail if the link's token index is out of bounds (fail)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <request>
            |            <xml>
            |                <xpath link-mismatch-action="fail" token-index="5">/link</xpath>
            |            </xml>
            |        </request>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val requestBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<link>http://example.com/v1/12345/foo</link>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        response.getStatus shouldEqual HttpServletResponse.SC_BAD_REQUEST
        response.getContentLength shouldEqual 0
      }

      it("should update multiple links given multiple xpaths") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <request>
            |            <xml>
            |                <xpath>/root/link</xpath>
            |            </xml>
            |            <xml>
            |                <xpath>/root/linktwo</xpath>
            |            </xml>
            |        </request>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val requestBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<root>
            | <link>http://example.com/v1/12345/foo</link>
            | <linktwo>http://example.com/v1/12345/foo</linktwo>
            |</root>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        val xmlBody = XML.loadString(getPostFilterRequestBody)
        (xmlBody \\ "link").text shouldEqual "http://example.com/v1/foo"
        (xmlBody \\ "linktwo").text shouldEqual "http://example.com/v1/foo"
      }

      it("should update multiple links given multiple xpaths with independent failure behaviors (continue, remove)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <request>
            |            <xml>
            |                <xpath>/root/link</xpath>
            |            </xml>
            |            <xml>
            |                <xpath link-mismatch-action="remove" token-index="5">/root/linknumerodos</xpath>
            |            </xml>
            |            <xml>
            |                <xpath link-mismatch-action="continue">/dne</xpath>
            |            </xml>
            |        </request>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val requestBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<root>
            |  <link>http://example.com/v1/12345/foo</link>
            |  <linknumerodos>http://example.com/v1/12345/foo</linknumerodos>
            |</root>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        val xmlBody = XML.loadString(getPostFilterRequestBody)
        (xmlBody \\ "link").text shouldEqual "http://example.com/v1/foo"
        (xmlBody \\ "linknumerodos").text shouldEqual ""
      }

      it("should pass if the namespaces used in the request are specified in the config") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <request>
            |            <xml>
            |                <namespace name="foo" url="bar"/>
            |                <xpath>/root/foo:service/foo:link</xpath>
            |            </xml>
            |        </request>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val requestBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<root xmlns:foo="bar">
            |  <foo:service>
            |    <foo:link>http://example.com/v1/12345/foo</foo:link>
            |  </foo:service>
            |</root>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        getPostFilterRequestBody contains "<foo:link>http://example.com/v1/foo</foo:link>"
      }

      it("should fail if the namespaces used in the request are not specified in the config (fail)") {
        val config =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<uri-stripper xmlns="http://docs.openrepose.org/repose/uri-stripper/v1.0" rewrite-location="false" token-index="1">
            |    <link-resource uri-path-regex=".*">
            |        <request>
            |            <xml>
            |                <namespace name="foo" url="bar"/>
            |                <xpath link-mismatch-action="fail">/service/link</xpath>
            |            </xml>
            |        </request>
            |    </link-resource>
            |</uri-stripper>
          """.stripMargin

        val requestBody =
          """<?xml version="1.0" encoding="UTF-8"?>
            |<badnamespace:service xmlns:badnamespace="bar">
            |  <badnamespace:link>http://example.com/v1/12345/foo</badnamespace:link>
            |</badnamespace:service>
          """.stripMargin

        filter.configurationUpdated(Marshaller.uriStripperConfigFromString(config))
        request.setRequestURI("/v1/12345/foo")
        request.setContent(requestBody.getBytes(CHARSET_UTF8))
        request.setContentType(MimeType.APPLICATION_XML.toString)

        filter.doFilter(request, response, filterChain)

        response.getStatus shouldEqual HttpServletResponse.SC_BAD_REQUEST
        response.getContentLength shouldEqual 0
      }
    }
  }

  def getPostFilterRequestBody: String = {
    val requestCaptor = ArgumentCaptor.forClass(classOf[HttpServletRequestWrapper])
    verify(filterChain).doFilter(requestCaptor.capture(), any(classOf[ServletResponse]))
    scala.io.Source.fromInputStream(requestCaptor.getValue.getInputStream)(scala.io.Codec.UTF8).mkString
  }
}

object UriStripperFilterRequestTest {
  val CHARSET_UTF8 = Charset.forName("UTF-8")
}
