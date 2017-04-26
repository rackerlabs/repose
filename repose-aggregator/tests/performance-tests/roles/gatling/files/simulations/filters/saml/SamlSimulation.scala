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

package filters.saml

import java.io.File
import java.util.{Base64, UUID}

import com.typesafe.config._
import io.gatling.core.Predef._
import io.gatling.core.config.GatlingFiles
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

import scala.concurrent.duration._
import scala.collection.JavaConverters._

/**
  * SAML Policy Translation filter performance simulation.
  */
class SamlSimulation extends Simulation {
  import SamlSimulation._

  // properties specific to this test simulation
  val samlConf = ConfigFactory.load("saml_simulation.conf")
  val samlConfRoot = "saml_simulation"
  val samlPayloadsDir = samlConf.getString(s"$samlConfRoot.payloads_dir")
  val numOfSamlIssuers = samlConf.getInt(s"$samlConfRoot.issuers.total_number")
  val includeLegacyIssuer = samlConf.getBoolean(s"$samlConfRoot.issuers.include_legacy")
  val sendXmlBody = samlConf.getBoolean(s"$samlConfRoot.xml_body")
  val acceptMediaTypes = samlConf.getStringList(s"$samlConfRoot.accept_types").asScala.toIndexedSeq

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

  // this value is provided through a Java property on the command line when Gatling is run
  val baseUrl = conf.getString("test.base_url")

  val httpConf = http.baseURL(s"http://$baseUrl")
  val httpRequestBuilder = if (sendXmlBody) {
    postAuthRequestWithSamlResponseXmlBody
  } else {
    postAuthRequestWithSamlResponseBase64Form
  }

  val issuers = if (includeLegacyIssuer) {
    generateUniqueIssuers(numOfSamlIssuers - 1) ++ IndexedSeq(LegacySamlIssuer)
  } else {
    generateUniqueIssuers(numOfSamlIssuers)
  }

  val samlResponseFileNames = new File(GatlingFiles.bodiesDirectory.toFile, samlPayloadsDir)
    .listFiles
    .filter(_.isFile)
    .map(_.getAbsolutePath)

  // create Feeders and let Gatling randomize which ones get used for each request
  val acceptFeeder = acceptMediaTypes.map(mediaType => Map("accept" -> mediaType)).random
  val issuerFeeder = issuers.map(issuer => Map("samlIssuer" -> issuer)).random
  val fileNameFeeder = samlResponseFileNames.map(filename => Map("filename" -> filename)).random

  // set up the warm up scenario
  val warmup = scenario("Warmup")
    .feed(acceptFeeder)
    .feed(issuerFeeder)
    .feed(fileNameFeeder)
    .forever {
      exec(httpRequestBuilder)
    }
    .inject(
      constantUsersPerSec(rampUpUsers) during(rampUpDuration seconds))
    .throttle(
      jumpToRps(throughput), holdFor(warmUpDuration minutes),  // warm up period
      jumpToRps(0), holdFor(duration minutes))                 // stop scenario during actual test

  // set up the main scenario
  val mainScenario = scenario("SAML Filter Test")
    .feed(acceptFeeder)
    .feed(issuerFeeder)
    .feed(fileNameFeeder)
    .forever {
      exec(httpRequestBuilder)
    }
    .inject(
      nothingFor(warmUpDuration minutes),  // do nothing during warm up period
      constantUsersPerSec(rampUpUsers) during(rampUpDuration seconds))
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

  def postAuthRequestWithSamlResponseBase64Form: HttpRequestBuilder = {
    http(session => session.scenario)
      .post("/v2.0/RAX-AUTH/federation/saml/auth")
      .formParam("SAMLResponse", session => ElFileBody("${filename}").apply(session).map(base64Encode))
      .header(HttpHeaderNames.Accept, "${accept}")
      .header(HttpHeaderNames.ContentType, HttpHeaderValues.ApplicationFormUrlEncoded)
      .check(status.is(200))
  }

  def postAuthRequestWithSamlResponseXmlBody: HttpRequestBuilder = {
    http(session => session.scenario)
      .post("/v2.0/RAX-AUTH/federation/saml/auth")
      .body(ElFileBody("${filename}"))
      .header(HttpHeaderNames.Accept, "${accept}")
      .header(HttpHeaderNames.ContentType, HttpHeaderValues.ApplicationXml)
      .check(status.is(200))
  }
}

object SamlSimulation {
  val LegacySamlIssuer = "http://legacy.idp.external.com"
  val SamlIssuerBase = "http://idp.external.com/"

  def generateUniqueIssuers(numOfIssuers: Int): IndexedSeq[String] =
    Iterator.continually(SamlIssuerBase + UUID.randomUUID().toString).take(numOfIssuers).toVector

  def base64Encode(string: String): String = Base64.getEncoder.encodeToString(string.getBytes)
}
