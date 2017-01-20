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
import org.openrepose.filters.samlpolicy.SamlPolicyProvider.{OverLimitException, SC_TOO_MANY_REQUESTS}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

import scala.language.implicitConversions
import scala.util.{Failure, Success}

@RunWith(classOf[JUnitRunner])
class SamlPolicyProviderTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {

  var akkaServiceClient: AkkaServiceClient = _
  var akkaServiceClientFactory: AkkaServiceClientFactory = _
  var samlPolicyProvider: SamlPolicyProvider = _

  override def beforeEach(): Unit = {
    akkaServiceClient = mock[AkkaServiceClient]
    akkaServiceClientFactory = mock[AkkaServiceClientFactory]

    samlPolicyProvider = new SamlPolicyProvider(akkaServiceClientFactory)

    when(akkaServiceClientFactory.newAkkaServiceClient())
      .thenReturn(akkaServiceClient)
    when(akkaServiceClientFactory.newAkkaServiceClient(MM.anyString()))
      .thenReturn(akkaServiceClient)
  }

  describe("using") {
    it("should build a new service client when the token connection pool ID changes") {
      samlPolicyProvider.using("tokenUri", "policyUri", Some("foo"), Some("baz"))
      samlPolicyProvider.using("tokenUri", "policyUri", Some("bar"), Some("baz"))

      verify(akkaServiceClientFactory).newAkkaServiceClient("foo")
      verify(akkaServiceClient).destroy()
      verify(akkaServiceClientFactory).newAkkaServiceClient("bar")
    }

    it("should build a new service client when the token connection pool ID changes from None") {
      samlPolicyProvider.using("tokenUri", "policyUri", None, Some("baz"))
      samlPolicyProvider.using("tokenUri", "policyUri", Some("bar"), Some("baz"))

      verify(akkaServiceClientFactory).newAkkaServiceClient()
      verify(akkaServiceClient).destroy()
      verify(akkaServiceClientFactory).newAkkaServiceClient("bar")
    }

    it("should build a new service client when the token connection pool ID changes to None") {
      samlPolicyProvider.using("tokenUri", "policyUri", Some("foo"), Some("baz"))
      samlPolicyProvider.using("tokenUri", "policyUri", None, Some("baz"))

      verify(akkaServiceClientFactory).newAkkaServiceClient("foo")
      verify(akkaServiceClient).destroy()
      verify(akkaServiceClientFactory).newAkkaServiceClient()
    }

    it("should not build a new service client if the token connection pool ID does not change") {
      samlPolicyProvider.using("tokenUri", "policyUri", Some("foo"), Some("baz"))
      samlPolicyProvider.using("tokenUri", "policyUri", Some("foo"), Some("baz"))

      verify(akkaServiceClientFactory).newAkkaServiceClient("foo")
      verify(akkaServiceClient, never).destroy()
    }

    it("should not build a new service client if the token connection pool ID does not change from/to null") {
      samlPolicyProvider.using("tokenUri", "policyUri", None, Some("baz"))
      samlPolicyProvider.using("tokenUri", "policyUri", None, Some("baz"))

      verify(akkaServiceClientFactory).newAkkaServiceClient()
      verify(akkaServiceClient, never).destroy()
    }

    it("should build a new service client when the policy connection pool ID changes") {
      samlPolicyProvider.using("tokenUri", "policyUri", Some("baz"), Some("foo"))
      samlPolicyProvider.using("tokenUri", "policyUri", Some("baz"), Some("bar"))

      verify(akkaServiceClientFactory).newAkkaServiceClient("foo")
      verify(akkaServiceClient).destroy()
      verify(akkaServiceClientFactory).newAkkaServiceClient("bar")
    }

    it("should build a new service client when the policy connection pool ID changes from None") {
      samlPolicyProvider.using("tokenUri", "policyUri", Some("baz"), None)
      samlPolicyProvider.using("tokenUri", "policyUri", Some("baz"), Some("bar"))

      verify(akkaServiceClientFactory).newAkkaServiceClient()
      verify(akkaServiceClient).destroy()
      verify(akkaServiceClientFactory).newAkkaServiceClient("bar")
    }

    it("should build a new service client when the policy connection pool ID changes to None") {
      samlPolicyProvider.using("tokenUri", "policyUri", Some("baz"), Some("foo"))
      samlPolicyProvider.using("tokenUri", "policyUri", Some("baz"), None)

      verify(akkaServiceClientFactory).newAkkaServiceClient("foo")
      verify(akkaServiceClient).destroy()
      verify(akkaServiceClientFactory).newAkkaServiceClient()
    }

    it("should not build a new service client if the policy connection pool ID does not change") {
      samlPolicyProvider.using("tokenUri", "policyUri", Some("baz"), Some("foo"))
      samlPolicyProvider.using("tokenUri", "policyUri", Some("baz"), Some("foo"))

      verify(akkaServiceClientFactory).newAkkaServiceClient("foo")
      verify(akkaServiceClient, never).destroy()
    }

    it("should not build a new service client if the policy connection pool ID does not change from/to null") {
      samlPolicyProvider.using("tokenUri", "policyUri", Some("baz"), None)
      samlPolicyProvider.using("tokenUri", "policyUri", Some("baz"), None)

      verify(akkaServiceClientFactory).newAkkaServiceClient()
      verify(akkaServiceClient, never).destroy()
    }
  }

