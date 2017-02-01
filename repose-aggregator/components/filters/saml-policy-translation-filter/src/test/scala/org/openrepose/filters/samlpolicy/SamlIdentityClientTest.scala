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
package org.openrepose.filters.samlpolicy

import java.io.ByteArrayInputStream
import java.nio.charset.{Charset, StandardCharsets}
import javax.servlet.http.HttpServletResponse._
import javax.ws.rs.core.MediaType

import org.apache.http.message.BasicHeader
import org.hamcrest.{Matchers => HM}
import org.junit.runner.RunWith
import org.mockito.Mockito.{never, verify, when}
import org.mockito.{Matchers => MM}
import org.openrepose.commons.utils.http.CommonHttpHeader.{CONTENT_TYPE, TRACE_GUID}
import org.openrepose.commons.utils.http.{CommonHttpHeader, ServiceClientResponse}
import org.openrepose.core.services.serviceclient.akka.{AkkaServiceClient, AkkaServiceClientFactory}
import org.openrepose.filters.samlpolicy.SamlIdentityClient.{OverLimitException, SC_TOO_MANY_REQUESTS}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import play.api.libs.json.Json

import scala.language.implicitConversions
import scala.util.{Failure, Success}

@RunWith(classOf[JUnitRunner])
class SamlIdentityClientTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {

  final val TokenServiceClientId = "token-service-client"
  final val PolicyServiceClientId = "policy-service-client"

  var defaultServiceClient: AkkaServiceClient = _
  var tokenServiceClient: AkkaServiceClient = _
  var policyServiceClient: AkkaServiceClient = _
  var akkaServiceClientFactory: AkkaServiceClientFactory = _
  var samlPolicyProvider: SamlIdentityClient = _

  override def beforeEach(): Unit = {
    defaultServiceClient = mock[AkkaServiceClient]
    tokenServiceClient = mock[AkkaServiceClient]
    policyServiceClient = mock[AkkaServiceClient]
    akkaServiceClientFactory = mock[AkkaServiceClientFactory]

    samlPolicyProvider = new SamlIdentityClient(akkaServiceClientFactory)

    when(akkaServiceClientFactory.newAkkaServiceClient())
      .thenReturn(defaultServiceClient)
    when(akkaServiceClientFactory.newAkkaServiceClient(MM.anyString()))
      .thenReturn(defaultServiceClient)
    when(akkaServiceClientFactory.newAkkaServiceClient(MM.eq(TokenServiceClientId)))
      .thenReturn(tokenServiceClient)
    when(akkaServiceClientFactory.newAkkaServiceClient(MM.eq(PolicyServiceClientId)))
      .thenReturn(policyServiceClient)
  }

