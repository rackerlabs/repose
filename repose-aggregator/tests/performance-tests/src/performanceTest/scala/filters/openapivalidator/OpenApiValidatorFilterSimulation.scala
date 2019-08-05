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

package filters.openapivalidator

import filters.openapivalidator.OpenApiValidatorFilterSimulation._
import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder
import org.openrepose.performance.test.AbstractReposeSimulation

/**
  * OpenAPI Validator filter performance simulation.
  */
class OpenApiValidatorFilterSimulation extends AbstractReposeSimulation {
  var feederGet: Array[Map[String, Any]] =
    ScenariosGet.map { case (path, respcode) =>
      Map(
        "getPath" -> path,
        "getRespCode" -> respcode
      )
    }

  var feederPost: Array[Map[String, Any]] =
    ScenariosPost.map { case (path, respcode) =>
      Map(
        "postPath" -> path,
        "postRespCode" -> respcode
      )
    }

  var feederDelete: Array[Map[String, Any]] =
    ScenariosDelete.map { case (path, respcode) =>
      Map(
        "deletePath" -> path,
        "deleteRespCode" -> respcode
      )
    }

  var feederRole: Array[Map[String, Any]] =
    ScenariosDelete.map { case (role, respcode) =>
      Map(
        "role" -> role,
        "roleRespCode" -> respcode
      )
    }

  // set up the warm up scenario
  override val warmupScenario: ScenarioBuilder = scenario("Warmup")
    .feed(feederGet.circular)
    .feed(feederPost.circular)
    .feed(feederDelete.circular)
    .feed(feederRole.circular)
    .forever {
      exec(requestGet)
      exec(requestPost)
      exec(requestDelete)
      exec(requestRole)
    }

  // set up the main scenario
  override val mainScenario: ScenarioBuilder = scenario("API Validator Filter Test")
    .feed(feederGet.circular)
    .feed(feederPost.circular)
    .feed(feederDelete.circular)
    .feed(feederRole.circular)
    .forever {
      exec(requestGet)
      exec(requestPost)
      exec(requestDelete)
      exec(requestRole)
    }

  // run the scenarios
  runScenarios()

  def requestGet: HttpRequestBuilder = {
    http(session => session.scenario)
      .get("${getPath}")
      .header(HttpHeaderNames.Host, "localhost")
      .header(HeaderMockRespStatus, "${getRespCode}")
      .check(status.is(session => session("getRespCode").as[Int]))
  }

  def requestPost: HttpRequestBuilder = {
    http(session => session.scenario)
      .post("${postPath}")
      .header(HttpHeaderNames.Host, "localhost")
      .header(HeaderMockRespStatus, "${postRespCode}")
      .body(StringBody("{\"name\":\"Fido\"}"))
      .asJSON
      .check(status.is(session => session("postRespCode").as[Int]))
  }

  def requestDelete: HttpRequestBuilder = {
    http(session => session.scenario)
      .delete("${deletePath}")
      .header(HttpHeaderNames.Host, "localhost")
      .header(HeaderMockRespStatus, "${deleteRespCode}")
      .check(status.is(session => session("deleteRespCode").as[Int]))
  }

  def requestRole: HttpRequestBuilder = {
    http(session => session.scenario)
      .get("/roles")
      .header(HttpHeaderNames.Host, "localhost")
      .header(HeaderMockRespStatus, s"$StatusCodeNoContent")
      .header(HeaderRole, "${role}, banana")
      .check(status.is(session => session("roleRespCode").as[Int]))
  }
}

object OpenApiValidatorFilterSimulation {
  val HeaderMockRespStatus = "Mock-Origin-Res-Status"
  val HeaderRole = "X-Role"
  val StatusCodeOk = 200
  val StatusCodeNoContent = 204
  val StatusCodeUnauthorized =401
  val StatusCodeNotFound = 404
  val StatusCodeMethodNotAllowed = 405

  //(path: String   , respcode: Int)
  private val ScenariosGet = Array[(String, Int)](
    ("/pets", StatusCodeOk),
    ("/pets/17362", StatusCodeOk),
    ("/notPets", StatusCodeNotFound))
  private val ScenariosPost = Array[(String, Int)](
    ("/pets", StatusCodeOk),
    ("/pets/17362", StatusCodeMethodNotAllowed),
    ("/notPets", StatusCodeNotFound))
  private val ScenariosDelete = Array[(String, Int)](
    ("/pets", StatusCodeMethodNotAllowed),
    ("/pets/17362", StatusCodeOk),
    ("/notPets", StatusCodeNotFound))

  //(role: String   , respcode: Int)
  private val ScenariosRole = Array[(String, Int)](
    ("foo", StatusCodeNoContent),
    ("bar", StatusCodeNoContent),
    ("baz", StatusCodeUnauthorized))
}
