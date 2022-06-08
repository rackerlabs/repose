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

package filters.keystonev2

import java.util.Base64

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder
import org.openrepose.performance.test.AbstractReposeSimulation

/**
  * Keystone v2 Authorization filter performance simulation.
  */
class KeystoneV2AuthorizationFilterSimulation extends AbstractReposeSimulation {
  val encodedTenantToRolesMap = Base64.getEncoder.encodeToString(
    """
      |{
      |  "defaultTenant": [
      |  ],
      |  "headerTenant": [
      |    "headerRole1",
      |    "headerRole2"
      |  ],
      |  "nonMatchingTenant": [
      |    "nonMatchingRole"
      |  ]
      |}
    """.stripMargin.getBytes)

  // set up the warm up scenario
  override val warmupScenario = scenario("Warmup")
    .forever() {
      exec(getRequest)
    }

  // set up the main scenario
  override val mainScenario = scenario("Keystone v2 Filter Test")
    .forever() {
      exec(getRequest)
    }

  // run the scenarios
  runScenarios()

  def getRequest: HttpRequestBuilder = {
    http(session => session.scenario)
      .get("/path")
      .header("x-tenant-id", "headerTenant")
      .header("x-map-roles", encodedTenantToRolesMap)
      .check(status.is(200))
  }
}
