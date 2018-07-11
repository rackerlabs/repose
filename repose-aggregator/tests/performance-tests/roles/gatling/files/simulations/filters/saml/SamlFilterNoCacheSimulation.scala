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
import org.openrepose.performance.test.AbstractReposeSimulation

import scala.collection.JavaConverters._

/**
  * SAML Policy Translation filter performance simulation.
  */
class SamlFilterNoCacheSimulation extends AbstractReposeSimulation {
  import SamlFilterNoCacheSimulation._

  // properties specific to this test simulation
  val samlConf = ConfigFactory.load("saml_simulation.conf")
  val samlConfRoot = "saml_simulation"
  val samlPayloadsDir = samlConf.getString(s"$samlConfRoot.payloads_dir")
  val numOfSamlIssuers = samlConf.getInt(s"$samlConfRoot.issuers.total_number")
  val includeLegacyIssuer = samlConf.getBoolean(s"$samlConfRoot.issuers.include_legacy")
  val sendXmlBody = samlConf.getBoolean(s"$samlConfRoot.xml_body")
  val acceptMediaTypes = samlConf.getStringList(s"$samlConfRoot.accept_types").asScala.toIndexedSeq

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
  override val warmupScenario = scenario("Warmup")
    .feed(acceptFeeder)
    .feed(issuerFeeder)
    .feed(fileNameFeeder)
    .forever {
      exec(httpRequestBuilder)
    }

  // set up the main scenario
  override val mainScenario = scenario("SAML Filter Test")
    .feed(acceptFeeder)
    .feed(issuerFeeder)
    .feed(fileNameFeeder)
    .forever {
      exec(httpRequestBuilder)
    }

  // run the scenarios
  runScenarios()

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

object SamlFilterNoCacheSimulation {
  val LegacySamlIssuer = "http://legacy.idp.external.com"
  val SamlIssuerBase = "http://idp.external.com/"

  def generateUniqueIssuers(numOfIssuers: Int): IndexedSeq[String] =
    Iterator.continually(SamlIssuerBase + UUID.randomUUID().toString).take(numOfIssuers).toVector

  def base64Encode(string: String): String = Base64.getEncoder.encodeToString(string.getBytes)
}
