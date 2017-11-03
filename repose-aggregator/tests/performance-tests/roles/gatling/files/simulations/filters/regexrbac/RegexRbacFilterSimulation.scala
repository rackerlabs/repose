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

import com.typesafe.config.ConfigFactory
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

import filters.regexrbac.RegexRbacFilterSimulation._

import scala.concurrent.duration._

/**
  * RegEx RBAC filter performance simulation.
  */
class RegexRbacFilterSimulation extends Simulation {
  // properties to configure the Gatling test
  val conf = ConfigFactory.load("application.conf")
  val confRoot = "test"
  val throughput = conf.getInt(s"$confRoot.throughput")
  val duration = conf.getInt(s"$confRoot.duration")
  val warmUpDuration = conf.getInt(s"$confRoot.warmup_duration")
  val rampUpUsers = conf.getInt(s"$confRoot.ramp_up_users.new_per_sec")
  val rampUpDuration = conf.getInt(s"$confRoot.ramp_up_users.duration_in_sec")
  val percentile3ResponseTimeUpperBound = conf.getInt(s"$confRoot.expectations.percentile3_response_time_upper_bound")
  val percentSuccessfulRequest = conf.getInt(s"$confRoot.expectations.percent_successful_requests")

  // this value is provided through a Java property on the command line when Gatling is run
  val baseUrl = conf.getString("test.base_url")

  val httpConf = http.baseURL(s"http://$baseUrl")

  // set up the warm up scenario
  val warmup = scenario("Warmup")
    .forever() {
      exec(getFooBarRole1)
      exec(getFooBarRole2)
      exec(getFooBarBoth)
      exec(getFooBarBazSpace)
    }
    .inject(
      constantUsersPerSec(rampUpUsers) during(rampUpDuration seconds))
    .throttle(
      jumpToRps(throughput), holdFor(warmUpDuration minutes),  // warm up period
      jumpToRps(0), holdFor(duration minutes))                 // stop scenario during actual test

  // set up the main scenario
  val mainScenario = scenario("RegEx RBAC Filter Test")
    .forever() {
      exec(getFooBarRole1)
      exec(getFooBarRole2)
      exec(getFooBarBoth)
      exec(getFooBarBazSpace)
    }
    .inject(
      nothingFor(warmUpDuration minutes),  // do nothing during warm up period
      constantUsersPerSec(rampUpUsers) during(rampUpDuration seconds))
    .throttle(
      jumpToRps(throughput), holdFor((warmUpDuration + duration) minutes))

  // run the scenarios
  setUp(
    warmup,
    mainScenario
  ).assertions(
    global.responseTime.percentile3.lte(percentile3ResponseTimeUpperBound),
    global.successfulRequests.percent.gte(percentSuccessfulRequest)
  ).protocols(httpConf)

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
    .header("X-Roles", "role with space")
    .check(status.is(StatusCodeOk))
}

object RegexRbacFilterSimulation {
  val StatusCodeOk = 200
  val StatusCodeForbidden = 403
}
