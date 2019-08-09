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
package usecases.passthrough

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder
import org.openrepose.performance.test.AbstractReposeSimulation

/**
  * Pass-through use case performance simulation.
  */
class PassthroughUseCaseSimulation extends AbstractReposeSimulation {
  // Set up the warm up scenario
  override val warmupScenario = scenario("Warmup")
    .forever() {
      exec(getRequest)
      exec(postRequest)
    }

  // Set up the main scenario
  override val mainScenario = scenario("Simple Use Case Test")
    .forever() {
      exec(getRequest)
      exec(postRequest)
    }

  // Run the scenarios
  runScenarios()

  def getRequest: HttpRequestBuilder = {
    http(session => session.scenario)
      .get("/use-case/passthrough")
      .header(HttpHeaderNames.Host, "localhost")
      .check(status.is(200))
  }

  def postRequest: HttpRequestBuilder = {
    http(session => session.scenario)
      .post("/use-case/passthrough")
      .body(StringBody("""<root attr="attr-value"/>"""))
      .asXML
      .header(HttpHeaderNames.Host, "localhost")
      .check(status.is(201))
  }
}