  describe("using") {
    it("should build a new service client when the token connection pool ID changes") {
      samlPolicyProvider.using("tokenUri", "policyUri", Some("foo"), Some("baz"))
      samlPolicyProvider.using("tokenUri", "policyUri", Some("bar"), Some("baz"))

      verify(akkaServiceClientFactory).newAkkaServiceClient("foo")
      verify(defaultServiceClient).destroy()
      verify(akkaServiceClientFactory).newAkkaServiceClient("bar")
    }

    it("should build a new service client when the token connection pool ID changes from None") {
      samlPolicyProvider.using("tokenUri", "policyUri", None, Some("baz"))
      samlPolicyProvider.using("tokenUri", "policyUri", Some("bar"), Some("baz"))

      verify(akkaServiceClientFactory).newAkkaServiceClient()
      verify(defaultServiceClient).destroy()
      verify(akkaServiceClientFactory).newAkkaServiceClient("bar")
    }

    it("should build a new service client when the token connection pool ID changes to None") {
      samlPolicyProvider.using("tokenUri", "policyUri", Some("foo"), Some("baz"))
      samlPolicyProvider.using("tokenUri", "policyUri", None, Some("baz"))

      verify(akkaServiceClientFactory).newAkkaServiceClient("foo")
      verify(defaultServiceClient).destroy()
      verify(akkaServiceClientFactory).newAkkaServiceClient()
    }

    it("should not build a new service client if the token connection pool ID does not change") {
      samlPolicyProvider.using("tokenUri", "policyUri", Some("foo"), Some("baz"))
      samlPolicyProvider.using("tokenUri", "policyUri", Some("foo"), Some("baz"))

      verify(akkaServiceClientFactory).newAkkaServiceClient("foo")
      verify(defaultServiceClient, never).destroy()
    }

    it("should not build a new service client if the token connection pool ID does not change from/to null") {
      samlPolicyProvider.using("tokenUri", "policyUri", None, Some("baz"))
      samlPolicyProvider.using("tokenUri", "policyUri", None, Some("baz"))

      verify(akkaServiceClientFactory).newAkkaServiceClient()
      verify(defaultServiceClient, never).destroy()
    }

    it("should build a new service client when the policy connection pool ID changes") {
      samlPolicyProvider.using("tokenUri", "policyUri", Some("baz"), Some("foo"))
      samlPolicyProvider.using("tokenUri", "policyUri", Some("baz"), Some("bar"))

      verify(akkaServiceClientFactory).newAkkaServiceClient("foo")
      verify(defaultServiceClient).destroy()
      verify(akkaServiceClientFactory).newAkkaServiceClient("bar")
    }

    it("should build a new service client when the policy connection pool ID changes from None") {
      samlPolicyProvider.using("tokenUri", "policyUri", Some("baz"), None)
      samlPolicyProvider.using("tokenUri", "policyUri", Some("baz"), Some("bar"))

      verify(akkaServiceClientFactory).newAkkaServiceClient()
      verify(defaultServiceClient).destroy()
      verify(akkaServiceClientFactory).newAkkaServiceClient("bar")
    }

    it("should build a new service client when the policy connection pool ID changes to None") {
      samlPolicyProvider.using("tokenUri", "policyUri", Some("baz"), Some("foo"))
      samlPolicyProvider.using("tokenUri", "policyUri", Some("baz"), None)

      verify(akkaServiceClientFactory).newAkkaServiceClient("foo")
      verify(defaultServiceClient).destroy()
      verify(akkaServiceClientFactory).newAkkaServiceClient()
    }

    it("should not build a new service client if the policy connection pool ID does not change") {
      samlPolicyProvider.using("tokenUri", "policyUri", Some("baz"), Some("foo"))
      samlPolicyProvider.using("tokenUri", "policyUri", Some("baz"), Some("foo"))

      verify(akkaServiceClientFactory).newAkkaServiceClient("foo")
      verify(defaultServiceClient, never).destroy()
    }

    it("should not build a new service client if the policy connection pool ID does not change from/to null") {
      samlPolicyProvider.using("tokenUri", "policyUri", Some("baz"), None)
      samlPolicyProvider.using("tokenUri", "policyUri", Some("baz"), None)

      verify(akkaServiceClientFactory).newAkkaServiceClient()
      verify(defaultServiceClient, never).destroy()
    }
  }

