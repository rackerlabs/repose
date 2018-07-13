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

package filters.apivalidator

import filters.apivalidator.ApiValidatorFilterSimulation._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder
import org.openrepose.performance.test.AbstractReposeSimulation

/**
  * API Validator filter performance simulation.
  */
class ApiValidatorFilterSimulation extends AbstractReposeSimulation {
  var feederGet: Array[Map[String, Any]] =
    ScenariosGet map { case (path, roles, respcode) =>
      Map(
        "getPath" -> path,
        "getRoles" -> roles,
        "getRespCode" -> respcode
      )
    }

  var feederPut: Array[Map[String, Any]] =
    ScenariosPut map { case (path, roles, respcode) =>
      Map(
        "putPath" -> path,
        "putRoles" -> roles,
        "putRespCode" -> respcode
      )
    }

  var feederPost: Array[Map[String, Any]] =
    ScenariosPost map { case (path, roles, respcode) =>
      Map(
        "postPath" -> path,
        "postRoles" -> roles,
        "postRespCode" -> respcode
      )
    }

  var feederDelete: Array[Map[String, Any]] =
    ScenariosDelete map { case (path, roles, respcode) =>
      Map(
        "deletePath" -> path,
        "deleteRoles" -> roles,
        "deleteRespCode" -> respcode
      )
    }

  // set up the warm up scenario
  override val warmupScenario = scenario("Warmup")
    .feed(feederGet.circular)
    .feed(feederPut.circular)
    .feed(feederPost.circular)
    .feed(feederDelete.circular)
    .forever() {
      exec(requestGet)
      exec(requestPut)
      exec(requestPost)
      exec(requestDelete)
    }

  // set up the main scenario
  override val mainScenario = scenario("API Validator Filter Test")
    .feed(feederGet.circular)
    .feed(feederPut.circular)
    .feed(feederPost.circular)
    .feed(feederDelete.circular)
    .forever() {
      exec(requestGet)
      exec(requestPut)
      exec(requestPost)
      exec(requestDelete)
    }

  // run the scenarios
  runScenarios()

  def requestGet: HttpRequestBuilder = {
    http(session => session.scenario)
      .get("${getPath}")
      .header(HttpHeaderNames.Host, "localhost")
      .header(HeaderXRoles, "${getRoles}")
      .header(HeaderMockRespStatus, "${getRespCode}")
      .check(status.is(session => session("getRespCode").as[Int]))
  }

  def requestPut: HttpRequestBuilder = {
    http(session => session.scenario)
      .put("${putPath}")
      .header(HttpHeaderNames.Host, "localhost")
      .header(HeaderXRoles, "${putRoles}")
      .header(HeaderMockRespStatus, "${putRespCode}")
      .check(status.is(session => session("putRespCode").as[Int]))
  }

  def requestPost: HttpRequestBuilder = {
    http(session => session.scenario)
      .post("${postPath}")
      .header(HttpHeaderNames.Host, "localhost")
      .header(HeaderXRoles, "${postRoles}")
      .header(HeaderMockRespStatus, "${postRespCode}")
      .check(status.is(session => session("postRespCode").as[Int]))
  }

  def requestDelete: HttpRequestBuilder = {
    http(session => session.scenario)
      .delete("${deletePath}")
      .header(HttpHeaderNames.Host, "localhost")
      .header(HeaderXRoles, "${deleteRoles}")
      .header(HeaderMockRespStatus, "${deleteRespCode}")
      .check(status.is(session => session("deleteRespCode").as[Int]))
  }
}

object ApiValidatorFilterSimulation {
  val HeaderXRoles = "X-Roles"
  val HeaderMockRespStatus = "Mock-Origin-Res-Status"
  val StatusCodeOk = 200
  val StatusCodeForbidden = 403
  val StatusCodeNotFound = 404
  val StatusCodeMethodNotAllowed = 405

