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
package org.openrepose.filters.bodypatcher

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.servlet.{FilterChain, FilterConfig}

import org.hamcrest.Matchers.{endsWith, hasProperty}
import org.junit.runner.RunWith
import org.mockito.Matchers.{any, anyString, argThat, eq => eql}
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.bodypatcher.BodyPatcherFilter._
import org.openrepose.filters.bodypatcher.config.{BodyPatcherConfig, ChangeDetails, Patch}
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.springframework.mock.web.{MockFilterChain, MockHttpServletRequest, MockHttpServletResponse}
import play.api.libs.json.{JsResultException, JsValue, Json => PJson}

/**
  * Created by adrian on 5/2/16.
  */
@RunWith(classOf[JUnitRunner])
class BodyPatcherFilterTest
  extends FunSpec
    with Matchers
    with MockitoSugar
    with BeforeAndAfter {

  var filter: BodyPatcherFilter = _
  var configurationService: ConfigurationService = _

  before {
    configurationService = mock[ConfigurationService]
    filter = new BodyPatcherFilter(configurationService)
    filter.configurationUpdated(basicConfig)
  }

  describe("init method") {
    it("should have the correct default config and schema") {
      filter.init(mock[FilterConfig])

      verify(configurationService).subscribeTo(anyString(), eql("body-patcher.cfg.xml"),
        argThat(hasProperty("path", endsWith("/META-INF/schema/config/body-patcher.xsd"))),
        any(classOf[UpdateListener[BodyPatcherConfig]]), any(classOf[Class[BodyPatcherConfig]]))
    }
  }

  describe("filterPathChanges method") {
    it("should select the correct changes based on path regex") {
      val request = basicRequest("/bars", "")

      val changes: List[ChangeDetails] = filter.filterPathChanges(request)

      changes should contain allOf (allChange, barChange)
      changes.length shouldBe 2
    }
  }

  describe("filterRequestChanges method") {
    it("should select those patches that apply to the request") {
      val patches: List[Patch] = filter.filterRequestChanges(List(allChange, fooChange, barChange))

      patches should contain allOf (allRequestPatch, fooPatch)
      patches.length shouldBe 2
    }
  }

  describe("filterResponseChanges method") {
    it("should select those patches that apply to the response") {
      val patches: List[Patch] = filter.filterResponseChanges(List(allChange, fooChange, barChange))

      patches should contain allOf (allResponsePatch, barPatch)
      patches.length shouldBe 2
    }
  }

  describe("determineContentType method") {
    it("should match json based on a regex") {
      filter.determineContentType("application/json+butts") shouldBe Json
    }

    it("should match xml based on a regex") {
      filter.determineContentType("application/xml+butts") shouldBe Xml
    }

    it("should match other for random") {
      filter.determineContentType("foo/bar+butts") shouldBe Other
    }

    it("should match other when absent") {
      filter.determineContentType(null) shouldBe Other
      filter.determineContentType("") shouldBe Other
    }

    it("should match despite casing") {
      filter.determineContentType("application/JsOn+butts") shouldBe Json
    }
  }

  describe("filterJsonPatches method") {
    it("should select only where there is a json element") {
      val patches: List[String] = filter.filterJsonPatches(List(allRequestPatch, allResponsePatch, new Patch()))

      patches should contain allOf (allRequestPatch.getJson, allResponsePatch.getJson)
      patches.length shouldBe 2
    }
  }

  describe("filterXmlPatches method") (pending)

  describe("applyJsonPatches method") {
    it("should apply patches") {
      val patched: JsValue = filter.applyJsonPatches(PJson.parse(testBody), List(allRequestPatch.getJson, fooPatch.getJson))

      (patched \ "all").as[String] shouldBe "request"
      (patched \ "foo").as[String] shouldBe "request"
    }
  }

  describe("applyXmlPatches method") (pending)

  describe("doWork method") {
    it("should apply the appropriate patches to the request with simple path match") {
      val request = basicRequest("/phone", "banana/json")
      val chain: MockFilterChain = new MockFilterChain()

      filter.doWork(request, new MockHttpServletResponse(), chain)

      val content: JsValue = PJson.parse(chain.getRequest.getInputStream)
      (content \ "all").as[String] shouldBe "request"
      intercept[JsResultException] {
        (content \ "foo").as[String]
      }
      intercept[JsResultException] {
        (content \ "bar").as[String]
      }
      (content \ "some").as[String] shouldBe "json"
    }

    it("should apply the appropriate patches to the request with specific path match") {
      val request = basicRequest("/foo", "banana/json")
      val chain: MockFilterChain = new MockFilterChain()

      filter.doWork(request, new MockHttpServletResponse(), chain)

      val content: JsValue = PJson.parse(chain.getRequest.getInputStream)
      (content \ "all").as[String] shouldBe "request"
      (content \ "foo").as[String] shouldBe "request"
      intercept[JsResultException] {
        (content \ "bar").as[String]
      }
      (content \ "some").as[String] shouldBe "json"
    }

    it("should apply nothing to the body when content type on the request is wrong") {
      val request = basicRequest("/foo", "banana/phone")
      val chain: MockFilterChain = new MockFilterChain()

      filter.doWork(request, new MockHttpServletResponse(), chain)

      val content: JsValue = PJson.parse(chain.getRequest.getInputStream)
      intercept[JsResultException] {
        (content \ "all").as[String]
      }
        intercept[JsResultException] {
        (content \ "foo").as[String]
      }
      intercept[JsResultException] {
        (content \ "bar").as[String]
      }
      (content \ "some").as[String] shouldBe "json"
    }

    it("should return a 400 for malformed request body") {
      val request = basicRequest("/foo", "banana/json")
      request.setContent("this isn't proper json who would do such a thing".getBytes)
      val response: MockHttpServletResponse = new MockHttpServletResponse()

      filter.doWork(request, response, new MockFilterChain())

      response.getStatus shouldBe 400
      response.getErrorMessage shouldBe "Body was unparseable as specified content type"
    }

    it("should apply the appropriate patches to the response with simple path match") {
      val request = basicRequest("/foo", "banana/json")
      val response: MockHttpServletResponse = new MockHttpServletResponse()
      val chain: FilterChain = mock[FilterChain]
      when(chain.doFilter(any(classOf[HttpServletRequest]), any(classOf[HttpServletResponse]))).thenAnswer(new Answer[Unit] {
        override def answer(invocation: InvocationOnMock): Unit = {
          val chainedResponse: HttpServletResponse = invocation.getArguments()(1).asInstanceOf[HttpServletResponse]
          chainedResponse.setContentType("application/json")
          chainedResponse.getWriter.print(testBody)
          chainedResponse.flushBuffer()
        }
      } )

      filter.doWork(request, response, chain)

      val content: JsValue = PJson.parse(response.getContentAsString)
      (content \ "all").as[String] shouldBe "response"
      intercept[JsResultException] {
        (content \ "foo").as[String]
      }
      intercept[JsResultException] {
        (content \ "bar").as[String]
      }
      (content \ "some").as[String] shouldBe "json"
    }

    it("should apply the appropriate patches to the response with with specific path match") {
      val request = basicRequest("/b%61rcelona", "banana/json")
      val response: MockHttpServletResponse = new MockHttpServletResponse()
      val chain: FilterChain = mock[FilterChain]
      when(chain.doFilter(any(classOf[HttpServletRequest]), any(classOf[HttpServletResponse]))).thenAnswer(new Answer[Unit] {
        override def answer(invocation: InvocationOnMock): Unit = {
          val chainedResponse: HttpServletResponse = invocation.getArguments()(1).asInstanceOf[HttpServletResponse]
          chainedResponse.setContentType("application/json")
          chainedResponse.getWriter.print(testBody)
          chainedResponse.flushBuffer()
        }
      } )

      filter.doWork(request, response, chain)

      val content: JsValue = PJson.parse(response.getContentAsString)
      (content \ "all").as[String] shouldBe "response"
      intercept[JsResultException] {
        (content \ "foo").as[String]
      }
      (content \ "bar").as[String] shouldBe "response"
      (content \ "some").as[String] shouldBe "json"
    }

    it("should apply nothing to the body when content type on the response is wrong") {
      val request = basicRequest("/barcelona", "banana/json")
      val response: MockHttpServletResponse = new MockHttpServletResponse()
      val chain: FilterChain = mock[FilterChain]
      when(chain.doFilter(any(classOf[HttpServletRequest]), any(classOf[HttpServletResponse]))).thenAnswer(new Answer[Unit] {
        override def answer(invocation: InvocationOnMock): Unit = {
          val chainedResponse: HttpServletResponse = invocation.getArguments()(1).asInstanceOf[HttpServletResponse]
          chainedResponse.setContentType("application/html")
          chainedResponse.getWriter.print(testBody)
          chainedResponse.flushBuffer()
        }
      } )

      filter.doWork(request, response, chain)

      val content: JsValue = PJson.parse(response.getContentAsString)
      intercept[JsResultException] {
        (content \ "all").as[String]
      }
      intercept[JsResultException] {
        (content \ "foo").as[String]
      }
      intercept[JsResultException] {
        (content \ "bar").as[String]
      }
      (content \ "some").as[String] shouldBe "json"
    }

    it("should throw an exception when the response body is unparseable") {
      val request = basicRequest("/foo", "banana/json")
      val response: MockHttpServletResponse = new MockHttpServletResponse()
      val chain: FilterChain = mock[FilterChain]
      when(chain.doFilter(any(classOf[HttpServletRequest]), any(classOf[HttpServletResponse]))).thenAnswer(new Answer[Unit] {
        override def answer(invocation: InvocationOnMock): Unit = {
          val chainedResponse: HttpServletResponse = invocation.getArguments()(1).asInstanceOf[HttpServletResponse]
          chainedResponse.setContentType("application/json")
          chainedResponse.getWriter.print("this is not json, why would you send this and mark it as json?")
          chainedResponse.flushBuffer()
        }
      } )

      intercept[BodyUnparseableException] {
        filter.doWork(request, response, chain)
      }
    }
  }

  val allRequestPatch: Patch = new Patch().withJson("""[{"op":"add", "path":"/all", "value":"request"}]""")
  val allResponsePatch: Patch = new Patch().withJson("""[{"op":"add", "path":"/all", "value":"response"}]""")
  val fooPatch: Patch = new Patch().withJson("""[{"op":"add", "path":"/foo", "value":"request"}]""")
  val barPatch: Patch = new Patch().withJson("""[{"op":"add", "path":"/bar", "value":"response"}]""")
  val allChange: ChangeDetails = new ChangeDetails()
                                        .withRequest(allRequestPatch)
                                        .withResponse(allResponsePatch)
  val fooChange: ChangeDetails = new ChangeDetails().withPath("/foo")
                                        .withRequest(fooPatch)
  val barChange: ChangeDetails = new ChangeDetails().withPath("/bar.*")
                                        .withResponse(barPatch)
  val basicConfig: BodyPatcherConfig = new BodyPatcherConfig().withChange(allChange, fooChange, barChange)

  val testBody: String =
    """
      |{
      |   "some": "json",
      |   "nested": {
      |       "json": "object"
      |   }
      |}
    """.stripMargin

  def basicRequest(uri: String, contentType: String): MockHttpServletRequest = {
    val request: MockHttpServletRequest = new MockHttpServletRequest()
    request.setContentType(contentType)
    request.setProtocol("http")
    request.setServerName("rackspace.com")
    request.setRequestURI(uri)
    request.setContent(testBody.getBytes)
    request
  }
}