  describe("getToken") {
    val sampleToken =
      """
        |{
        |  "access": {
        |    "token": {
        |      "id": "some-token"
        |    }
        |  }
        |}
      """.stripMargin

    it("should return a Failure if the service client cannot connect") {
      when(tokenServiceClient.post(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.anyString(),
        MM.any[MediaType]
      )).thenThrow(new RuntimeException("Could not connect"))

      samlPolicyProvider.using("", "", Some(TokenServiceClientId), None)

      val result = samlPolicyProvider.getToken("username", "password", None)

      result shouldBe a[Failure[_]]
    }

    it("should forward a trace ID if provided") {
      when(tokenServiceClient.post(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.anyString(),
        MM.any[MediaType]
      )).thenReturn(new ServiceClientResponse(SC_OK, null))

      samlPolicyProvider.using("", "", Some(TokenServiceClientId), None)

      samlPolicyProvider.getToken("username", "password", Some("trace-id"))

      verify(tokenServiceClient).post(
        MM.anyString(),
        MM.anyString(),
        MM.argThat(HM.hasEntry(TRACE_GUID.toString, "trace-id")),
        MM.anyString(),
        MM.any[MediaType]
      )
    }

    it("should not forward a trace ID if not provided") {
      when(tokenServiceClient.post(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.anyString(),
        MM.any[MediaType]
      )).thenReturn(new ServiceClientResponse(SC_OK, null))

      samlPolicyProvider.using("", "", Some(TokenServiceClientId), None)

      samlPolicyProvider.getToken("username", "password", None)

      verify(tokenServiceClient).post(
        MM.anyString(),
        MM.anyString(),
        MM.argThat(HM.not(HM.hasKey(TRACE_GUID.toString))),
        MM.anyString(),
        MM.any[MediaType]
      )
    }

    it("should return a Failure if the response cannot be parsed") {
      when(tokenServiceClient.post(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.anyString(),
        MM.any[MediaType]
      )).thenReturn(new ServiceClientResponse(
        SC_OK,
        Array(new BasicHeader(
          CONTENT_TYPE.toString,
          MediaType.APPLICATION_JSON + "; charset=" + Charset.defaultCharset().name()
        )),
        new ByteArrayInputStream(
          """
            |{
            |}
          """.stripMargin.getBytes
        )
      ))

      samlPolicyProvider.using("", "", Some(TokenServiceClientId), None)

      val result = samlPolicyProvider.getToken("username", "password", None)

      result shouldBe a[Failure[_]]
    }

    Set(
      SC_REQUEST_ENTITY_TOO_LARGE,
      SC_TOO_MANY_REQUESTS
    ) foreach { statusCode =>
      it(s"should return a Failure if the request is rate limited with a $statusCode") {
        when(tokenServiceClient.post(
          MM.anyString(),
          MM.anyString(),
          MM.anyMapOf(classOf[String], classOf[String]),
          MM.anyString(),
          MM.any[MediaType]
        )).thenReturn(new ServiceClientResponse(statusCode, null))

        samlPolicyProvider.using("", "", Some(TokenServiceClientId), None)

        val result = samlPolicyProvider.getToken("username", "password", None)

        result shouldBe a[Failure[_]]
        an [OverLimitException] should be thrownBy result.get
      }
    }

    it("should return a Success if the response is a 200") {
      when(tokenServiceClient.post(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.anyString(),
        MM.any[MediaType]
      )).thenReturn(new ServiceClientResponse(
        SC_OK,
        Array(new BasicHeader(
          CONTENT_TYPE.toString,
          MediaType.APPLICATION_JSON + "; charset=" + Charset.defaultCharset().name()
        )),
        new ByteArrayInputStream(sampleToken.getBytes)
      ))

      samlPolicyProvider.using("", "", Some(TokenServiceClientId), None)

      val result = samlPolicyProvider.getToken("username", "password", None)

      result shouldBe a[Success[_]]
      result.get shouldEqual "some-token"
    }

    Set(
      SC_CREATED,
      SC_ACCEPTED,
      SC_NO_CONTENT,
      SC_MOVED_PERMANENTLY,
      SC_MOVED_TEMPORARILY,
      SC_FOUND,
      SC_BAD_REQUEST,
      SC_UNAUTHORIZED,
      SC_FORBIDDEN,
      SC_NOT_FOUND,
      SC_METHOD_NOT_ALLOWED,
      SC_REQUEST_ENTITY_TOO_LARGE,
      SC_TOO_MANY_REQUESTS,
      SC_INTERNAL_SERVER_ERROR,
      SC_SERVICE_UNAVAILABLE
    ) foreach { responseCode =>
      it(s"should return a Failure if the response is a $responseCode") {
        when(tokenServiceClient.post(
          MM.anyString(),
          MM.anyString(),
          MM.anyMapOf(classOf[String], classOf[String]),
          MM.anyString(),
          MM.any[MediaType]
        )).thenReturn(new ServiceClientResponse(responseCode, null))

        samlPolicyProvider.using("", "", Some(TokenServiceClientId), None)

        val result = samlPolicyProvider.getToken("username", "password", None)

        result shouldBe a[Failure[_]]
      }
    }

    Set(
      StandardCharsets.ISO_8859_1,
      StandardCharsets.US_ASCII,
      StandardCharsets.UTF_8,
      StandardCharsets.UTF_16
    ) foreach { charset =>
      it(s"should handle $charset response encoding") {
        when(tokenServiceClient.post(
          MM.anyString(),
          MM.anyString(),
          MM.anyMapOf(classOf[String], classOf[String]),
          MM.anyString(),
          MM.any[MediaType]
        )).thenReturn(new ServiceClientResponse(
          SC_OK,
          Array(new BasicHeader(
            CONTENT_TYPE.toString,
            MediaType.APPLICATION_JSON + "; charset=" + charset.name()
          )),
          new ByteArrayInputStream(sampleToken.getBytes(charset))
        ))

        samlPolicyProvider.using("", "", Some(TokenServiceClientId), None)

        val result = samlPolicyProvider.getToken("username", "password", None)

        result shouldBe a[Success[_]]
        result.get shouldEqual "some-token"
      }
    }

    it("should always check the HTTP request cache") {
      when(tokenServiceClient.post(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.anyString(),
        MM.any[MediaType]
      )).thenReturn(new ServiceClientResponse(
        SC_OK,
        Array(new BasicHeader(
          CONTENT_TYPE.toString,
          MediaType.APPLICATION_JSON + "; charset=" + Charset.defaultCharset().name()
        )),
        new ByteArrayInputStream(sampleToken.getBytes)
      ))

      samlPolicyProvider.using("", "", Some(TokenServiceClientId), None)

      samlPolicyProvider.getToken("username", "password", None)

      verify(tokenServiceClient).post(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.anyString(),
        MM.any[MediaType]
      )
    }
  }