  describe("getToken") {
    it("should return a Failure if the service client cannot connect") {
      when(akkaServiceClient.post(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.anyString(),
        MM.any[MediaType],
        MM.anyBoolean()
      )).thenThrow(new RuntimeException("Could not connect"))

      samlPolicyProvider.using("", "", None, None)

      val result = samlPolicyProvider.getToken("username", "password", None)

      result shouldBe a[Failure[_]]
    }

    it("should forward a trace ID if provided") {
      when(akkaServiceClient.post(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.anyString(),
        MM.any[MediaType],
        MM.anyBoolean()
      )).thenReturn(new ServiceClientResponse(SC_OK, null))

      samlPolicyProvider.using("", "", None, None)

      samlPolicyProvider.getToken("username", "password", Some("trace-id"))

      verify(akkaServiceClient).post(
        MM.anyString(),
        MM.anyString(),
        MM.argThat(HM.hasEntry(TRACE_GUID, "trace-id")),
        MM.anyString(),
        MM.any[MediaType],
        MM.anyBoolean()
      )
    }

    it("should not forward a trace ID if not provided") {
      when(akkaServiceClient.post(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.anyString(),
        MM.any[MediaType],
        MM.anyBoolean()
      )).thenReturn(new ServiceClientResponse(SC_OK, null))

      samlPolicyProvider.using("", "", None, None)

      samlPolicyProvider.getToken("username", "password", None)

      verify(akkaServiceClient).post(
        MM.anyString(),
        MM.anyString(),
        MM.argThat(HM.not(HM.hasKey(TRACE_GUID))),
        MM.anyString(),
        MM.any[MediaType],
        MM.anyBoolean()
      )
    }

    it("should return a Failure if the response cannot be parsed") {
      when(akkaServiceClient.post(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.anyString(),
        MM.any[MediaType],
        MM.anyBoolean()
      )).thenReturn(new ServiceClientResponse(
        SC_OK,
        Array(new BasicHeader(
          CONTENT_TYPE,
          MediaType.APPLICATION_JSON + "; charset=" + Charset.defaultCharset().name()
        )),
        new ByteArrayInputStream(
          """
            |{
            |}
          """.stripMargin.getBytes
        )
      ))

      samlPolicyProvider.using("", "", None, None)

      val result = samlPolicyProvider.getToken("username", "password", None)

      result shouldBe a[Failure[_]]
    }

    Set(
      SC_REQUEST_ENTITY_TOO_LARGE,
      SC_TOO_MANY_REQUESTS
    ) foreach { statusCode =>
      it(s"should return a Failure if the request is rate limited with a $statusCode") {
        when(akkaServiceClient.post(
          MM.anyString(),
          MM.anyString(),
          MM.anyMapOf(classOf[String], classOf[String]),
          MM.anyString(),
          MM.any[MediaType],
          MM.anyBoolean()
        )).thenReturn(new ServiceClientResponse(statusCode, null))

        samlPolicyProvider.using("", "", None, None)

        val result = samlPolicyProvider.getToken("username", "password", None)

        result shouldBe a[Failure[_]]
        an [OverLimitException] should be thrownBy result.get
      }
    }

    it("should return a Success if the response is a 200") {
      when(akkaServiceClient.post(
        MM.anyString(),
        MM.anyString(),
        MM.anyMapOf(classOf[String], classOf[String]),
        MM.anyString(),
        MM.any[MediaType],
        MM.anyBoolean()
      )).thenReturn(new ServiceClientResponse(
        SC_OK,
        Array(new BasicHeader(
          CONTENT_TYPE,
          MediaType.APPLICATION_JSON + "; charset=" + Charset.defaultCharset().name()
        )),
        new ByteArrayInputStream(
          """
            |{
            |  "access": {
            |    "token": {
            |      "id": "some-token"
            |    }
            |  }
            |}
          """.stripMargin.getBytes
        )
      ))

      samlPolicyProvider.using("", "", None, None)

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
        when(akkaServiceClient.post(
          MM.anyString(),
          MM.anyString(),
          MM.anyMapOf(classOf[String], classOf[String]),
          MM.anyString(),
          MM.any[MediaType],
          MM.anyBoolean()
        )).thenReturn(new ServiceClientResponse(SC_NOT_FOUND, null))

        samlPolicyProvider.using("", "", None, None)

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
        when(akkaServiceClient.post(
          MM.anyString(),
          MM.anyString(),
          MM.anyMapOf(classOf[String], classOf[String]),
          MM.anyString(),
          MM.any[MediaType],
          MM.anyBoolean()
        )).thenReturn(new ServiceClientResponse(
          SC_OK,
          Array(new BasicHeader(
            CONTENT_TYPE,
            MediaType.APPLICATION_JSON + "; charset=" + charset.name()
          )),
          new ByteArrayInputStream(
            """
              |{
              |  "access": {
              |    "token": {
              |      "id": "some-token"
              |    }
              |  }
              |}
            """.stripMargin.getBytes(charset)
          )
        ))

        samlPolicyProvider.using("", "", None, None)

        val result = samlPolicyProvider.getToken("username", "password", None)

        result shouldBe a[Success[_]]
        result.get shouldEqual "some-token"
      }
    }
  }

  describe("getPolicy") {
    pending
  }

  implicit def looseToStrictStringMap(sm: java.util.Map[_, _]): java.util.Map[String, String] =
    sm.asInstanceOf[java.util.Map[String, String]]

  implicit def commonHeaderToString(h: CommonHttpHeader): String = h.toString
}
