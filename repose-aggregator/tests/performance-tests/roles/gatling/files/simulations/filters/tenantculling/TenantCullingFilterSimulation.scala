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

package filters.tenantculling

import java.util.Base64

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder
import org.openrepose.performance.test.AbstractReposeSimulation

import scala.util.Random

/**
  * Tenant Culling filter performance simulation.
  */
class TenantCullingFilterSimulation extends AbstractReposeSimulation {
  import TenantCullingFilterSimulation._

  val tenantToRolesMap = Map[String, Set[String]](
    "defaultTenant" -> Set.empty,
    "resourceTenant" -> Set("object-store:default", "compute:default"),
    "otherTenant" -> Set("other:default"),
    DomainRoleKeyName -> Set("identity:user-admin")
  )
  val tenantToRolesJson =
    "{" + tenantToRolesMap.map({ case (tenant, roles) => s""""$tenant":[${roles.map('"' + _ + '"').mkString(",")}]""" }).mkString(",") + "}"
  val encodedTenantToRolesJson = base64Encode(tenantToRolesJson)

  val feeder = Iterator.continually(Map(
    "relevantRoles" -> getRelevantRoles
  ))

  // set up the warm up scenario
  override val warmupScenario = scenario("Warmup")
    .feed(feeder)
    .forever() {
      exec(getRequest)
    }

  // set up the main scenario
  override val mainScenario = scenario("Tenant Culling Filter Test")
    .feed(feeder)
    .forever() {
      exec(getRequest)
    }

  // run the scenarios
  runScenarios()

  def getRequest: HttpRequestBuilder = {
    http(session => session.scenario)
      .get("/test")
      .header("X-Relevant-Roles", "${relevantRoles}")
      .header("X-Tenant-Id", tenantToRolesMap.keySet.-(DomainRoleKeyName).mkString(","))
      .header("X-Map-Roles", encodedTenantToRolesJson)
      .header("Mock-Origin-Res-Status", s"$ExpectedResponseStatusCode")
      .check(status.is(ExpectedResponseStatusCode))
  }

  def getRelevantRoles: String = {
    val allRoles = tenantToRolesMap.values.flatten
    Random.shuffle(allRoles).take(Random.nextInt(allRoles.size) + 1).mkString(",")
  }
}

object TenantCullingFilterSimulation {
  // This is a bogus value that wouldn't occur naturally.
  val ExpectedResponseStatusCode = 210
  val DomainRoleKeyName = "repose/domain/roles"

  def base64Encode(tenantToRolesMap: String): String = {
    Base64.getEncoder.encodeToString(tenantToRolesMap.getBytes)
  }
}
