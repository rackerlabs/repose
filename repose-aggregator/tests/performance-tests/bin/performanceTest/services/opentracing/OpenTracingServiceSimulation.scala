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

package services.opentracing

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder
import org.openrepose.performance.test.AbstractReposeSimulation

import scala.util.Random

/**
  * Simple opentracing performance simulation (with keystone in the middle).
  */
class OpenTracingServiceSimulation extends AbstractReposeSimulation {
  val feeder = Iterator.continually(Map("authToken" -> Random.alphanumeric.take(250).mkString))

  // set up the warm up scenario
  override val warmupScenario = scenario("Warmup")
    .feed(feeder)
    .forever() {
      exec(getRequest)
    }

  // set up the main scenario
  override val mainScenario = scenario("OpenTracing Service Test")
    .feed(feeder)
    .forever() {
      exec(getRequest)
    }

  // run the scenarios
  runScenarios()

  def getRequest: HttpRequestBuilder = {
    http(session => session.scenario)
      .get("/")
      .header("x-auth-token", "${authToken}")
      .check(status.is(200))
  }
}
