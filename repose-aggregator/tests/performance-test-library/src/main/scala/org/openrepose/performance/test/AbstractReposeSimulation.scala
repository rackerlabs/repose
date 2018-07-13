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

package org.openrepose.performance.test

import com.typesafe.config.{Config, ConfigFactory}
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.{PopulationBuilder, ScenarioBuilder}
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.concurrent.duration._
import scala.language.postfixOps

abstract class AbstractReposeSimulation extends Simulation {
  // properties to configure the Gatling test
  val conf: Config = ConfigFactory.load("application.conf")
  val confRoot: String = "test"
  val throughput: Int = conf.getInt(s"$confRoot.throughput")
  val duration: Int = conf.getInt(s"$confRoot.duration")
  val warmUpDuration: Int = conf.getInt(s"$confRoot.warmup_duration")
  val rampUpUsers: Int = conf.getInt(s"$confRoot.ramp_up_users.new_per_sec")
  val rampUpDuration: Int = conf.getInt(s"$confRoot.ramp_up_users.duration_in_sec")
  val percentile3ResponseTimeUpperBound: Int = conf.getInt(s"$confRoot.expectations.percentile3_response_time_upper_bound")
  val percentSuccessfulRequest: Int = conf.getInt(s"$confRoot.expectations.percent_successful_requests")

  // this value is provided through a Java property on the command line when Gatling is run
  val baseUrl: String = conf.getString("test.base_url")

  val httpConf: HttpProtocolBuilder = http.baseURL(s"http://$baseUrl")

  val warmupScenario: ScenarioBuilder
  val mainScenario: ScenarioBuilder

  // the warm up scenario
  def populateWarmupScenario: PopulationBuilder = {
    warmupScenario
      .inject(
        constantUsersPerSec(rampUpUsers) during (rampUpDuration seconds))
      .throttle(
        jumpToRps(throughput), holdFor(warmUpDuration minutes), // warm up period
        jumpToRps(0), holdFor(duration minutes)) // stop scenario during actual test
  }

  // the main scenario
  def populateMainScenario: PopulationBuilder = {
    mainScenario
      .inject(
        nothingFor(warmUpDuration minutes), // do nothing during warm up period
        constantUsersPerSec(rampUpUsers) during (rampUpDuration seconds))
      .throttle(
        jumpToRps(throughput), holdFor((warmUpDuration + duration) minutes))
  }

  // run the scenarios
  def runScenarios(): Unit = {
    setUp(
      populateWarmupScenario,
      populateMainScenario
    ).assertions(
      global.responseTime.percentile3.lte(percentile3ResponseTimeUpperBound),
      global.successfulRequests.percent.gte(percentSuccessfulRequest)
    ).protocols(httpConf)
  }
}
