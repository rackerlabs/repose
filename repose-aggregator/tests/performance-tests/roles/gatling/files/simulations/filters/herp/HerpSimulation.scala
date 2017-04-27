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

package filters.herp

import com.typesafe.config.ConfigFactory
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

import scala.concurrent.duration._
import scala.util.Random

/**
 * Herp filter performance simulation.
 */
class HerpSimulation extends Simulation {
  import HerpSimulation._

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

  val feeder = Iterator.continually(Map(
    "tenantId" -> s"hybrid:${Random.numeric.take(8).mkString}",
    "authToken" -> Random.alphanumeric.take(32).mkString,
    "userId" -> Random.numeric.take(8).mkString,
    "userName" -> Random.alphanumeric.take(10).mkString,
    "impersonatorId" -> Random.numeric.take(8).mkString,
    "impersonatorName" -> Random.alphanumeric.take(10).mkString
  ))

  // set up the warm up scenario
  val warmup = scenario("Warmup")
    .feed(feeder)
    .forever() {
      exec(getResource)
    }
    .inject(
      constantUsersPerSec(rampUpUsers) during(rampUpDuration seconds))
    .throttle(
      jumpToRps(throughput), holdFor(warmUpDuration minutes),  // warm up period
      jumpToRps(0), holdFor(duration minutes))                 // stop scenario during actual test

  // set up the main scenario
  val mainScenario = scenario("Herp Filter Test")
    .feed(feeder)
    .forever() {
      exec(getResource)
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

  def getResource: HttpRequestBuilder = {
    http(session => session.scenario)
      .get("/resource")
      .queryParam("tenantid", "12345")
      .header(HttpHeaderNames.Accept, HttpHeaderValues.ApplicationXml)
      .header(HttpHeaderNames.Host, "localhost")
      .header("x-tenant-id", "${tenantId}")
      .header("x-auth-token", "${authToken}")
      .header("x-roles", "default")
      .header("x-user-id", "${userId}")
      .header("x-user-name", "${userName}")
      .header("x-impersonator-id", "${impersonatorId}")
      .header("x-impersonator-name", "${impersonatorName}")
      .check(status.is(200))
  }
}

object HerpSimulation {
  implicit class RandomStreams(val rand: Random) {
    def numeric: Stream[Char] = {
      def nextNum: Char = {
        val chars = "0123456789"
        chars charAt (rand nextInt chars.length)
      }

      Stream continually nextNum
    }
  }
}
