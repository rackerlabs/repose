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

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder
import org.openrepose.performance.test.AbstractReposeSimulation

import scala.util.Random

/**
  * Valkyrie filter performance simulation.
  */
class ValkyrieFilterSimulation extends AbstractReposeSimulation {
  import ValkyrieFilterSimulation._

  val feeder = Iterator.continually(Map(
    "tenantId" -> s"hybrid:${Random.numeric.take(8).mkString}",
    "authToken" -> Random.alphanumeric.take(10).mkString,
    "authUser" -> Random.numeric.take(8).mkString,
    "contactId" -> Random.numeric.take(8).mkString
  ))

  // set up the warm up scenario
  override val warmupScenario = scenario("Warmup")
    .feed(feeder)
    .forever() {
      exec(getResourceWithDeviceId)
    }

  // set up the main scenario
  override val mainScenario = scenario("Valkyrie Filter Test")
    .feed(feeder)
    .forever() {
      exec(getResourceWithDeviceId)
    }

  // run the scenarios
  runScenarios()

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

object ValkyrieFilterSimulation {
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
