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
package filters.splitheader

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder
import org.openrepose.performance.test.AbstractReposeSimulation

/**
  * Split Header filter performance simulation.
  */
class SplitHeaderFilterSimulation extends AbstractReposeSimulation {
  // set up the warm up scenario
  override val warmupScenario = scenario("Warmup")
    .forever() {
      exec(getRequest)
    }

  // set up the main scenario
  override val mainScenario = scenario("Split Header Filter Test")
    .forever() {
      exec(getRequest)
    }

  // run the scenarios
  runScenarios()

  def getRequest: HttpRequestBuilder = {
    http(session => session.scenario)
      .get("/")
      .header("request-test-1", "one,two,three")
      .header("request-test-1", "four,  five,  six  ")
      .header("request-test-1", "seven")
      .header("ReQuEsT-TeSt-2", "one;two=2;three=3")
      .header("REQUEST-TEST-3", "one,two,three")
      .check(status.is(200))
  }
}