  describe("getIdpId") {
    val sampleIdpId = "508daa5d406d41639c67860f25db29df"
    val sampleIdp =
      s"""
        |{
        |  "RAX-AUTH:identityProviders": [{
        |    "name": "demo",
        |	   "federationType": "domain",
        |	   "approvedDomains": ["77366"],
        |	   "description": "a description",
        |	   "id": "$sampleIdpId",
        |	   "issuer": "https://demo.issuer.com"
        |  }]
        |}
      """.stripMargin

    it("should return a Failure if the service client cannot connect") {
      when(policyServiceClient.get(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.anyBoolean()
      )).thenThrow(new RuntimeException("Could not connect"))

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      val result = samlPolicyProvider.getIdpId("issuer", "token", None, checkCache = true)

      result shouldBe a[Failure[_]]
    }

    it("should forward a trace ID if provided") {
      when(policyServiceClient.get(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.anyBoolean()
      )).thenReturn(new ServiceClientResponse(SC_OK, null))

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      samlPolicyProvider.getIdpId("issuer", "token", Some("trace-id"), checkCache = true)

      verify(policyServiceClient).get(
        MM.anyString(),
        MM.anyString(),
        MM.argThat(HM.hasEntry(TRACE_GUID.toString, "trace-id")),
        MM.anyBoolean()
      )
    }

    it("should not forward a trace ID if not provided") {
      when(policyServiceClient.get(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.anyBoolean()
      )).thenReturn(new ServiceClientResponse(SC_OK, null))

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      samlPolicyProvider.getIdpId("issuer", "token", None, checkCache = true)

      verify(policyServiceClient).get(
        MM.anyString(),
        MM.anyString(),
        MM.argThat(HM.not(HM.hasKey(TRACE_GUID.toString))),
        MM.anyBoolean()
      )
    }

    it("should forward the provided token as a header") {
      val token = "a-unique-token"

      when(policyServiceClient.get(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.anyBoolean()
      )).thenReturn(new ServiceClientResponse(SC_OK, null))

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      samlPolicyProvider.getIdpId("issuer", token, Some("trace-id"), checkCache = true)

      verify(policyServiceClient).get(
        MM.anyString(),
        MM.anyString(),
        MM.argThat(HM.hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, token)),
        MM.anyBoolean()
      )
    }

