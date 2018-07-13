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

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder
import org.openrepose.performance.test.AbstractReposeSimulation

import scala.util.Random

/**
  * Complex use case performance simulation.
  */
class ComplexUseCaseSimulation extends AbstractReposeSimulation {
  val feeder = Iterator.continually(Map("authToken" -> Random.alphanumeric.take(24).mkString))

  // set up the warm up scenario
  override val warmupScenario = scenario("Warmup")
    .feed(feeder)
    .forever() {
      exec(getUsers)
    }

  // set up the main scenario
  override val mainScenario = scenario("Complex Use Case Test")
    .feed(feeder)
    .forever() {
      exec(getUsers)
    }

  // run the scenarios
  runScenarios()

  def getUsers: HttpRequestBuilder = {
    http(session => session.scenario)
      .get("/functest1/users/")
      .queryParam("limit", "1000")
      .queryParam("includeLocked", "true")
      .header("x-auth-token", "${authToken}")

      .check(status.is(200))
  }
}
