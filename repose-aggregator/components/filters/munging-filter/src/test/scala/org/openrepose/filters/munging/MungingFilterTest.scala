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
package org.openrepose.filters.munging

import javax.servlet.FilterConfig

import org.hamcrest.Matchers.{endsWith, hasProperty}
import org.junit.runner.RunWith
import org.mockito.Matchers.{any, anyString, argThat, eq => eql}
import org.mockito.Mockito._
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.munging.config.{ChangeDetails, HeaderFilter, MungingConfig, Patch}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.springframework.mock.web.MockHttpServletRequest
import spray.json._

/**
  * Created by adrian on 5/2/16.
  */
@RunWith(classOf[JUnitRunner])
class MungingFilterTest
  extends FunSpec
    with Matchers
    with MockitoSugar
    with BeforeAndAfter {

  var filter: MungingFilter = _
  var configurationService: ConfigurationService = _

  before {
    configurationService= mock[ConfigurationService]
    filter = new MungingFilter(configurationService)
  }

  describe("init method") {
    it("should have the correct default config and schema") {
      filter.init(mock[FilterConfig])

      verify(configurationService).subscribeTo(anyString(), eql("munging.cfg.xml"),
        argThat(hasProperty("path", endsWith("/META-INF/schema/config/munging.xsd"))),
        any(classOf[UpdateListener[MungingConfig]]), any(classOf[Class[MungingConfig]]))
    }
  }

  describe("filterChanges method") {
    it("should select the correct changes based on path regex") {
      filter.configurationUpdated(basicConfig)
      val request: MockHttpServletRequest = new MockHttpServletRequest()
      request.setRequestURI("http://rackspace.com/bars")

      val changes: List[ChangeDetails] = filter.filterChanges(request)

      changes.length shouldBe 2
      changes should contain allOf (allChange, barChange)
    }

    it("should select the correct changes based on headers with correct value present") {
      filter.configurationUpdated(basicConfig)
      val request: MockHttpServletRequest = new MockHttpServletRequest()
      request.setRequestURI("http://rackspace.com/foo")
      request.addHeader("banana", "phone")

      val changes: List[ChangeDetails] = filter.filterChanges(request)

      changes.length shouldBe 2
      changes should contain allOf (allChange, fooChange)
    }

    it("should select the correct changes based on headers with incorrect value present") {
      filter.configurationUpdated(basicConfig)
      val request: MockHttpServletRequest = new MockHttpServletRequest()
      request.setRequestURI("http://rackspace.com/foo")
      request.addHeader("banana", "fiend")

      val changes: List[ChangeDetails] = filter.filterChanges(request)

      changes.length shouldBe 1
      changes should contain (allChange)
    }

    it("should select the correct changes based on headers when not present") {
      filter.configurationUpdated(basicConfig)
      val request: MockHttpServletRequest = new MockHttpServletRequest()
      request.setRequestURI("http://rackspace.com/foo")

      val changes: List[ChangeDetails] = filter.filterChanges(request)

      changes.length shouldBe 1
      changes should contain (allChange)
    }
  }

  describe("filterRequestChanges method") {
    it("should select those patches that apply to the request") {
      val patches: List[Patch] = filter.filterRequestChanges(List(allChange, fooChange, barChange))

      patches.length shouldBe 2
      patches should contain allOf (allRequestPatch, fooPatch)
    }
  }

  describe("filterResponseChanges method") {
    it("should select those patches that apply to the response") {
      val patches: List[Patch] = filter.filterResponseChanges(List(allChange, fooChange, barChange))

      patches.length shouldBe 2
      patches should contain allOf (allResponsePatch, barPatch)
    }
  }

  describe("filterJsonPatches method") {
    it("should select only where there is a json element") {
      val patches: List[String] = filter.filterJsonPatches(List(allRequestPatch, allResponsePatch, new Patch()))

      patches.length shouldBe 2
      patches should contain allOf (allRequestPatch.getJson, allResponsePatch.getJson)
    }
  }

  describe("filterXmlPatches method") (pending)

  describe("applyJsonPatches method") {
    val body: String =
      """
        |{
        |   "some": "json",
        |   "nested": {
        |       "json": "object"
        |   }
        |}
      """.stripMargin

    //todo: bring this back when we get to repose 8 and we can use play instead of spray
//    it("should apply patches") {
//      val patched: JsValue = filter.applyJsonPatches(Json.parse(body), List(allRequestPatch.getJson, fooPatch.getJson))
//
//      (patched \ "all").as[String] shouldBe "request"
//      (patched \ "foo").as[String] shouldBe "request"
//    }

    it("should apply patches") {
      val patched: JsValue = filter.applyJsonPatches(body.parseJson, List(allRequestPatch.getJson, fooPatch.getJson))
       val json: String = patched.prettyPrint

      json should include (""""all": "request"""")
      json should include (""""foo": "request"""")
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
                                        .withHeaderFilter(new HeaderFilter().withName("banana").withValue("phon.*"))
                                        .withRequest(fooPatch)
  val barChange: ChangeDetails = new ChangeDetails().withPath("/bar.*")
                                        .withResponse(barPatch)
  val basicConfig: MungingConfig = new MungingConfig().withChange(allChange, fooChange, barChange)
}
