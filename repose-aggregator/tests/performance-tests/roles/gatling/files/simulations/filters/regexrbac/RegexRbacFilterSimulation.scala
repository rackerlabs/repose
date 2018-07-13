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

package filters.regexrbac

import filters.regexrbac.RegexRbacFilterSimulation._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder
import org.openrepose.performance.test.AbstractReposeSimulation

/**
  * RegEx RBAC filter performance simulation.
  */
class RegexRbacFilterSimulation extends AbstractReposeSimulation {
  // set up the warm up scenario
  override val warmupScenario = scenario("Warmup")
    .forever() {
      exec(getFooBarRole1)
      exec(getFooBarRole2)
      exec(getFooBarBoth)
      exec(getFooBarBazSpace)
    }

  // set up the main scenario
  override val mainScenario = scenario("RegEx RBAC Filter Test")
    .forever() {
      exec(getFooBarRole1)
      exec(getFooBarRole2)
      exec(getFooBarBoth)
      exec(getFooBarBazSpace)
    }

  // run the scenarios
  runScenarios()

  def getFooBarRole1: HttpRequestBuilder = http(session => session.scenario)
    .get("/foo/bar")
    .header(HttpHeaderNames.Host, "localhost")
    .header(HttpHeaderNames.Accept, "*/*")
    .header("X-Roles", "role1")
    .check(status.is(StatusCodeForbidden))

  def getFooBarRole2: HttpRequestBuilder = http(session => session.scenario)
    .get("/foo/bar")
    .header(HttpHeaderNames.Host, "localhost")
    .header(HttpHeaderNames.Accept, "*/*")
    .header("X-Roles", "role2")
    .check(status.is(StatusCodeForbidden))

  def getFooBarBoth: HttpRequestBuilder = http(session => session.scenario)
    .get("/foo/bar")
    .header(HttpHeaderNames.Host, "localhost")
    .header(HttpHeaderNames.Accept, "*/*")
    .header("X-Roles", "role1, role2")
    .check(status.is(StatusCodeOk))

  def getFooBarBazSpace: HttpRequestBuilder = http(session => session.scenario)
    .get("/foo/bar/baz")
    .header(HttpHeaderNames.Host, "localhost")
    .header("X-Roles", "role5")
    .check(status.is(StatusCodeOk))
}

object RegexRbacFilterSimulation {
  val StatusCodeOk = 200
  val StatusCodeForbidden = 403
}