  // @formatter:off
  //(path: String        , roles: String  , respcode: Int             )
  val ScenariosGet = Array[(String, String, Int)](
    ("/path/to/this"     , "super"        , StatusCodeOk              ),
    ("/path/to/this"     , "useradmin"    , StatusCodeOk              ),
    ("/path/to/this"     , "admin"        , StatusCodeOk              ),
    ("/path/to/this"     , "user"         , StatusCodeOk              ),
    ("/path/to/this"     , "none"         , StatusCodeForbidden       ),
    ("/path/to/that"     , "super"        , StatusCodeOk              ),
    ("/path/to/that"     , "useradmin"    , StatusCodeOk              ),
    ("/path/to/test"     , "user"         , StatusCodeOk              ),
    ("/path/to/test"     , "admin"        , StatusCodeForbidden       ),
    ("/path/to/something", "user"         , StatusCodeNotFound        ),
    ("/path/to/something", "super"        , StatusCodeNotFound        ),
    ("/path/to/something", "admin"        , StatusCodeNotFound        ),
    ("/path/to/space"    , "super wsp"    , StatusCodeOk              ),
    ("/path/to/space"    , "useradmin wsp", StatusCodeOk              ),
    ("/path/to/space"    , "admin wsp"    , StatusCodeOk              ),
    ("/path/to/space"    , "super"        , StatusCodeForbidden       ),
    ("/path/to/space"    , "useradmin"    , StatusCodeForbidden       ),
    ("/path/to/space"    , "admin"        , StatusCodeForbidden       ),
    ("/path/to/space"    , "wsp"          , StatusCodeForbidden       ))
  val ScenariosPut = Array[(String, String, Int)](
    ("/path/to/this"     , "super"        , StatusCodeOk              ),
    ("/path/to/this"     , "useradmin"    , StatusCodeOk              ),
    ("/path/to/this"     , "admin"        , StatusCodeOk              ),
    ("/path/to/this"     , "user"         , StatusCodeForbidden       ),
    ("/path/to/this"     , "none"         , StatusCodeForbidden       ),
    ("/path/to/that"     , "super"        , StatusCodeOk              ),
    ("/path/to/that"     , "useradmin"    , StatusCodeOk              ),
    ("/path/to/test"     , "user"         , StatusCodeMethodNotAllowed),
    ("/path/to/something", "useradmin"    , StatusCodeNotFound        ))
  val ScenariosPost = Array[(String, String, Int)](
    ("/path/to/this"     , "super"        , StatusCodeOk              ),
    ("/path/to/this"     , "useradmin"    , StatusCodeOk              ),
    ("/path/to/this"     , "admin"        , StatusCodeForbidden       ),
    ("/path/to/this"     , "user"         , StatusCodeForbidden       ),
    ("/path/to/this"     , "none"         , StatusCodeForbidden       ),
    ("/path/to/that"     , "super"        , StatusCodeOk              ),
    ("/path/to/that"     , "user"         , StatusCodeForbidden       ),
    ("/path/to/that"     , "super"        , StatusCodeOk              ),
    ("/path/to/test"     , "useradmin"    , StatusCodeOk              ),
    ("/path/to/test"     , "super"        , StatusCodeForbidden       ),
    ("/path/to/something", "none"         , StatusCodeNotFound        ))
  val ScenariosDelete = Array[(String, String, Int)](
    ("/path/to/this"     , "super"        , StatusCodeOk              ),
    ("/path/to/this"     , "useradmin"    , StatusCodeForbidden       ),
    ("/path/to/this"     , "admin"        , StatusCodeForbidden       ),
    ("/path/to/this"     , "user"         , StatusCodeForbidden       ),
    ("/path/to/this"     , "none"         , StatusCodeForbidden       ),
    ("/path/to/that"     , "super"        , StatusCodeOk              ),
    ("/path/to/that"     , "admin"        , StatusCodeForbidden       ),
    ("/path/to/that"     , "super"        , StatusCodeOk              ),
    ("/path/to/test"     , "useradmin"    , StatusCodeMethodNotAllowed))
  // @formatter:on
}
