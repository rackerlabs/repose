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

import com.typesafe.config.ConfigFactory
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

import scala.concurrent.duration._
import scala.util.Random

/**
  * Tenant Culling filter performance simulation.
  */
class TenantCullingFilterSimulation extends Simulation {
  import TenantCullingFilterSimulation._

  // properties to configure the Gatling test
  val conf = ConfigFactory.load("application.conf")
  val confRoot = "test"
  val throughput = conf.getInt(s"$confRoot.throughput")
  val duration = conf.getInt(s"$confRoot.duration")
  val warmUpDuration = conf.getInt(s"$confRoot.warmup_duration")
  val rampUpUsers = conf.getInt(s"$confRoot.ramp_up_users.new_per_sec")
  val rampUpDuration = conf.getInt(s"$confRoot.ramp_up_users.duration_in_sec")
  val percentile3ResponseTimeUpperBound = conf.getInt(s"$confRoot.expectations.percentile3_response_time_upper_bound")
  val percentSuccessfulRequest = conf.getInt(s"$confRoot.expectations.percent_successful_requests")
  val tenantToRolesMap = Map(
    "defaultTenant" -> Set.empty,
    "resourceTenant" -> Set("object-store:default", "compute:default"),
    "otherTenant" -> Set("other:default"),
    DomainRoleKeyName -> Set("identity:user-admin")
  )
  val tenantToRolesJson =
    "{" + tenantToRolesMap.map({ case tenant -> roles => s""""$tenant":[${roles.map('"' + _ + '"').mkString(",")}]""" }).mkString(",") + "}"
  val encodedTenantToRolesJson = base64Encode(tenantToRolesJson)

  // this value is provided through a Java property on the command line when Gatling is run
  val baseUrl = conf.getString("test.base_url")

  val httpConf = http.baseURL(s"http://$baseUrl")

  val feeder = Iterator.continually(Map(
    "relevantRoles" -> getRelevantRoles
  ))

  // set up the warm up scenario
  val warmup = scenario("Warmup")
    .feed(feeder)
    .forever() {
      exec(getRequest)
    }
    .inject(
      constantUsersPerSec(rampUpUsers) during (rampUpDuration seconds))
    .throttle(
      jumpToRps(throughput), holdFor(warmUpDuration minutes), // warm up period
      jumpToRps(0), holdFor(duration minutes)) // stop scenario during actual test

  // set up the main scenario
  val mainScenario = scenario("Tenant Culling Filter Test")
    .feed(feeder)
    .forever() {
      exec(getRequest)
    }
    .inject(
      nothingFor(warmUpDuration minutes), // do nothing during warm up period
      constantUsersPerSec(rampUpUsers) during (rampUpDuration seconds))
    .throttle(
      jumpToRps(throughput), holdFor((warmUpDuration + duration) minutes))

  // run the scenarios
  setUp(
    warmup,
    mainScenario
  ).assertions(
    global.responseTime.percentile3.lte(percentile3ResponseTimeUpperBound),
    global.successfulRequests.percent.gte(percentSuccessfulRequest)
  ).protocols(httpConf)

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
    Base64.encoder.encodeToString(tenantToRolesMap.bytes)
  }
}
