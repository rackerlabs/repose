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

import java.nio.charset.{Charset, StandardCharsets}

import javax.servlet.http.HttpServletResponse._
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.{CloseableHttpResponse, HttpGet, HttpPost, HttpUriRequest}
import org.apache.http.entity.ContentType
import org.apache.http.message.BasicHttpResponse
import org.apache.http.protocol.HttpContext
import org.apache.http.{HttpEntity, HttpVersion}
import org.junit.runner.RunWith
import org.mockito.Mockito.{verify, when}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.mockito.{ArgumentCaptor, Matchers => MM}
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.commons.utils.http.CommonHttpHeader.TRACE_GUID
import org.openrepose.core.services.httpclient.{CachingHttpClientContext, HttpClientService, HttpClientServiceClient}
import org.openrepose.filters.samlpolicy.SamlIdentityClient.{OverLimitException, ProviderInfo, SC_TOO_MANY_REQUESTS}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.web.util.UriUtils
import play.api.libs.json.Json

import scala.Function.tupled
import scala.language.implicitConversions
import scala.util.{Failure, Success}

@RunWith(classOf[JUnitRunner])
class SamlIdentityClientTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {

  final val TokenServiceClientId = "token-service-client"
  final val PolicyServiceClientId = "policy-service-client"

  var defaultHttpClient: HttpClientServiceClient = _
  var tokenHttpClient: HttpClientServiceClient = _
  var policyHttpClient: HttpClientServiceClient = _
  var httpClientService: HttpClientService = _
  var samlPolicyProvider: SamlIdentityClient = _

  override def beforeEach(): Unit = {
    defaultHttpClient = mock[HttpClientServiceClient]
    tokenHttpClient = mock[HttpClientServiceClient]
    policyHttpClient = mock[HttpClientServiceClient]
    httpClientService = mock[HttpClientService]

    samlPolicyProvider = new SamlIdentityClient(httpClientService)

    when(httpClientService.getDefaultClient)
      .thenReturn(defaultHttpClient)
    when(httpClientService.getClient(MM.anyString()))
      .thenReturn(defaultHttpClient)
    when(httpClientService.getClient(MM.eq(TokenServiceClientId)))
      .thenReturn(tokenHttpClient)
    when(httpClientService.getClient(MM.eq(PolicyServiceClientId)))
      .thenReturn(policyHttpClient)
  }

