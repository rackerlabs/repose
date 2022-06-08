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

package usecases.transferencoding

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder
import io.gatling.http.{HeaderNames, HeaderValues}
import org.openrepose.performance.test.AbstractReposeSimulation

import scala.util.Random

/**
  * Simple non-streaming request content performance simulation.
  */
class IdentityTransferEncodingSimulation extends AbstractReposeSimulation {

  final val BodySize: Int = 10485

  // set up the warm up scenario
  override val warmupScenario = scenario("Warmup")
    .forever() {
      exec(postRequest)
    }

  // set up the main scenario
  override val mainScenario = scenario("Identity Transfer Encoding Test")
    .forever() {
      exec(postRequest)
    }

  // run the scenarios
  runScenarios()

  // make a request with non-streamed content (using a known-size ByteArrayBody)
  def postRequest: HttpRequestBuilder = {
    http(session => session.scenario)
      .post("/transferencoding/identity")
      .body(StringBody(new Random().alphanumeric.take(BodySize).mkString))
      .header(HeaderNames.ContentType, HeaderValues.TextPlain)
      .header(HttpHeaderNames.Host, "localhost")
      .check(status.is(201))
  }
}
