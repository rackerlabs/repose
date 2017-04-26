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

package usecases.complex

import com.typesafe.config.ConfigFactory
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

import scala.concurrent.duration._
import scala.util.Random

/**
  * Complex use case performance simulation.
  */
class ComplexUseCaseSimulation extends Simulation {
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

  val feeder = Iterator.continually(Map("authToken" -> Random.alphanumeric.take(24).mkString))

  // set up the warm up scenario
  val warmup = scenario("Warmup")
    .feed(feeder)
    .forever() {
      exec(getUsers)
    }
    .inject(
      constantUsersPerSec(rampUpUsers) during(rampUpDuration seconds))
    .throttle(
      jumpToRps(throughput), holdFor(warmUpDuration minutes),  // warm up period
      jumpToRps(0), holdFor(duration minutes))                 // stop scenario during actual test

  // set up the main scenario
  val mainScenario = scenario("Complex Use Case Test")
    .feed(feeder)
    .forever() {
      exec(getUsers)
    }
    .inject(
      nothingFor(warmUpDuration minutes),  // do nothing during warm up period
      constantUsersPerSec(rampUpUsers) during(rampUpDuration seconds))
    .throttle(
      jumpToRps(throughput), holdFor((warmUpDuration + duration) minutes))

  // run the scenario
  setUp(
    warmup,
    mainScenario
  ).assertions(
    global.responseTime.percentile4.lte(percentile3ResponseTimeUpperBound),
    global.successfulRequests.percent.gte(percentSuccessfulRequest)
  ).protocols(httpConf)

  def getUsers: HttpRequestBuilder = {
    http(session => session.scenario)
      .get("/functest1/users/")
      .queryParam("limit", "1000")
      .queryParam("includeLocked", "true")
      .header("x-auth-token", "${authToken}")

      .check(status.is(200))
  }
}