  describe("using") {
    it("should build a new service client when the token connection pool ID changes") {
      samlPolicyProvider.using("tokenUri", "policyUri", Some("foo"), Some("baz"))
      samlPolicyProvider.using("tokenUri", "policyUri", Some("bar"), Some("baz"))

      verify(httpClientService).getClient("foo")
      verify(httpClientService).getClient("bar")
    }

    it("should build a new service client when the token connection pool ID changes from None") {
      samlPolicyProvider.using("tokenUri", "policyUri", None, Some("baz"))
      samlPolicyProvider.using("tokenUri", "policyUri", Some("bar"), Some("baz"))

      verify(httpClientService).getDefaultClient()
      verify(httpClientService).getClient("bar")
    }

    it("should build a new service client when the token connection pool ID changes to None") {
      samlPolicyProvider.using("tokenUri", "policyUri", Some("foo"), Some("baz"))
      samlPolicyProvider.using("tokenUri", "policyUri", None, Some("baz"))

      verify(httpClientService).getClient("foo")
      verify(httpClientService).getDefaultClient()
    }

    it("should not build a new service client if the token connection pool ID does not change") {
      samlPolicyProvider.using("tokenUri", "policyUri", Some("foo"), Some("baz"))
      samlPolicyProvider.using("tokenUri", "policyUri", Some("foo"), Some("baz"))

      verify(httpClientService).getClient("foo")
    }

    it("should not build a new service client if the token connection pool ID does not change from/to null") {
      samlPolicyProvider.using("tokenUri", "policyUri", None, Some("baz"))
      samlPolicyProvider.using("tokenUri", "policyUri", None, Some("baz"))

      verify(httpClientService).getDefaultClient()
    }

    it("should build a new service client when the policy connection pool ID changes") {
      samlPolicyProvider.using("tokenUri", "policyUri", Some("baz"), Some("foo"))
      samlPolicyProvider.using("tokenUri", "policyUri", Some("baz"), Some("bar"))

      verify(httpClientService).getClient("foo")
      verify(httpClientService).getClient("bar")
    }

    it("should build a new service client when the policy connection pool ID changes from None") {
      samlPolicyProvider.using("tokenUri", "policyUri", Some("baz"), None)
      samlPolicyProvider.using("tokenUri", "policyUri", Some("baz"), Some("bar"))

      verify(httpClientService).getDefaultClient()
      verify(httpClientService).getClient("bar")
    }

    it("should build a new service client when the policy connection pool ID changes to None") {
      samlPolicyProvider.using("tokenUri", "policyUri", Some("baz"), Some("foo"))
      samlPolicyProvider.using("tokenUri", "policyUri", Some("baz"), None)

      verify(httpClientService).getClient("foo")
      verify(httpClientService).getDefaultClient()
    }

    it("should not build a new service client if the policy connection pool ID does not change") {
      samlPolicyProvider.using("tokenUri", "policyUri", Some("baz"), Some("foo"))
      samlPolicyProvider.using("tokenUri", "policyUri", Some("baz"), Some("foo"))

      verify(httpClientService).getClient("foo")
    }

    it("should not build a new service client if the policy connection pool ID does not change from/to null") {
      samlPolicyProvider.using("tokenUri", "policyUri", Some("baz"), None)
      samlPolicyProvider.using("tokenUri", "policyUri", Some("baz"), None)

      verify(httpClientService).getDefaultClient()
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
      when(tokenHttpClient.execute(
        MM.any[HttpUriRequest],
        MM.any[HttpContext]
      )).thenAnswer(makeAnswer((request, _) =>
        if (HttpPost.METHOD_NAME.equals(request.getMethod)) throw new RuntimeException("Could not connect")
        else null
      ))

      samlPolicyProvider.using("", "", Some(TokenServiceClientId), None)

      val result = samlPolicyProvider.getToken("username", "password", None)

      result shouldBe a[Failure[_]]
    }

    it("should forward a trace ID if provided") {
      when(tokenHttpClient.execute(
        MM.any[HttpUriRequest],
        MM.any[HttpContext]
      )).thenAnswer(makeAnswer((request, _) =>
        if (HttpPost.METHOD_NAME.equals(request.getMethod)) makeResponse(SC_OK)
        else null
      ))

      samlPolicyProvider.using("", "", Some(TokenServiceClientId), None)

      samlPolicyProvider.getToken("username", "password", Some("trace-id"))

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpUriRequest])
      verify(tokenHttpClient).execute(requestCaptor.capture(), MM.any[HttpContext])

