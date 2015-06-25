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
package org.openrepose.filters.keystonev2

import java.net.URL
import java.util.concurrent.TimeUnit
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.mockrunner.mock.web.{MockFilterChain, MockFilterConfig, MockHttpServletRequest, MockHttpServletResponse}
import com.rackspace.httpdelegation.{HttpDelegationHeaderNames, HttpDelegationManager}
import org.apache.commons.codec.binary.Base64
import org.hamcrest.Matchers.{both, greaterThanOrEqualTo, lessThanOrEqualTo}
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.mockito.Matchers.{eq => mockitoEq, _}
import org.mockito.Mockito._
import org.openrepose.commons.utils.http.{CommonHttpHeader, IdentityStatus, OpenStackServiceHeader, PowerApiHeader}
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.{Datastore, DatastoreService}
import org.openrepose.filters.keystonev2.RequestHandler._
import org.openrepose.filters.keystonev2.config.ServiceEndpointType
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec}

import scala.collection.JavaConverters._
import scala.util.Success

@RunWith(classOf[JUnitRunner])
class KeystoneV2FilterTest extends FunSpec
with org.scalatest.Matchers
with BeforeAndAfter
with MockitoSugar
with IdentityResponses
with MockedAkkaServiceClient
with HttpDelegationManager {
  System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
    "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl")

  val mockDatastoreService = mock[DatastoreService]
  private val mockDatastore: Datastore = mock[Datastore]
  when(mockDatastoreService.getDefaultDatastore).thenReturn(mockDatastore)
  val mockConfigService = mock[ConfigurationService]
  private final val dateTime = DateTime.now().plusHours(1)

  before {
    reset(mockDatastore)
    reset(mockConfigService)
    mockAkkaServiceClient.reset()
  }

  after {
    mockAkkaServiceClient.validate()
  }

  describe("Filter lifecycle") {
    val filter: KeystoneV2Filter = new KeystoneV2Filter(mockConfigService, mockAkkaServiceClient, mockDatastoreService)
    val config: MockFilterConfig = new MockFilterConfig

    it("should throw 500 if filter is not initialized") {
      val request = new MockHttpServletRequest
      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain
      filter.isInitialized shouldBe false
      filter.doFilter(request, response, filterChain)
      response.getErrorCode shouldBe HttpServletResponse.SC_INTERNAL_SERVER_ERROR
    }

    it("should subscribe a listener to the configuration service on init") {
      filter.init(config)

      verify(mockConfigService).subscribeTo(
        anyString(),
        anyString(),
        any[URL],
        any(),
        any()
      )
    }

    it("should unsubscribe a listener to the configuration service on destroy") {
      filter.destroy()

      verify(mockConfigService).unsubscribeFrom(
        anyString(),
        any()
      )
    }
  }

  describe("Configured simply to authenticate tokens, defaults for everything else") {
    //Configure the filter
    def configuration = Marshaller.keystoneV2ConfigFromString(
      """<?xml version="1.0" encoding="UTF-8"?>
        |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
        |    <identity-service
        |            username="username"
        |            password="password"
        |            uri="https://some.identity.com"
        |            set-groups-in-header="false"
        |            set-catalog-in-header="false"
        |            />
        |</keystone-v2>
      """.stripMargin)

    val filter: KeystoneV2Filter = new KeystoneV2Filter(mockConfigService, mockAkkaServiceClient, mockDatastoreService)

    val config: MockFilterConfig = new MockFilterConfig
    filter.init(config)
    filter.configurationUpdated(configuration)

    it("Validates a token allowing through the filter") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn(null, "glibglob")

      //Pretend like identity is going to give us a valid admin token
      mockAkkaPostResponse(
        AkkaServiceClientResponse(HttpServletResponse.SC_OK, adminAuthenticationTokenResponse())
      )

      //Urgh, I have to hit the akka service client twice
      mockAkkaGetResponse(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse())
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)
    }

    it("caches the admin token request for 10 minutes") {
      //Can only make sure it was put into the cache with a 10 minute timeout...
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn(null, "glibglob")

      //Pretend like identity is going to give us a valid admin token
      mockAkkaPostResponse(
        AkkaServiceClientResponse(HttpServletResponse.SC_OK, adminAuthenticationTokenResponse())
      )

      mockAkkaGetResponse(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse())
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      verify(mockDatastore).put(ADMIN_TOKEN_KEY, "glibglob")

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)
    }

    it("caches a valid token for 10 minutes") {
      //Can only make sure it was put into the cache with a 10 minute timeout...
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn(null, "glibglob")

      //Pretend like identity is going to give us a valid admin token
      mockAkkaPostResponse(
        AkkaServiceClientResponse(HttpServletResponse.SC_OK, adminAuthenticationTokenResponse())
      )

      mockAkkaGetResponse(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse(expires = dateTime))
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      verify(mockDatastore).put(ADMIN_TOKEN_KEY, "glibglob")
      //Have to cache the result of the stuff
      verify(mockDatastore).put(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"),
        any(classOf[ValidToken]),
        mockitoEq(600),
        mockitoEq(TimeUnit.SECONDS))

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)
    }

    it("caches an INvalid token for 10 minutes") {
      //Can only make sure it was put into the cache with a 10 minute timeout...
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, "INVALID_TOKEN")

      //Pretend like identity is going to give us a valid admin token
      mockAkkaPostResponse {
        AkkaServiceClientResponse(HttpServletResponse.SC_OK, adminAuthenticationTokenResponse())
      }

      mockAkkaGetResponse(s"${TOKEN_KEY_PREFIX}INVALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_NOT_FOUND, "")
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      verify(mockDatastore).put(ADMIN_TOKEN_KEY, "glibglob")
      //Have to cache the result of the stuff
      verify(mockDatastore).put(s"${TOKEN_KEY_PREFIX}INVALID_TOKEN", InvalidToken, 600, TimeUnit.SECONDS)

      filterChain.getLastRequest should be(null)
      filterChain.getLastResponse should be(null)

      response.getErrorCode shouldBe HttpServletResponse.SC_UNAUTHORIZED
    }

    it("Makes no other calls if the token is already cached with a valid result") {
      //Can only make sure it was put into the cache with a 10 minute timeout...
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //When the user's token details are cached, no calls to identity should take place

      //When we ask the cache for our token, it works
      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(roles = Vector("compute:admin", "object-store:admin")), Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)
    }

    it("Makes no other calls if the token is already cached with a token not valid result") {
      //Can only make sure it was put into the cache with a 10 minute timeout...
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, "LOLNOPE")

      //When the user's token details are cached, no calls to identity should take place

      //When we ask the cache for our token, it works
      when(mockDatastore.get(s"${TOKEN_KEY_PREFIX}LOLNOPE")).thenReturn(InvalidToken, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest should be(null)
      filterChain.getLastResponse should be(null)

      response.getErrorCode shouldBe HttpServletResponse.SC_UNAUTHORIZED
    }

    it("rejects with 401 an invalid token") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, "notValidToken")

      //Pretend like identity is going to give us a valid admin token
      mockAkkaPostResponse {
        AkkaServiceClientResponse(HttpServletResponse.SC_OK, adminAuthenticationTokenResponse())
      }
      //Urgh, I have to hit the akka service client twice
      mockAkkaGetResponse(s"${TOKEN_KEY_PREFIX}notValidToken")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_NOT_FOUND, "")
      )
      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest should be(null)
      filterChain.getLastResponse should be(null)

      response.getErrorCode shouldBe HttpServletResponse.SC_UNAUTHORIZED
      response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString) shouldBe "Keystone uri=https://some.identity.com"
    }

    it("rejects with 403 if no x-auth-token is present") {
      //No auth token, no interactions with identity at all!
      val request = new MockHttpServletRequest()

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest should be(null)
      filterChain.getLastResponse should be(null)

      response.getErrorCode shouldBe HttpServletResponse.SC_FORBIDDEN
      response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString) shouldBe "Keystone uri=https://some.identity.com"
    }

    it("retries authentication as the admin user if the admin token is not valid") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Our admin token is good every time
      mockAkkaPostResponses {
        Seq(
          AkkaServiceClientResponse(HttpServletResponse.SC_OK, adminAuthenticationTokenResponse()),
          AkkaServiceClientResponse(HttpServletResponse.SC_OK, adminAuthenticationTokenResponse(token = "morty"))
        )
      }

      //When validating a token, we're going to not be authorized the first time,
      // Then we'll be authorized
      mockAkkaGetResponses(s"$TOKEN_KEY_PREFIX$VALID_TOKEN") {
        Seq(
          "glibglob" -> AkkaServiceClientResponse(HttpServletResponse.SC_UNAUTHORIZED, ""),
          "morty" -> AkkaServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse())
        )
      }
      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)
    }

    it("rejects with 500 if the admin token is not authorized to validate tokens") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Our admin token is good every time
      mockAkkaPostResponses {
        Seq(
          AkkaServiceClientResponse(HttpServletResponse.SC_OK, adminAuthenticationTokenResponse())
        )
      }

      //When validating a token, we're going to not be authorized the first time,
      // Then we'll be authorized
      mockAkkaGetResponses(s"$TOKEN_KEY_PREFIX$VALID_TOKEN") {
        Seq(
          "glibglob" -> AkkaServiceClientResponse(HttpServletResponse.SC_FORBIDDEN, "")
        )
      }
      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getErrorCode shouldBe HttpServletResponse.SC_INTERNAL_SERVER_ERROR

      filterChain.getLastRequest should be(null)
      filterChain.getLastResponse should be(null)
    }

    it("rejects with 502 if we cannot reach identity") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Our admin token is good every time
      //Need to throw an exception from akka when trying to talk to it
      //The admin token retry logic doesn't retry when it's a 500 class error
      mockAkkaPostResponse {
        AkkaServiceClientResponse.failure("Unable to reach identity!")
      }

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getErrorCode shouldBe HttpServletResponse.SC_BAD_GATEWAY

      filterChain.getLastRequest should be(null)
      filterChain.getLastResponse should be(null)

    }

    it("rejects with 502 if we cannot authenticate as the admin user") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Our admin token is good every time
      mockAkkaPostResponses {
        Seq(
          AkkaServiceClientResponse(HttpServletResponse.SC_OK, adminAuthenticationTokenResponse())
        )
      }

      //When validating a token, we're going to not be authorized the first time,
      // Then we'll be authorized
      mockAkkaGetResponses(s"$TOKEN_KEY_PREFIX$VALID_TOKEN") {
        Seq(
          "glibglob" -> AkkaServiceClientResponse.failure("Unable to talk to identity!")
        )
      }
      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getErrorCode shouldBe HttpServletResponse.SC_BAD_GATEWAY

      filterChain.getLastRequest should be(null)
      filterChain.getLastResponse should be(null)
    }
  }

  describe("Configured to authenticate and authorize a specific service endpoint") {
    def configuration = Marshaller.keystoneV2ConfigFromString(
      """<?xml version="1.0" encoding="UTF-8"?>
        |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
        |    <identity-service
        |            username="admin_username"
        |            password="password"
        |            uri="https://some.identity.com"
        |            set-groups-in-header="false"
        |            set-catalog-in-header="false"
        |            />
        |
        |    <require-service-endpoint public-url="https://compute.north.public.com/v1" region="Global" name="Compute" type="compute">
        |        <bypass-validation-roles>
        |            <role>serviceAdmin</role>
        |            <role>racker</role>
        |        </bypass-validation-roles>
        |    </require-service-endpoint>
        |
        |</keystone-v2>
      """.stripMargin)

    val filter: KeystoneV2Filter = new KeystoneV2Filter(mockConfigService, mockAkkaServiceClient, mockDatastoreService)

    val config: MockFilterConfig = new MockFilterConfig
    filter.init(config)
    filter.configurationUpdated(configuration)

    it("allows a user through if they have the endpoint configured in their endpoints list") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      mockAkkaGetResponse(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse())
      )
      mockAkkaGetResponse(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, endpointsResponse())
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      //Continues with the chain
      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)
    }

    it("handles 203 response from endpoints call") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(), Nil: _*)

      mockAkkaGetResponse(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION, endpointsResponse())
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      //Continues with the chain
      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)
    }

    it("handles 403 response from endpoints call") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(TestValidToken(), Nil: _*)

      mockAkkaGetResponse(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_FORBIDDEN, "")
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest should be(null)
      filterChain.getLastResponse should be(null)

      response.getErrorCode shouldBe HttpServletResponse.SC_FORBIDDEN
    }

    it("handles 401 response from endpoints call") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(TestValidToken(), Nil: _*)

      mockAkkaGetResponses(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")(
        Seq(
          "glibglob" -> AkkaServiceClientResponse(HttpServletResponse.SC_UNAUTHORIZED, ""),
          "glibglob" -> AkkaServiceClientResponse(HttpServletResponse.SC_UNAUTHORIZED, "")
        )
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getErrorCode shouldBe HttpServletResponse.SC_FORBIDDEN
    }

    it("Tests failure case when serviceClientResponse.getStatus fails") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(TestValidToken(), Nil: _*)

      mockAkkaGetResponse(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse.failure("Unable to reach identity!")
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      //Continues with the chain
      filterChain.getLastRequest shouldBe null
      filterChain.getLastResponse shouldBe null

      response.getErrorCode shouldBe HttpServletResponse.SC_FORBIDDEN
    }

    it("handles unexpected response from endpoints call") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(TestValidToken(), Nil: _*)

      mockAkkaGetResponses(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")(
        Seq(
          "glibglob" -> AkkaServiceClientResponse(HttpServletResponse.SC_NOT_IMPLEMENTED, "")
        )
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest shouldBe null
      filterChain.getLastResponse shouldBe null

      response.getErrorCode shouldBe HttpServletResponse.SC_FORBIDDEN
    }

    it("rejects with 403 if the user does not have the required endpoint") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(TestValidToken(), Nil: _*)
      mockAkkaGetResponse(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, oneEndpointResponse())
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getErrorCode shouldBe HttpServletResponse.SC_FORBIDDEN
      //Continues with the chain
      filterChain.getLastRequest should be(null)
      filterChain.getLastResponse should be(null)
    }

    it("bypasses validation if the user has the role listed in bypass-validation-roles") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      mockAkkaGetResponse(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, validateRackerTokenResponse())
      )
      mockAkkaGetResponse(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, oneEndpointResponse())
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)
    }

    it("rejects with 403 if the user does not have an endpoint when catalog variable is set") {
      val modifiedConfig = configuration
      modifiedConfig.setRequireServiceEndpoint(null)
      modifiedConfig.getIdentityService.setSetCatalogInHeader(true)
      filter.configurationUpdated(modifiedConfig)

      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(TestValidToken(), Nil: _*)

      mockAkkaGetResponse(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, groupsResponse())
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.configurationUpdated(configuration)

      response.getErrorCode shouldBe HttpServletResponse.SC_FORBIDDEN
      //Continues with the chain
      filterChain.getLastRequest should be(null)
      filterChain.getLastResponse should be(null)
    }

    describe("when endpoints are cached") {
      it("will reject if the user doesn't have the endpoint") {
        //make a request and validate that it called the akka service client?
        val request = new MockHttpServletRequest()
        request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

        //Pretend like the admin token is cached all the time
        when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

        mockAkkaGetResponse(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")(
          "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse())
        )

        val endpointsList = Vector(Endpoint(Some("DERP"), Some("Compute"), Some("compute"), "https://compute.north.public.com/v1"))
        when(mockDatastore.get(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")).thenReturn(EndpointsData("", endpointsList), Nil: _*)

        val response = new MockHttpServletResponse
        val filterChain = new MockFilterChain()
        filter.doFilter(request, response, filterChain)

        response.getErrorCode shouldBe HttpServletResponse.SC_FORBIDDEN
        filterChain.getLastRequest should be(null)
        filterChain.getLastResponse should be(null)
      }

      it("will allow through if the user has the endpoint") {
        //make a request and validate that it called the akka service client?
        val request = new MockHttpServletRequest()
        request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

        //Pretend like the admin token is cached all the time
        when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

        mockAkkaGetResponse(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")(
          "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse())
        )

        val endpointsList = Vector(Endpoint(Some("Global"), Some("Compute"), Some("compute"), "https://compute.north.public.com/v1"))
        when(mockDatastore.get(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")).thenReturn(EndpointsData("", endpointsList), Nil: _*)

        val response = new MockHttpServletResponse
        val filterChain = new MockFilterChain()
        filter.doFilter(request, response, filterChain)

        filterChain.getLastRequest shouldNot be(null)
        filterChain.getLastResponse shouldNot be(null)
      }

      it("will allow through if the user matches the bypass roles") {
        //make a request and validate that it called the akka service client?
        val request = new MockHttpServletRequest()
        request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

        //Pretend like the admin token is cached all the time
        when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

        mockAkkaGetResponse(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")(
          "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, validateRackerTokenResponse())
        )

        val endpointsList = Vector(Endpoint(Some("DERP"), Some("LOLNOPE"), Some("compute"), "https://compute.north.public.com/v1"))
        when(mockDatastore.get(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")).thenReturn(EndpointsData("", endpointsList), Nil: _*)

        val response = new MockHttpServletResponse
        val filterChain = new MockFilterChain()
        filter.doFilter(request, response, filterChain)

        filterChain.getLastRequest shouldNot be(null)
        filterChain.getLastResponse shouldNot be(null)
      }
    }
  }

  describe("Configured to send groups") {
    def configuration = Marshaller.keystoneV2ConfigFromString(
      """<?xml version="1.0" encoding="UTF-8"?>
        |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
        |    <identity-service
        |            username="username"
        |            password="password"
        |            uri="https://some.identity.com"
        |            />
        |</keystone-v2>
      """.stripMargin)

    val filter: KeystoneV2Filter = new KeystoneV2Filter(mockConfigService, mockAkkaServiceClient, mockDatastoreService)

    val config: MockFilterConfig = new MockFilterConfig
    filter.init(config)
    filter.configurationUpdated(configuration)

    it("will allow through if the user has the group cached") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      mockAkkaGetResponse(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse())
      )

      val groupsList = Vector("DERP")
      when(mockDatastore.get(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")).thenReturn(groupsList, Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)

      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(PowerApiHeader.GROUPS.toString) shouldBe "DERP"
    }

    it("Tests failure case in getGroupsForToken when serviceClientResponse.getStatus fails") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(TestValidToken(), Nil: _*)

      mockAkkaGetResponse(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse.failure("Unable to reach identity!")
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(PowerApiHeader.GROUPS.toString) shouldBe null
    }

    it("handles 401 response from groups call") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(TestValidToken(), Nil: _*)

      mockAkkaGetResponses(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")(
        Seq(
          "glibglob" -> AkkaServiceClientResponse(HttpServletResponse.SC_UNAUTHORIZED, ""),
          "glibglob" -> AkkaServiceClientResponse(HttpServletResponse.SC_UNAUTHORIZED, "")
        )
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(PowerApiHeader.GROUPS.toString) shouldBe null
    }

    it("handles 403 response from groups call") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(TestValidToken(), Nil: _*)

      mockAkkaGetResponse(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")(
          "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_FORBIDDEN, "")
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(PowerApiHeader.GROUPS.toString) shouldBe null
    }

    it("handles 413 response from groups call") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(TestValidToken(), Nil: _*)

      mockAkkaGetResponse(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")(
          "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "")
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(PowerApiHeader.GROUPS.toString) shouldBe null
    }

    it("handles 429 response from groups call") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(TestValidToken(), Nil: _*)

      mockAkkaGetResponse(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")(
          "glibglob", AkkaServiceClientResponse(SC_TOO_MANY_REQUESTS, "")
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(PowerApiHeader.GROUPS.toString) shouldBe null
    }

    it("handles unexpected response from groups call") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(TestValidToken(), Nil: _*)

      mockAkkaGetResponse(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_NOT_IMPLEMENTED, "")
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(PowerApiHeader.GROUPS.toString) shouldBe null
    }
  }

  describe("when delegating") {
    //Configure the filter
    def configuration = Marshaller.keystoneV2ConfigFromString(
      """<?xml version="1.0" encoding="UTF-8"?>
        |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
        |    <identity-service
        |            username="username"
        |            password="password"
        |            uri="https://some.identity.com"
        |            />
        |
        |     <delegating/>
        |</keystone-v2>
      """.stripMargin)

    val filter: KeystoneV2Filter = new KeystoneV2Filter(mockConfigService, mockAkkaServiceClient, mockDatastoreService)

    val config: MockFilterConfig = new MockFilterConfig
    filter.init(config)
    filter.configurationUpdated(configuration)

    it("delegates with an invalid token and adds the header") {
      val request = new MockHttpServletRequest
      request.setRequestURL("http://www.sample.com/some/path/application.wadl")
      request.setRequestURI("/some/path/application.wadl")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, "INVALID_TOKEN")

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)
      when(mockDatastore.get(s"${TOKEN_KEY_PREFIX}INVALID_TOKEN")).thenReturn(InvalidToken, Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      val delegationHeader = parseDelegationHeader(filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(HttpDelegationHeaderNames.Delegated))
      delegationHeader shouldBe a[Success[_]]
      delegationHeader.get.statusCode shouldBe HttpServletResponse.SC_UNAUTHORIZED
    }

    it("delegates if lacking the required service endpoint and adds the header") {
      val modifiedConfig = configuration
      modifiedConfig.setRequireServiceEndpoint(new ServiceEndpointType().withPublicUrl("http://google.com/"))
      filter.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest
      request.setRequestURL("http://www.sample.com/some/path/application.wadl")
      request.setRequestURI("/some/path/application.wadl")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)
      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(), Nil: _*)
      when(mockDatastore.get(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")).thenReturn(EndpointsData("", Vector.empty[Endpoint]), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.configurationUpdated(configuration)

      val delegationHeader = parseDelegationHeader(filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(HttpDelegationHeaderNames.Delegated))
      delegationHeader shouldBe a[Success[_]]
      delegationHeader.get.statusCode shouldBe HttpServletResponse.SC_FORBIDDEN
    }

    it("delegates if identity doesn't respond properly") {
      val request = new MockHttpServletRequest
      request.setRequestURL("http://www.sample.com/some/path/application.wadl")
      request.setRequestURI("/some/path/application.wadl")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      mockAkkaGetResponse(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, "butts")
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      val delegationHeader = parseDelegationHeader(filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(HttpDelegationHeaderNames.Delegated))
      delegationHeader shouldBe a[Success[_]]
      delegationHeader.get.statusCode shouldBe HttpServletResponse.SC_BAD_GATEWAY
    }

    it("delegates if the admin token is invalid") {
      val request = new MockHttpServletRequest
      request.setRequestURL("http://www.sample.com/some/path/application.wadl")
      request.setRequestURI("/some/path/application.wadl")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("invalid", null)

      mockAkkaGetResponse(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")(
        "invalid", AkkaServiceClientResponse(HttpServletResponse.SC_UNAUTHORIZED, "")
      )

      mockAkkaPostResponse(
        AkkaServiceClientResponse(HttpServletResponse.SC_NOT_FOUND, "")
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      val delegationHeader = parseDelegationHeader(filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(HttpDelegationHeaderNames.Delegated))
      delegationHeader shouldBe a[Success[_]]
      delegationHeader.get.statusCode shouldBe HttpServletResponse.SC_INTERNAL_SERVER_ERROR
    }

    it("forwards the identity status as Indeterminate in the x-identity-status header when delegating") {
      val request = new MockHttpServletRequest
      request.setRequestURL("http://www.sample.com/some/path/application.wadl")
      request.setRequestURI("/some/path/application.wadl")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, "INVALID_TOKEN")

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)
      when(mockDatastore.get(s"${TOKEN_KEY_PREFIX}INVALID_TOKEN")).thenReturn(InvalidToken, Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.IDENTITY_STATUS.toString) shouldBe IdentityStatus.Indeterminate.toString
    }
  }

  describe("when whitelist is configured for a particular URI") {
    def configuration = Marshaller.keystoneV2ConfigFromString(
      """<?xml version="1.0" encoding="UTF-8"?>
        |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
        |    <identity-service
        |            username="admin_username"
        |            password="password"
        |            uri="https://some.identity.com"
        |            />
        |
        |    <white-list>
        |        <uri-regex>.*/application\.wadl$</uri-regex>
        |        <uri-regex>/some/endpoint</uri-regex>
        |    </white-list>
        |</keystone-v2>
      """.stripMargin)

    val filter: KeystoneV2Filter = new KeystoneV2Filter(mockConfigService, mockAkkaServiceClient, mockDatastoreService)

    val config: MockFilterConfig = new MockFilterConfig
    filter.init(config)
    filter.configurationUpdated(configuration)

    val testUris = Seq("/some/path/application.wadl", "/some/other/path/application.wadl", "/some/endpoint")
    testUris foreach { uri =>
      it(s"will not perform authentication or authorization if the URI matches the whitelist: $uri") {
        //make a request and validate that it called the akka service client?
        val request = new MockHttpServletRequest
        request.setRequestURI(uri)

        val response = new MockHttpServletResponse
        val filterChain = new MockFilterChain()
        filter.doFilter(request, response, filterChain)

        filterChain.getLastRequest shouldNot be(null)
        filterChain.getLastResponse shouldNot be(null)
      }
    }
  }

  describe("configuring timeouts") {
    def configuration = Marshaller.keystoneV2ConfigFromString(
      """<?xml version="1.0" encoding="UTF-8"?>
        |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
        |    <identity-service
        |            username="username"
        |            password="password"
        |            uri="https://some.identity.com"
        |            set-catalog-in-header="true"
        |            />
        |      <cache>
        |          <timeouts>
        |              <token>270</token>
        |              <group>300</group>
        |              <endpoints>330</endpoints>
        |          </timeouts>
        |      </cache>
        |</keystone-v2>
      """.stripMargin)

    val filter: KeystoneV2Filter = new KeystoneV2Filter(mockConfigService, mockAkkaServiceClient, mockDatastoreService)

    val config: MockFilterConfig = new MockFilterConfig
    filter.init(config)
    filter.configurationUpdated(configuration)

    it("passes through the values to the distributed datastore for the proper cache timeouts") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn(null, "glibglob")

      mockAkkaPostResponse(
        AkkaServiceClientResponse(HttpServletResponse.SC_OK, adminAuthenticationTokenResponse())
      )

      mockAkkaGetResponse(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse())
      )

      mockAkkaGetResponse(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, endpointsResponse())
      )

      mockAkkaGetResponse(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, groupsResponse())
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)
      verify(mockDatastore).put(mockitoEq(s"$ADMIN_TOKEN_KEY"), any())
      verify(mockDatastore).put(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), any(), mockitoEq(270), mockitoEq(TimeUnit.SECONDS))
      verify(mockDatastore).put(mockitoEq(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN"), any(), mockitoEq(330), mockitoEq(TimeUnit.SECONDS))
      verify(mockDatastore).put(mockitoEq(s"$GROUPS_KEY_PREFIX$VALID_TOKEN"), any(), mockitoEq(300), mockitoEq(TimeUnit.SECONDS))
    }

    it("passes through variable offsets within a range to the distributed datastore") {
      val modifiedConfig = configuration
      modifiedConfig.getCache.getTimeouts.setVariability(1)
      filter.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn(null, "glibglob")

      mockAkkaPostResponse(
        AkkaServiceClientResponse(HttpServletResponse.SC_OK, adminAuthenticationTokenResponse())
      )

      mockAkkaGetResponse(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse())
      )

      mockAkkaGetResponse(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, endpointsResponse())
      )

      mockAkkaGetResponse(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, groupsResponse())
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.configurationUpdated(configuration)

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)
      verify(mockDatastore).put(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"),
        any(),
        intThat(both(greaterThanOrEqualTo(Int.box(269))).and(lessThanOrEqualTo(Int.box(271)))),
        mockitoEq(TimeUnit.SECONDS))
      verify(mockDatastore).put(mockitoEq(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN"),
        any(),
        intThat(both(greaterThanOrEqualTo(Int.box(329))).and(lessThanOrEqualTo(Int.box(331)))),
        mockitoEq(TimeUnit.SECONDS))
      verify(mockDatastore).put(mockitoEq(s"$GROUPS_KEY_PREFIX$VALID_TOKEN"),
        any(),
        intThat(both(greaterThanOrEqualTo(Int.box(299))).and(lessThanOrEqualTo(Int.box(301)))),
        mockitoEq(TimeUnit.SECONDS))
    }

    it("tests that configurationUpdated sets timeouts to default if CacheTimeoutType is null") {
      val modifiedConfig = configuration
      modifiedConfig.getCache.setTimeouts(null)
      filter.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn(null, "glibglob")

      mockAkkaPostResponse(
        AkkaServiceClientResponse(HttpServletResponse.SC_OK, adminAuthenticationTokenResponse())
      )

      mockAkkaGetResponse(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse())
      )

      mockAkkaGetResponse(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, endpointsResponse())
      )

      mockAkkaGetResponse(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, groupsResponse())
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)
      verify(mockDatastore).put(mockitoEq(s"$ADMIN_TOKEN_KEY"), any())
      verify(mockDatastore).put(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), any(), mockitoEq(600), mockitoEq(TimeUnit.SECONDS))
      verify(mockDatastore).put(mockitoEq(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN"), any(), mockitoEq(600), mockitoEq(TimeUnit.SECONDS))
      verify(mockDatastore).put(mockitoEq(s"$GROUPS_KEY_PREFIX$VALID_TOKEN"), any(), mockitoEq(600), mockitoEq(TimeUnit.SECONDS))

      filter.configurationUpdated(configuration)
    }
  }

  describe("when tenant handling is enabled") {
    def configuration = Marshaller.keystoneV2ConfigFromString(
      """<?xml version="1.0" encoding="UTF-8"?>
        |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
        |    <identity-service
        |            username="username"
        |            password="password"
        |            uri="https://some.identity.com"
        |            set-groups-in-header="false"
        |            />
        |    <tenant-handling send-all-tenant-ids="true">
        |        <validate-tenant>
        |            <uri-extraction-regex>/(\w+)/.*</uri-extraction-regex>
        |            <bypass-validation-roles>
        |                <role>serviceAdmin</role>
        |                <role>racker</role>
        |            </bypass-validation-roles>
        |        </validate-tenant>
        |        <send-tenant-id-quality default-tenant-quality="0.9" uri-tenant-quality="0.7" roles-tenant-quality="0.5"/>
        |    </tenant-handling>
        |</keystone-v2>
      """.stripMargin)

    val filter: KeystoneV2Filter = new KeystoneV2Filter(mockConfigService, mockAkkaServiceClient, mockDatastoreService)

    val config: MockFilterConfig = new MockFilterConfig
    filter.init(config)
    filter.configurationUpdated(configuration)

    it("will extract the tenant from the URI and validate that the user has that tenant in their list") {
      val request = new MockHttpServletRequest()
      request.setRequestURL("http://www.sample.com/tenant/test")
      request.setRequestURI("/tenant/test")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(defaultTenantId = "tenant"), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)
    }

    it("will extract the tenant from the URI and reject if the user does not have that tenant in their list") {
      val request = new MockHttpServletRequest()
      request.setRequestURL("http://www.sample.com/tenant/test")
      request.setRequestURI("/tenant/test")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(defaultTenantId = "not-tenant"), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.wasErrorSent shouldBe true
      response.getErrorCode shouldBe HttpServletResponse.SC_UNAUTHORIZED
    }

    it("sends all tenant IDs when configured to") {
      val modifiedConfig = configuration
      modifiedConfig.getTenantHandling.setSendTenantIdQuality(null)
      filter.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest()
      request.setRequestURL("http://www.sample.com/tenant/test")
      request.setRequestURI("/tenant/test")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(defaultTenantId = "tenant", tenantIds = Seq("rick", "morty")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.configurationUpdated(configuration)

      val processedRequest = filterChain.getLastRequest.asInstanceOf[HttpServletRequest]
      processedRequest.getHeader(OpenStackServiceHeader.TENANT_ID.toString) should include("tenant")
      processedRequest.getHeader(OpenStackServiceHeader.TENANT_ID.toString) should include("rick")
      processedRequest.getHeader(OpenStackServiceHeader.TENANT_ID.toString) should include("morty")
    }

    it("sends all tenant IDs with a quality when all three are configured") {
      val request = new MockHttpServletRequest()
      request.setRequestURL("http://www.sample.com/rick/test")
      request.setRequestURI("/rick/test")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(defaultTenantId = "tenant", tenantIds = Seq("rick", "morty")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      val processedRequest = filterChain.getLastRequest.asInstanceOf[HttpServletRequest]
      processedRequest.getHeader(OpenStackServiceHeader.TENANT_ID.toString) should include("tenant;q=0.9")
      processedRequest.getHeader(OpenStackServiceHeader.TENANT_ID.toString) should include("rick;q=0.7")
      processedRequest.getHeader(OpenStackServiceHeader.TENANT_ID.toString) should include("morty;q=0.5")
    }

    it("sends tenant quality when not configured to send all tenant IDs") {
      val modifiedConfig = configuration
      modifiedConfig.getTenantHandling.setSendAllTenantIds(false)
      filter.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest()
      request.setRequestURL("http://www.sample.com/rick/test")
      request.setRequestURI("/rick/test")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(tenantIds = Seq("rick", "morty")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.configurationUpdated(configuration)

      val processedRequest = filterChain.getLastRequest.asInstanceOf[HttpServletRequest]
      processedRequest.getHeaders(OpenStackServiceHeader.TENANT_ID.toString).asScala.size shouldBe 1
      processedRequest.getHeader(OpenStackServiceHeader.TENANT_ID.toString) shouldBe "rick;q=0.7"
    }

    it("bypasses the URI tenant validation check when a user has a role in the bypass-validation-roles list") {
      val request = new MockHttpServletRequest()
      request.setRequestURL("http://www.sample.com/tenant/test")
      request.setRequestURI("/tenant/test")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(defaultTenantId = "not-tenant", roles = Seq("racker")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)
    }

    it("sends the tenant matching the URI when send all tenants is false and validate-tenant is enabled") {
      val modifiedConfig = configuration
      modifiedConfig.getTenantHandling.setSendAllTenantIds(false)
      modifiedConfig.getTenantHandling.setSendTenantIdQuality(null)
      filter.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest()
      request.setRequestURL("http://www.sample.com/morty/test")
      request.setRequestURI("/morty/test")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(defaultTenantId = "tenant", tenantIds = Seq("rick", "morty")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.configurationUpdated(configuration)

      val processedRequest = filterChain.getLastRequest.asInstanceOf[HttpServletRequest]
      processedRequest.getHeaders(OpenStackServiceHeader.TENANT_ID.toString).asScala.size shouldBe 1
      processedRequest.getHeader(OpenStackServiceHeader.TENANT_ID.toString) shouldBe "morty"
    }

    it("sends the user's default tenant, if validate-tenant is not enabled") {
      val modifiedConfig = configuration
      modifiedConfig.getTenantHandling.setValidateTenant(null)
      modifiedConfig.getTenantHandling.setSendAllTenantIds(false)
      modifiedConfig.getTenantHandling.setSendTenantIdQuality(null)
      filter.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest()
      request.setRequestURL("http://www.sample.com/years/test")
      request.setRequestURI("/years/test")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(defaultTenantId = "one", tenantIds = Seq("hundred", "years")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.configurationUpdated(configuration)

      val processedRequest = filterChain.getLastRequest.asInstanceOf[HttpServletRequest]
      processedRequest.getHeaders(OpenStackServiceHeader.TENANT_ID.toString).asScala.size shouldBe 1
      processedRequest.getHeader(OpenStackServiceHeader.TENANT_ID.toString) shouldBe "one"
    }

    it("should return a failure if a tenant could not be parsed from the URI") {
      val request = new MockHttpServletRequest()
      request.setRequestURL("http://www.sample.com/bu-%tts/test")
      request.setRequestURI("/bu-%tts/test")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(defaultTenantId = "bu-%tts"), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.wasErrorSent shouldBe true
      response.getErrorCode shouldBe HttpServletResponse.SC_UNAUTHORIZED
    }

    it("should send the X-Authorization header with the tenant in the uri") {
      val request = new MockHttpServletRequest()
      request.setRequestURL("http://www.sample.com/years/test")
      request.setRequestURI("/years/test")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(tenantIds = Seq("hundred", "years")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      val processedRequest = filterChain.getLastRequest.asInstanceOf[HttpServletRequest]
      processedRequest.getHeader(OpenStackServiceHeader.EXTENDED_AUTHORIZATION.toString) shouldBe "Proxy years"
    }

    it("should send the X-Authorization header without a tenant if tenant handling is not used") {
      val modifiedConfig = configuration
      modifiedConfig.setTenantHandling(null)
      filter.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest()
      request.setRequestURL("http://www.sample.com/years/test")
      request.setRequestURI("/years/test")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(tenantIds = Seq("hundred", "years")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.configurationUpdated(configuration)

      val processedRequest = filterChain.getLastRequest.asInstanceOf[HttpServletRequest]
      processedRequest.getHeader(OpenStackServiceHeader.EXTENDED_AUTHORIZATION.toString) shouldBe "Proxy"
    }

    it("should send the X-Authorization header without a tenant if tenant validation is not used") {
      val modifiedConfig = configuration
      modifiedConfig.getTenantHandling.setValidateTenant(null)
      filter.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest()
      request.setRequestURL("http://www.sample.com/years/test")
      request.setRequestURI("/years/test")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(tenantIds = Seq("hundred", "years")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.configurationUpdated(configuration)

      val processedRequest = filterChain.getLastRequest.asInstanceOf[HttpServletRequest]
      processedRequest.getHeader(OpenStackServiceHeader.EXTENDED_AUTHORIZATION.toString) shouldBe "Proxy"
    }
  }

  describe("Forwarding information enabled") {
    //Configure the filter
    def configuration = Marshaller.keystoneV2ConfigFromString(
      """<?xml version="1.0" encoding="UTF-8"?>
        |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
        |    <identity-service
        |            username="username"
        |            password="password"
        |            uri="https://some.identity.com"
        |            />
        |</keystone-v2>
      """.stripMargin)

    val filter: KeystoneV2Filter = new KeystoneV2Filter(mockConfigService, mockAkkaServiceClient, mockDatastoreService)

    val config: MockFilterConfig = new MockFilterConfig
    filter.init(config)
    filter.configurationUpdated(configuration)

    it("forwards the user information in the x-pp-user, x-user-name, and x-user-id headers") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      mockAkkaGetResponse(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse())
      )

      mockAkkaGetResponse(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, groupsResponse())
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(PowerApiHeader.USER.toString) shouldBe "testuser"
      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.USER_NAME.toString) shouldBe "testuser"
      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.USER_ID.toString) shouldBe "123"
    }

    it("forwards the user's roles information in the x-roles header") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      mockAkkaGetResponse(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse())
      )

      mockAkkaGetResponse(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, groupsResponse())
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.ROLES.toString) should include("compute:admin")
      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.ROLES.toString) should include("object-store:admin")
    }

    it("forwards the user's contact id information in the x-contact-id header") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      mockAkkaGetResponse(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse())
      )

      mockAkkaGetResponse(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, groupsResponse())
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.CONTACT_ID.toString) shouldBe "abc123"
    }

    it("forwards the user's impersonator information in the x-impersonator-id and x-impersonator-name headers") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      mockAkkaGetResponse(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, validateImpersonatedTokenResponse())
      )

      mockAkkaGetResponse(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, groupsResponse())
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.IMPERSONATOR_ID.toString) shouldBe "567"
      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.IMPERSONATOR_NAME.toString) shouldBe "rick"
    }

    it("forwards the user's default region information in the x-default-region header") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      mockAkkaGetResponse(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse())
      )

      mockAkkaGetResponse(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, groupsResponse())
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.DEFAULT_REGION.toString) shouldBe "DFW"
    }

    it("forwards the expiration date information in the x-expiration header") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      mockAkkaGetResponse(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse(expires = dateTime))
      )

      mockAkkaGetResponse(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, groupsResponse())
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)


      filterChain.getLastRequest.asInstanceOf[HttpServletRequest]
        .getHeader(OpenStackServiceHeader.X_EXPIRATION.toString) shouldBe iso8601ToRfc1123(tokenDateFormat(dateTime))
    }

    it("forwards the identity status as Confirmed in the x-identity-status header when Repose is able to validate the token") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      mockAkkaGetResponse(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse())
      )

      mockAkkaGetResponse(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, groupsResponse())
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.IDENTITY_STATUS.toString) shouldBe IdentityStatus.Confirmed.toString
    }

    it("forwards the groups in the x-pp-groups header by default") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      mockAkkaGetResponse(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse())
      )

      mockAkkaGetResponse(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, groupsResponse())
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(PowerApiHeader.GROUPS.toString) should include("test-group-id")
    }

    it("should not add the groups in the x-pp-groups header when RAX-KSGRP:groups not defined") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      mockAkkaGetResponse(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse())
      )

      mockAkkaGetResponse(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse())
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(PowerApiHeader.GROUPS.toString) shouldBe null
    }

    it("should not add the roles in the x-roles header when isSetRolesInHeader is false") {
      val modifiedConfig = configuration
      modifiedConfig.getIdentityService.setSetRolesInHeader(false)
      filter.configurationUpdated(modifiedConfig)
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      mockAkkaGetResponse(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse())
      )

      mockAkkaGetResponse(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, groupsResponse())
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.ROLES.toString) shouldBe null
    }

    it("forwards the user's catalog in x-catalog header base64 JSON encoded by default") {
      val modifiedConfig = configuration
      modifiedConfig.getIdentityService.setSetCatalogInHeader(true)
      modifiedConfig.getIdentityService.setSetGroupsInHeader(false)
      modifiedConfig.setRequireServiceEndpoint(new ServiceEndpointType().withPublicUrl("example.com"))
      filter.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)
      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(), Nil: _*)
      when(mockDatastore.get(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")).thenReturn(EndpointsData(endpointsResponse(), Vector(Endpoint(None, None, None, "example.com"))), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.configurationUpdated(configuration)

      val encodedEndpoints = Base64.encodeBase64String(endpointsResponse().getBytes)
      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(PowerApiHeader.X_CATALOG.toString) shouldBe encodedEndpoints
    }
  }

  object TestValidToken {
    def apply(expirationDate: String = "",
              userId: String = "",
              username: String = "",
              tenantName: String = "",
              defaultTenantId: String = "",
              tenantIds: Seq[String] = Seq.empty[String],
              roles: Seq[String] = Seq.empty[String],
              impersonatorId: Option[String] = Option.empty[String],
              impersonatorName: Option[String] = Option.empty[String],
              defaultRegion: Option[String] = None,
              contactId: Option[String] = None) = {
      ValidToken(expirationDate,
        userId,
        username,
        tenantName,
        defaultTenantId,
        tenantIds,
        roles,
        impersonatorId,
        impersonatorName,
        defaultRegion,
        contactId)
    }
  }

}
