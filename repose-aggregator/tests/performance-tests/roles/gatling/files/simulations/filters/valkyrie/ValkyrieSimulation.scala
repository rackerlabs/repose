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

package filters.valkyrie

import com.typesafe.config.ConfigFactory
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

import scala.concurrent.duration._
import scala.util.Random

/**
  * Valkyrie filter performance simulation.
  */
class ValkyrieSimulation extends Simulation {
  import ValkyrieSimulation._

  // properties to configure the Gatling test
  val conf = ConfigFactory.load("application.conf")
  val confRoot = "test"
  val throughput = conf.getInt(s"$confRoot.throughput")
  val duration = conf.getInt(s"$confRoot.duration")
  val warmUpDuration = conf.getInt(s"$confRoot.warmup_duration")
  val rampUpUsers = conf.getInt(s"$confRoot.ramp_up_users.new_per_sec")
  val rampUpDuration = conf.getInt(s"$confRoot.ramp_up_users.duration_in_sec")
  val maxResponseTime = conf.getInt(s"$confRoot.expectations.max_response_time")
  val percentSuccessfulRequest = conf.getInt(s"$confRoot.expectations.percent_successful_requests")

  // this value is provided through a Java property on the command line when Gatling is run
  val baseUrl = conf.getString("test.base_url")

  val httpConf = http.baseURL(s"http://$baseUrl")

  val feeder = Iterator.continually(Map(
    "tenantId" -> s"hybrid:${Random.numeric.take(8).mkString}",
    "authToken" -> Random.alphanumeric.take(10).mkString,
    "authUser" -> Random.numeric.take(8).mkString,
    "contactId" -> Random.numeric.take(8).mkString
  ))

  // set up the warm up scenario
  val warmup = scenario("Warmup")
    .feed(feeder)
    .forever() {
      exec(getResourceWithDeviceId)
    }
    .inject(
      constantUsersPerSec(rampUpUsers) during(rampUpDuration seconds))
    .throttle(
      jumpToRps(throughput), holdFor(warmUpDuration minutes),  // warm up period
      jumpToRps(0), holdFor(duration minutes))                 // stop scenario during actual test

  // set up the main scenario
  val mainScenario = scenario("Valkyrie Filter Test")
    .feed(feeder)
    .forever() {
      exec(getResourceWithDeviceId)
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
    global.responseTime.percentile4.lte(maxResponseTime),
    global.successfulRequests.percent.gte(percentSuccessfulRequest)
  ).protocols(httpConf)

  def getResourceWithDeviceId: HttpRequestBuilder = {
    def deviceId = Random.numeric.take(8).mkString

    http(session => session.scenario)
      .get(_ => s"/resource/$deviceId")
      .header(HttpHeaderNames.Accept, HttpHeaderValues.ApplicationJson)
      .header("x-tenant-id", "${tenantId}")
      .header("x-auth-token", "${authToken}")
      .header("x-auth-user", "${authUser}")
      .header("x-contact-id", "${contactId}")
      .check(status.is(200))
  }
}

object ValkyrieSimulation {
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