      val request = requestCaptor.getValue
      request.getMethod shouldEqual HttpPost.METHOD_NAME
      request.getFirstHeader(TRACE_GUID).getValue shouldEqual "trace-id"
    }

    it("should not forward a trace ID if not provided") {
      when(tokenHttpClient.execute(
        MM.any[HttpUriRequest],
        MM.any[HttpContext]
      )).thenAnswer(makeAnswer((request, _) =>
        if (HttpPost.METHOD_NAME.equals(request.getMethod)) makeResponse(SC_OK)
        else null
      ))

      samlPolicyProvider.using("", "", Some(TokenServiceClientId), None)

      samlPolicyProvider.getToken("username", "password", None)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpUriRequest])
      verify(tokenHttpClient).execute(requestCaptor.capture(), MM.any[HttpContext])

      val request = requestCaptor.getValue
      request.getMethod shouldEqual HttpPost.METHOD_NAME
      request.getHeaders(TRACE_GUID) shouldBe empty
    }

    it("should return a Failure if the response cannot be parsed") {
      when(tokenHttpClient.execute(
        MM.any[HttpUriRequest],
        MM.any[HttpContext]
      )).thenAnswer(makeAnswer((request, _) =>
        if (HttpPost.METHOD_NAME.equals(request.getMethod)) {
          val responseContent = EntityBuilder.create()
            .setText(
              """
                |{
                |}
              """.stripMargin)
            .setContentType(ContentType.APPLICATION_JSON.withCharset(Charset.defaultCharset()))
            .build()
          makeResponse(
            SC_OK,
            responseContent
          )
        } else {
          null
        }
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
        when(tokenHttpClient.execute(
          MM.any[HttpUriRequest],
          MM.any[HttpContext]
        )).thenAnswer(makeAnswer((request, _) =>
          if (HttpPost.METHOD_NAME.equals(request.getMethod)) makeResponse(statusCode)
          else null
        ))

        samlPolicyProvider.using("", "", Some(TokenServiceClientId), None)

        val result = samlPolicyProvider.getToken("username", "password", None)

        result shouldBe a[Failure[_]]
        an [OverLimitException] should be thrownBy result.get
      }
    }

    it("should return a Success if the response is a 200") {
      when(tokenHttpClient.execute(
        MM.any[HttpUriRequest],
        MM.any[HttpContext]
      )).thenAnswer(makeAnswer((request, _) =>
        if (HttpPost.METHOD_NAME.equals(request.getMethod)) {
          val responseContent = EntityBuilder.create()
            .setText(sampleToken)
            .setContentType(ContentType.APPLICATION_JSON.withCharset(Charset.defaultCharset()))
            .build()
          makeResponse(
            SC_OK,
            responseContent
          )
        } else {
          null
        }
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
        when(tokenHttpClient.execute(
          MM.any[HttpUriRequest],
          MM.any[HttpContext]
        )).thenAnswer(makeAnswer((request, _) =>
          if (HttpPost.METHOD_NAME.equals(request.getMethod)) makeResponse(responseCode)
          else null
        ))

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
        when(tokenHttpClient.execute(
          MM.any[HttpUriRequest],
          MM.any[HttpContext]
        )).thenAnswer(makeAnswer((request, _) =>
          if (HttpPost.METHOD_NAME.equals(request.getMethod)) {
            val responseContent = EntityBuilder.create()
              .setText(sampleToken)
              .setContentType(ContentType.APPLICATION_JSON.withCharset(Charset.defaultCharset()))
              .build()
            makeResponse(
              SC_OK,
              responseContent
            )
          } else {
            null
          }
        ))

        samlPolicyProvider.using("", "", Some(TokenServiceClientId), None)

        val result = samlPolicyProvider.getToken("username", "password", None)

        result shouldBe a[Success[_]]
        result.get shouldEqual "some-token"
      }
    }

    it("should always check the HTTP request cache") {
      when(tokenHttpClient.execute(
        MM.any[HttpUriRequest],
        MM.any[HttpContext]
      )).thenAnswer(makeAnswer((request, _) =>
        if (HttpPost.METHOD_NAME.equals(request.getMethod)) {
          val responseContent = EntityBuilder.create()
            .setText(sampleToken)
            .setContentType(ContentType.APPLICATION_JSON.withCharset(Charset.defaultCharset()))
            .build()
          makeResponse(
            SC_OK,
            responseContent
          )
        } else {
          null
        }
      ))

      samlPolicyProvider.using("", "", Some(TokenServiceClientId), None)

      samlPolicyProvider.getToken("username", "password", None)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpUriRequest])
      verify(tokenHttpClient).execute(requestCaptor.capture(), MM.any[HttpContext])

      val request = requestCaptor.getValue
      request.getMethod shouldEqual HttpPost.METHOD_NAME
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
        |	   "approvedDomainIds": ["77366"],
        |	   "description": "a description",
        |	   "id": "$sampleIdpId",
        |	   "issuer": "https://demo.issuer.com"
        |  }]
        |}
      """.stripMargin

    it("should return a Failure if the service client cannot connect") {
      when(policyHttpClient.execute(
        MM.any[HttpUriRequest],
        MM.any[HttpContext]
      )).thenAnswer(makeAnswer((request, _) =>
        if (HttpGet.METHOD_NAME.equals(request.getMethod)) throw new RuntimeException("Could not connect")
        else null
      ))

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      val result = samlPolicyProvider.getIdpId("issuer", "token", None, checkCache = true)

      result shouldBe a[Failure[_]]
    }

    it("should forward a trace ID if provided") {
      when(policyHttpClient.execute(
        MM.any[HttpUriRequest],
        MM.any[HttpContext]
      )).thenAnswer(makeAnswer((request, _) =>
        if (HttpGet.METHOD_NAME.equals(request.getMethod)) makeResponse(SC_OK)
        else null
      ))

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      samlPolicyProvider.getIdpId("issuer", "token", Some("trace-id"), checkCache = true)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpUriRequest])
      verify(policyHttpClient).execute(requestCaptor.capture(), MM.any[HttpContext])

      val request = requestCaptor.getValue
      request.getMethod shouldEqual HttpGet.METHOD_NAME
      request.getFirstHeader(TRACE_GUID).getValue shouldEqual "trace-id"
    }

    it("should not forward a trace ID if not provided") {
      when(policyHttpClient.execute(
        MM.any[HttpUriRequest],
        MM.any[HttpContext]
      )).thenAnswer(makeAnswer((request, _) =>
        if (HttpGet.METHOD_NAME.equals(request.getMethod)) makeResponse(SC_OK)
        else null
      ))

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      samlPolicyProvider.getIdpId("issuer", "token", None, checkCache = true)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpUriRequest])
      verify(policyHttpClient).execute(requestCaptor.capture(), MM.any[HttpContext])

      val request = requestCaptor.getValue
      request.getMethod shouldEqual HttpGet.METHOD_NAME
      request.getHeaders(TRACE_GUID) shouldBe empty
    }

    it("should forward the provided token as a header") {
      val token = "a-unique-token"

      when(policyHttpClient.execute(
        MM.any[HttpUriRequest],
        MM.any[HttpContext]
      )).thenAnswer(makeAnswer((request, _) =>
        if (HttpGet.METHOD_NAME.equals(request.getMethod)) makeResponse(SC_OK)
        else null
      ))

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      samlPolicyProvider.getIdpId("issuer", token, Some("trace-id"), checkCache = true)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpUriRequest])
      verify(policyHttpClient).execute(requestCaptor.capture(), MM.any[HttpContext])

      val request = requestCaptor.getValue
      request.getMethod shouldEqual HttpGet.METHOD_NAME
      request.getFirstHeader(CommonHttpHeader.AUTH_TOKEN).getValue shouldEqual token
    }

    it("should return a Failure if the response cannot be parsed") {
      when(policyHttpClient.execute(
        MM.any[HttpUriRequest],
        MM.any[HttpContext]
      )).thenAnswer(makeAnswer((request, _) =>
        if (HttpGet.METHOD_NAME.equals(request.getMethod)) {
          val content = EntityBuilder.create()
            .setText(
              """
                |{
                |}
              """.stripMargin)
            .setContentType(ContentType.APPLICATION_JSON.withCharset(Charset.defaultCharset()))
            .build()
          makeResponse(SC_OK, content)
        } else {
          null
        }
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
        when(policyHttpClient.execute(
          MM.any[HttpUriRequest],
          MM.any[HttpContext]
        )).thenAnswer(makeAnswer((request, _) =>
          if (HttpGet.METHOD_NAME.equals(request.getMethod)) makeResponse(statusCode)
          else null
        ))

        samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

        val result = samlPolicyProvider.getIdpId("issuer", "token", None, checkCache = true)

        result shouldBe a[Failure[_]]
        an [OverLimitException] should be thrownBy result.get
      }
    }

    it("should return a Success if the response is a 200") {
      when(policyHttpClient.execute(
        MM.any[HttpUriRequest],
        MM.any[HttpContext]
      )).thenAnswer(makeAnswer((request, _) =>
        if (HttpGet.METHOD_NAME.equals(request.getMethod)) {
          val content = EntityBuilder.create()
            .setText(sampleIdp)
            .setContentType(ContentType.APPLICATION_JSON.withCharset(Charset.defaultCharset()))
            .build()
          makeResponse(SC_OK, content)
        } else {
          null
        }
      ))

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      val result = samlPolicyProvider.getIdpId("issuer", "token", None, checkCache = true)

      result shouldBe a[Success[_]]
      result.get.idpId shouldEqual sampleIdpId
      result.get.domains should contain only("77366")
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
        when(policyHttpClient.execute(
          MM.any[HttpUriRequest],
          MM.any[HttpContext]
        )).thenAnswer(makeAnswer((request, _) =>
          if (HttpGet.METHOD_NAME.equals(request.getMethod)) makeResponse(responseCode)
          else null
        ))

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
        when(policyHttpClient.execute(
          MM.any[HttpUriRequest],
          MM.any[HttpContext]
        )).thenAnswer(makeAnswer((request, _) =>
          if (HttpGet.METHOD_NAME.equals(request.getMethod)) {
            val content = EntityBuilder.create()
              .setText(sampleIdp)
              .setContentType(ContentType.APPLICATION_JSON.withCharset(Charset.defaultCharset()))
              .build()
            makeResponse(SC_OK, content)
          } else {
            null
          }
        ))

        samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

        val result = samlPolicyProvider.getIdpId("issuer", "token", None, checkCache = true)

        result shouldBe a[Success[_]]
        result.get.idpId shouldEqual sampleIdpId
        result.get.domains should contain only("77366")
      }
    }

    it("should check the HTTP request cache if not retrying") {
      when(policyHttpClient.execute(
        MM.any[HttpUriRequest],
        MM.any[HttpContext]
      )).thenAnswer(makeAnswer((request, _) =>
        if (HttpGet.METHOD_NAME.equals(request.getMethod)) {
          val content = EntityBuilder.create()
            .setText(sampleIdp)
            .setContentType(ContentType.APPLICATION_JSON.withCharset(Charset.defaultCharset()))
            .build()
          makeResponse(SC_OK, content)
        } else {
          null
        }
      ))

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      samlPolicyProvider.getIdpId("issuer", "token", None, checkCache = true)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpUriRequest])
      val contextCaptor = ArgumentCaptor.forClass(classOf[CachingHttpClientContext])
      verify(policyHttpClient).execute(requestCaptor.capture(), contextCaptor.capture())

      val request = requestCaptor.getValue
      val context = contextCaptor.getValue
      request.getMethod shouldEqual HttpGet.METHOD_NAME
      context.getUseCache shouldBe true
    }

    it("should not check the HTTP request cache if retrying") {
      when(policyHttpClient.execute(
        MM.any[HttpUriRequest],
        MM.any[HttpContext]
      )).thenAnswer(makeAnswer((request, _) =>
        if (HttpGet.METHOD_NAME.equals(request.getMethod)) {
          val content = EntityBuilder.create()
            .setText(sampleIdp)
            .setContentType(ContentType.APPLICATION_JSON.withCharset(Charset.defaultCharset()))
            .build()
          makeResponse(SC_OK, content)
        } else {
          null
        }
      ))

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      samlPolicyProvider.getIdpId("issuer", "token", None, checkCache = false)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpUriRequest])
      val contextCaptor = ArgumentCaptor.forClass(classOf[CachingHttpClientContext])
      verify(policyHttpClient).execute(requestCaptor.capture(), contextCaptor.capture())

      val request = requestCaptor.getValue
      val context = contextCaptor.getValue
      request.getMethod shouldEqual HttpGet.METHOD_NAME
      context.getUseCache shouldBe false
    }

    it("should encode the issuer query parameter") {
      val issuer = "http://example.com/path?query=string"

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      try {
        samlPolicyProvider.getIdpId(issuer, "token", None, checkCache = false)
      } catch { case _: Throwable => }

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpUriRequest])
      verify(policyHttpClient).execute(requestCaptor.capture(), MM.any[HttpContext])

      val request = requestCaptor.getValue
      request.getMethod shouldEqual HttpGet.METHOD_NAME
      request.getURI.toString should endWith (UriUtils.encodeQueryParam(issuer, StandardCharsets.UTF_8.name()))
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
      when(policyHttpClient.execute(
        MM.any[HttpUriRequest],
        MM.any[HttpContext]
      )).thenAnswer(makeAnswer((request, _) =>
        if (HttpGet.METHOD_NAME.equals(request.getMethod)) throw new RuntimeException("Could not connect")
        else null
      ))

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      val result = samlPolicyProvider.getPolicy(ProviderInfo("idpId", Array.empty), "token", None, checkCache = true)

      result shouldBe a[Failure[_]]
    }

    it("should forward a trace ID if provided") {
      when(policyHttpClient.execute(
        MM.any[HttpUriRequest],
        MM.any[HttpContext]
      )).thenAnswer(makeAnswer((request, _) =>
        if (HttpGet.METHOD_NAME.equals(request.getMethod)) makeResponse(SC_OK)
        else null
      ))

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      samlPolicyProvider.getPolicy(ProviderInfo("idpId", Array.empty), "token", Some("trace-id"), checkCache = true)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpUriRequest])
      verify(policyHttpClient).execute(requestCaptor.capture(), MM.any[HttpContext])

      val request = requestCaptor.getValue
      request.getMethod shouldEqual HttpGet.METHOD_NAME
      request.getFirstHeader(TRACE_GUID).getValue shouldEqual "trace-id"
    }

    it("should not forward a trace ID if not provided") {
      when(policyHttpClient.execute(
        MM.any[HttpUriRequest],
        MM.any[HttpContext]
      )).thenAnswer(makeAnswer((request, _) =>
        if (HttpGet.METHOD_NAME.equals(request.getMethod)) makeResponse(SC_OK)
        else null
      ))

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      samlPolicyProvider.getPolicy(ProviderInfo("idpId", Array.empty), "token", None, checkCache = true)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpUriRequest])
      verify(policyHttpClient).execute(requestCaptor.capture(), MM.any[HttpContext])

      val request = requestCaptor.getValue
      request.getMethod shouldEqual HttpGet.METHOD_NAME
      request.getHeaders(TRACE_GUID) shouldBe empty
    }

    it("should forward the provided token as a header") {
      val token = "a-unique-token"

      when(policyHttpClient.execute(
        MM.any[HttpUriRequest],
        MM.any[HttpContext]
      )).thenAnswer(makeAnswer((request, _) =>
        if (HttpGet.METHOD_NAME.equals(request.getMethod)) makeResponse(SC_OK)
        else null
      ))

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      samlPolicyProvider.getPolicy(ProviderInfo("idpId", Array.empty), token, Some("trace-id"), checkCache = true)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpUriRequest])
      verify(policyHttpClient).execute(requestCaptor.capture(), MM.any[HttpContext])

      val request = requestCaptor.getValue
      request.getMethod shouldEqual HttpGet.METHOD_NAME
      request.getFirstHeader(CommonHttpHeader.AUTH_TOKEN).getValue shouldEqual token
    }

    Set(
      SC_REQUEST_ENTITY_TOO_LARGE,
      SC_TOO_MANY_REQUESTS
    ) foreach { statusCode =>
      it(s"should return a Failure if the request is rate limited with a $statusCode") {
        when(policyHttpClient.execute(
          MM.any[HttpUriRequest],
          MM.any[HttpContext]
        )).thenAnswer(makeAnswer((request, _) =>
          if (HttpGet.METHOD_NAME.equals(request.getMethod)) makeResponse(statusCode)
          else null
        ))

        samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

        val result = samlPolicyProvider.getPolicy(ProviderInfo("idpId", Array.empty), "token", None, checkCache = true)

        result shouldBe a[Failure[_]]
        an [OverLimitException] should be thrownBy result.get
      }
    }

    it("should return a Success if the response is a 200") {
      when(policyHttpClient.execute(
        MM.any[HttpUriRequest],
        MM.any[HttpContext]
      )).thenAnswer(makeAnswer((request, _) =>
        if (HttpGet.METHOD_NAME.equals(request.getMethod)) {
          val content = EntityBuilder.create()
            .setText(samplePolicy)
            .setContentType(ContentType.APPLICATION_JSON.withCharset(Charset.defaultCharset()))
            .build()
          makeResponse(SC_OK, content)
        } else {
          null
        }
      ))

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      val result = samlPolicyProvider.getPolicy(ProviderInfo("idpId", Array.empty), "token", None, checkCache = true)

      result shouldBe a[Success[_]]
      Json.stringify(Json.parse(result.get.content)) shouldEqual Json.stringify(Json.parse(samplePolicy))
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
        when(policyHttpClient.execute(
          MM.any[HttpUriRequest],
          MM.any[HttpContext]
        )).thenAnswer(makeAnswer((request, _) =>
          if (HttpGet.METHOD_NAME.equals(request.getMethod)) makeResponse(responseCode)
          else null
        ))

        samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

        val result = samlPolicyProvider.getPolicy(ProviderInfo("idpId", Array.empty), "token", None, checkCache = true)

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
        when(policyHttpClient.execute(
          MM.any[HttpUriRequest],
          MM.any[HttpContext]
        )).thenAnswer(makeAnswer((request, _) =>
          if (HttpGet.METHOD_NAME.equals(request.getMethod)) {
            val content = EntityBuilder.create()
              .setText(samplePolicy)
              .setContentType(ContentType.APPLICATION_JSON.withCharset(Charset.defaultCharset()))
              .build()
            makeResponse(SC_OK, content)
          } else {
            null
          }
        ))

        samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

        val result = samlPolicyProvider.getPolicy(ProviderInfo("idpId", Array.empty), "token", None, checkCache = true)

        result shouldBe a[Success[_]]
        Json.stringify(Json.parse(result.get.content)) shouldEqual Json.stringify(Json.parse(samplePolicy))
      }
    }

    it(s"should pass on domains") {
      when(policyHttpClient.execute(
        MM.any[HttpUriRequest],
        MM.any[HttpContext]
      )).thenAnswer(makeAnswer((request, _) =>
        if (HttpGet.METHOD_NAME.equals(request.getMethod)) {
          val content = EntityBuilder.create()
            .setText(samplePolicy)
            .setContentType(ContentType.APPLICATION_JSON.withCharset(Charset.defaultCharset()))
            .build()
          makeResponse(SC_OK, content)
        } else {
          null
        }
      ))

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      val result = samlPolicyProvider.getPolicy(ProviderInfo("idpId", Array("banana")), "token", None, checkCache = true)

      result shouldBe a[Success[_]]
      result.get.domains should contain only "banana"
    }

    it("should check the HTTP request cache if not retrying") {
      when(policyHttpClient.execute(
        MM.any[HttpUriRequest],
        MM.any[HttpContext]
      )).thenAnswer(makeAnswer((request, _) =>
        if (HttpGet.METHOD_NAME.equals(request.getMethod)) {
          val content = EntityBuilder.create()
            .setText(samplePolicy)
            .setContentType(ContentType.APPLICATION_JSON.withCharset(Charset.defaultCharset()))
            .build()
          makeResponse(SC_OK, content)
        } else {
          null
        }
      ))

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      samlPolicyProvider.getPolicy(ProviderInfo("idpId", Array.empty), "token", None, checkCache = true)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpUriRequest])
      verify(policyHttpClient).execute(requestCaptor.capture(), MM.any[HttpContext])

      val request = requestCaptor.getValue
      request.getMethod shouldEqual HttpGet.METHOD_NAME
    }

    it("should not check the HTTP request cache if retrying") {
      when(policyHttpClient.execute(
        MM.any[HttpUriRequest],
        MM.any[HttpContext]
      )).thenAnswer(makeAnswer((request, _) =>
        if (HttpGet.METHOD_NAME.equals(request.getMethod)) {
          val content = EntityBuilder.create()
            .setText(samplePolicy)
            .setContentType(ContentType.APPLICATION_JSON.withCharset(Charset.defaultCharset()))
            .build()
          makeResponse(SC_OK, content)
        } else {
          null
        }
      ))

      samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

      samlPolicyProvider.getPolicy(ProviderInfo("idpId", Array.empty), "token", None, checkCache = false)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpUriRequest])
      verify(policyHttpClient).execute(requestCaptor.capture(), MM.any[HttpContext])

      val request = requestCaptor.getValue
      request.getMethod shouldEqual HttpGet.METHOD_NAME
    }

    Seq(
      ContentType.create(SamlIdentityClient.TextYaml),
      ContentType.APPLICATION_JSON,
      ContentType.APPLICATION_XML,
      ContentType.TEXT_PLAIN
    ) foreach { contentType =>
      it(s"should return the content type of the $contentType policy") {
        when(policyHttpClient.execute(
          MM.any[HttpUriRequest],
          MM.any[HttpContext]
        )).thenAnswer(makeAnswer((request, _) =>
          if (HttpGet.METHOD_NAME.equals(request.getMethod)) {
            val content = EntityBuilder.create()
              .setText("")
              .setContentType(contentType)
              .build()
            makeResponse(SC_OK, content)
          } else {
            null
          }
        ))

        samlPolicyProvider.using("", "", None, Some(PolicyServiceClientId))

        val result = samlPolicyProvider.getPolicy(ProviderInfo("idpId", Array.empty), "token", None, checkCache = false)

        result shouldBe a[Success[_]]
        result.get.contentType shouldEqual contentType.getMimeType
      }
    }
  }

  def makeAnswer(f: (HttpUriRequest, CachingHttpClientContext) => CloseableHttpResponse): Answer[CloseableHttpResponse] = {
    new Answer[CloseableHttpResponse] {
      override def answer(invocation: InvocationOnMock): CloseableHttpResponse = {
        val request = invocation.getArguments()(0).asInstanceOf[HttpUriRequest]
        val context = CachingHttpClientContext.adapt(invocation.getArguments()(1).asInstanceOf[HttpContext])
        f(request, context)
      }
    }
  }

  def makeResponse(statusCode: Int, entity: HttpEntity = null, headers: Map[String, Seq[String]] = Map.empty): CloseableHttpResponse = {
    val response = new BasicHttpResponse(HttpVersion.HTTP_1_1, statusCode, null) with CloseableHttpResponse {
      override def close(): Unit = {}
    }
    Option(entity).foreach(response.setEntity)
    headers.foldLeft(Seq.empty[(String, String)]) { case (aggregate, (headerName, headerValues)) =>
      aggregate ++ headerValues.map(headerName -> _)
    }.foreach(tupled(response.addHeader))
    response
  }

  implicit def looseToStrictStringMap(sm: java.util.Map[_, _]): java.util.Map[String, String] =
    sm.asInstanceOf[java.util.Map[String, String]]
}