    it("should return a Failure if the response cannot be parsed") {
      when(policyServiceClient.get(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.anyBoolean()
      )).thenReturn(new ServiceClientResponse(
        SC_OK,
        Array(new BasicHeader(
          CONTENT_TYPE.toString,
          MediaType.APPLICATION_JSON + "; charset=" + Charset.defaultCharset().name()
        )),
        new ByteArrayInputStream(
          """
            |{
            |}
          """.stripMargin.getBytes
        )
      ))

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      val result = samlPolicyProvider.getIdpId("issuer", "token", None, checkCache = true)

      result shouldBe a[Failure[_]]
    }

    Set(
      SC_REQUEST_ENTITY_TOO_LARGE,
      SC_TOO_MANY_REQUESTS
    ) foreach { statusCode =>
      it(s"should return a Failure if the request is rate limited with a $statusCode") {
        when(policyServiceClient.get(
          MM.anyString(),
          MM.anyString(),
          MM.anyMapOf(classOf[String], classOf[String]),
          MM.anyBoolean()
        )).thenReturn(new ServiceClientResponse(statusCode, null))

        samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

        val result = samlPolicyProvider.getIdpId("issuer", "token", None, checkCache = true)

        result shouldBe a[Failure[_]]
        an [OverLimitException] should be thrownBy result.get
      }
    }

    it("should return a Success if the response is a 200") {
      when(policyServiceClient.get(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.anyBoolean()
      )).thenReturn(new ServiceClientResponse(
        SC_OK,
        Array(new BasicHeader(
          CONTENT_TYPE.toString,
          MediaType.APPLICATION_JSON + "; charset=" + Charset.defaultCharset().name()
        )),
        new ByteArrayInputStream(sampleIdp.getBytes)
      ))

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      val result = samlPolicyProvider.getIdpId("issuer", "token", None, checkCache = true)

      result shouldBe a[Success[_]]
      result.get shouldEqual sampleIdpId
    }

    Set(
      SC_CREATED,
      SC_ACCEPTED,
      SC_NO_CONTENT,
      SC_MOVED_PERMANENTLY,
      SC_MOVED_TEMPORARILY,
      SC_FOUND,
      SC_BAD_REQUEST,
      SC_UNAUTHORIZED,
      SC_FORBIDDEN,
      SC_NOT_FOUND,
      SC_METHOD_NOT_ALLOWED,
      SC_REQUEST_ENTITY_TOO_LARGE,
      SC_TOO_MANY_REQUESTS,
      SC_INTERNAL_SERVER_ERROR,
      SC_SERVICE_UNAVAILABLE
    ) foreach { responseCode =>
      it(s"should return a Failure if the response is a $responseCode") {
        when(policyServiceClient.get(
          MM.anyString(),
          MM.anyString(),
          MM.anyMapOf(classOf[String], classOf[String]),
          MM.anyBoolean()
        )).thenReturn(new ServiceClientResponse(responseCode, null))

        samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

        val result = samlPolicyProvider.getIdpId("issuer", "token", None, checkCache = true)

        result shouldBe a[Failure[_]]
      }
    }

    Set(
      StandardCharsets.ISO_8859_1,
      StandardCharsets.US_ASCII,
      StandardCharsets.UTF_8,
      StandardCharsets.UTF_16
    ) foreach { charset =>
      it(s"should handle $charset response encoding") {
        when(policyServiceClient.get(
          MM.anyString(),
          MM.anyString(),
          MM.anyMapOf(classOf[String], classOf[String]),
          MM.anyBoolean()
        )).thenReturn(new ServiceClientResponse(
          SC_OK,
          Array(new BasicHeader(
            CONTENT_TYPE.toString,
            MediaType.APPLICATION_JSON + "; charset=" + charset.name
          )),
          new ByteArrayInputStream(sampleIdp.getBytes(charset))
        ))

        samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

        val result = samlPolicyProvider.getIdpId("issuer", "token", None, checkCache = true)

        result shouldBe a[Success[_]]
        result.get shouldEqual sampleIdpId
      }
    }

    it("should check the HTTP request cache if not retrying") {
      when(policyServiceClient.get(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.anyBoolean()
      )).thenReturn(new ServiceClientResponse(
        SC_OK,
        Array(new BasicHeader(
          CONTENT_TYPE.toString,
          MediaType.APPLICATION_JSON + "; charset=" + Charset.defaultCharset().name()
        )),
        new ByteArrayInputStream(sampleIdp.getBytes)
      ))

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      samlPolicyProvider.getIdpId("issuer", "token", None, checkCache = true)

      verify(policyServiceClient).get(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.eq(true)
      )
    }

    it("should not check the HTTP request cache if retrying") {
      when(policyServiceClient.get(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.anyBoolean()
      )).thenReturn(new ServiceClientResponse(
        SC_OK,
        Array(new BasicHeader(
          CONTENT_TYPE.toString,
          MediaType.APPLICATION_JSON + "; charset=" + Charset.defaultCharset().name()
        )),
        new ByteArrayInputStream(sampleIdp.getBytes)
      ))

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      samlPolicyProvider.getIdpId("issuer", "token", None, checkCache = false)

      verify(policyServiceClient).get(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.eq(false)
      )
    }
  }

  describe("getPolicy") {
    val samplePolicy =
      """
        |{
        |  "mapping" : {
        |    "version" : "RAX-1",
        |    "description" : "Default mapping policy",
        |    "rules": [
        |      {
        |        "local": {
        |          "user": {
        |            "domain":"{D}",
        |            "name":"{D}",
        |            "email":"{D}",
        |            "roles":"{D}",
        |            "expire":"{D}"
        |          }
        |        }
        |      }
        |    ]
        |  }
        |}
      """.stripMargin

    it("should return a Failure if the service client cannot connect") {
      when(policyServiceClient.get(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.anyBoolean()
      )).thenThrow(new RuntimeException("Could not connect"))

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      val result = samlPolicyProvider.getPolicy("idpId", "token", None, checkCache = true)

      result shouldBe a[Failure[_]]
    }

    it("should forward a trace ID if provided") {
      when(policyServiceClient.get(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.anyBoolean()
      )).thenReturn(new ServiceClientResponse(SC_OK, null))

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      samlPolicyProvider.getPolicy("idpId", "token", Some("trace-id"), checkCache = true)

      verify(policyServiceClient).get(
        MM.anyString(),
        MM.anyString(),
        MM.argThat(HM.hasEntry(TRACE_GUID.toString, "trace-id")),
        MM.anyBoolean()
      )
    }

    it("should not forward a trace ID if not provided") {
      when(policyServiceClient.get(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.anyBoolean()
      )).thenReturn(new ServiceClientResponse(SC_OK, null))

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      samlPolicyProvider.getPolicy("idpId", "token", None, checkCache = true)

      verify(policyServiceClient).get(
        MM.anyString(),
        MM.anyString(),
        MM.argThat(HM.not(HM.hasKey(TRACE_GUID))),
        MM.anyBoolean()
      )
    }

    it("should forward the provided token as a header") {
      val token = "a-unique-token"

      when(policyServiceClient.get(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.anyBoolean()
      )).thenReturn(new ServiceClientResponse(SC_OK, null))

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      samlPolicyProvider.getPolicy("idpId", token, Some("trace-id"), checkCache = true)

      verify(policyServiceClient).get(
        MM.anyString(),
        MM.anyString(),
        MM.argThat(HM.hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, token)),
        MM.anyBoolean()
      )
    }

    Set(
      SC_REQUEST_ENTITY_TOO_LARGE,
      SC_TOO_MANY_REQUESTS
    ) foreach { statusCode =>
      it(s"should return a Failure if the request is rate limited with a $statusCode") {
        when(policyServiceClient.get(
          MM.anyString(),
          MM.anyString(),
          MM.anyMapOf(classOf[String], classOf[String]),
          MM.anyBoolean()
        )).thenReturn(new ServiceClientResponse(statusCode, null))

        samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

        val result = samlPolicyProvider.getPolicy("idpId", "token", None, checkCache = true)

        result shouldBe a[Failure[_]]
        an [OverLimitException] should be thrownBy result.get
      }
    }

    it("should return a Success if the response is a 200") {
      when(policyServiceClient.get(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.anyBoolean()
      )).thenReturn(new ServiceClientResponse(
        SC_OK,
        Array(new BasicHeader(
          CONTENT_TYPE.toString,
          MediaType.APPLICATION_JSON + "; charset=" + Charset.defaultCharset().name()
        )),
        new ByteArrayInputStream(samplePolicy.getBytes)
      ))

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      val result = samlPolicyProvider.getPolicy("idpId", "token", None, checkCache = true)

      result shouldBe a[Success[_]]
      Json.stringify(Json.parse(result.get)) shouldEqual Json.stringify(Json.parse(samplePolicy))
    }

    Set(
      SC_CREATED,
      SC_ACCEPTED,
      SC_NO_CONTENT,
      SC_MOVED_PERMANENTLY,
      SC_MOVED_TEMPORARILY,
      SC_FOUND,
      SC_BAD_REQUEST,
      SC_UNAUTHORIZED,
      SC_FORBIDDEN,
      SC_NOT_FOUND,
      SC_METHOD_NOT_ALLOWED,
      SC_REQUEST_ENTITY_TOO_LARGE,
      SC_TOO_MANY_REQUESTS,
      SC_INTERNAL_SERVER_ERROR,
      SC_SERVICE_UNAVAILABLE
    ) foreach { responseCode =>
      it(s"should return a Failure if the response is a $responseCode") {
        when(policyServiceClient.get(
          MM.anyString(),
          MM.anyString(),
          MM.anyMapOf(classOf[String], classOf[String]),
          MM.anyBoolean()
        )).thenReturn(new ServiceClientResponse(responseCode, null))

        samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

        val result = samlPolicyProvider.getPolicy("idpId", "token", None, checkCache = true)

        result shouldBe a[Failure[_]]
      }
    }

    Set(
      StandardCharsets.ISO_8859_1,
      StandardCharsets.US_ASCII,
      StandardCharsets.UTF_8,
      StandardCharsets.UTF_16
    ) foreach { charset =>
      it(s"should handle $charset response encoding") {
        when(policyServiceClient.get(
          MM.anyString(),
          MM.anyString(),
          MM.anyMapOf(classOf[String], classOf[String]),
          MM.anyBoolean()
        )).thenReturn(new ServiceClientResponse(
          SC_OK,
          Array(new BasicHeader(
            CONTENT_TYPE.toString,
            MediaType.APPLICATION_JSON + "; charset=" + charset.name
          )),
          new ByteArrayInputStream(samplePolicy.getBytes(charset))
        ))

        samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

        val result = samlPolicyProvider.getPolicy("idpId", "token", None, checkCache = true)

        result shouldBe a[Success[_]]
        Json.stringify(Json.parse(result.get)) shouldEqual Json.stringify(Json.parse(samplePolicy))
      }
    }

    it("should check the HTTP request cache if not retrying") {
      when(policyServiceClient.get(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.anyBoolean()
      )).thenReturn(new ServiceClientResponse(
        SC_OK,
        Array(new BasicHeader(
          CONTENT_TYPE.toString,
          MediaType.APPLICATION_JSON + "; charset=" + Charset.defaultCharset().name()
        )),
        new ByteArrayInputStream(samplePolicy.getBytes)
      ))

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      samlPolicyProvider.getPolicy("idpId", "token", None, checkCache = true)

      verify(policyServiceClient).get(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.eq(true)
      )
    }

    it("should not check the HTTP request cache if retrying") {
      when(policyServiceClient.get(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.anyBoolean()
      )).thenReturn(new ServiceClientResponse(
        SC_OK,
        Array(new BasicHeader(
          CONTENT_TYPE.toString,
          MediaType.APPLICATION_JSON + "; charset=" + Charset.defaultCharset().name()
        )),
        new ByteArrayInputStream(samplePolicy.getBytes)
      ))

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      samlPolicyProvider.getPolicy("idpId", "token", None, checkCache = false)

      verify(policyServiceClient).get(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.eq(false)
      )
    }
  }

  implicit def looseToStrictStringMap(sm: java.util.Map[_, _]): java.util.Map[String, String] =
    sm.asInstanceOf[java.util.Map[String, String]]
}
