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

import java.io.{ByteArrayInputStream, InputStream}
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.servlet.{Servlet, ServletRequest, ServletResponse}
import javax.ws.rs.core.MediaType

import com.mockrunner.mock.web.{MockFilterChain, MockFilterConfig, MockHttpServletRequest, MockHttpServletResponse}
import com.rackspace.httpdelegation.{HttpDelegationHeaderNames, HttpDelegationManager}
import org.apache.commons.codec.binary.Base64
import org.apache.http.client.utils.DateUtils
import org.apache.http.message.BasicHeader
import org.apache.http.{Header, HttpHeaders}
import org.hamcrest.Matchers.{both, greaterThanOrEqualTo, hasEntry, lessThanOrEqualTo}
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.mockito.AdditionalMatchers._
import org.mockito.Matchers.{eq => mockitoEq, _}
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.openrepose.commons.utils.http._
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.types.SetPatch
import org.openrepose.core.services.datastore.{Datastore, DatastoreService}
import org.openrepose.core.services.serviceclient.akka.{AkkaServiceClient, AkkaServiceClientException, AkkaServiceClientFactory}
import org.openrepose.core.systemmodel.{SystemModel, TracingHeaderConfig}
import org.openrepose.filters.keystonev2.KeystoneRequestHandler._
import org.openrepose.filters.keystonev2.config.{KeystoneV2Config, ServiceEndpointType}
import org.openrepose.nodeservice.atomfeed.AtomFeedService
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec}

import scala.collection.JavaConverters._
import scala.language.implicitConversions
import scala.util.Success

@RunWith(classOf[JUnitRunner])
class KeystoneV2FilterTest extends FunSpec
with org.scalatest.Matchers
with BeforeAndAfterEach
with MockitoSugar
with IdentityResponses
with HttpDelegationManager {

  private final val dateTime = DateTime.now().plusHours(1)
  private val mockAkkaServiceClient = mock[AkkaServiceClient]
  private val mockAkkaServiceClientFactory = mock[AkkaServiceClientFactory]
  when(mockAkkaServiceClientFactory.newAkkaServiceClient(or(anyString(), isNull.asInstanceOf[String]))).thenReturn(mockAkkaServiceClient)
  private val mockDatastore = mock[Datastore]
  private val mockDatastoreService = mock[DatastoreService]
  private val mockConfigurationService = mock[ConfigurationService]
  when(mockDatastoreService.getDefaultDatastore).thenReturn(mockDatastore)
  private val mockSystemModel = mock[SystemModel]
  private val mockTracingHeader = mock[TracingHeaderConfig]
  when(mockSystemModel.getTracingHeader).thenReturn(mockTracingHeader)
  when(mockTracingHeader.isEnabled).thenReturn(true, Nil: _*)
  private val mockFilterConfig = new MockFilterConfig

  override def beforeEach() = {
    reset(mockDatastore)
    reset(mockConfigurationService)
    reset(mockAkkaServiceClient)
  }

  describe("Filter lifecycle") {
    val filter = new KeystoneV2Filter(mockConfigurationService, mockAkkaServiceClientFactory, mock[AtomFeedService], mockDatastoreService)

    it("should throw 503 if filter is not initialized") {
      val request = new MockHttpServletRequest
      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain
      filter.isInitialized shouldBe false
      filter.doFilter(request, response, filterChain)
      response.getErrorCode shouldBe HttpServletResponse.SC_SERVICE_UNAVAILABLE
    }

    it("should subscribe a listener to the configuration service on init") {
      filter.init(mockFilterConfig)

      verify(mockConfigurationService).subscribeTo(
        anyString(),
        anyString(),
        any[URL],
        any(),
        any[Class[KeystoneV2Config]]
      )
      verify(mockConfigurationService).subscribeTo(
        anyString(),
        any[URL],
        any(),
        any[Class[SystemModel]]
      )
    }

    it("should unsubscribe a listener to the configuration service on destroy") {
      filter.destroy()

      verify(mockConfigurationService, times(2)).unsubscribeFrom(
        anyString(),
        any()
      )
    }

    it("should destroy the akka service client when filter is destroyed") {
      def configuration = Marshaller.keystoneV2ConfigFromString(
        """<?xml version="1.0" encoding="UTF-8"?>
          |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
          |    <identity-service uri="https://some.identity.com"/>
          |</keystone-v2>
        """.stripMargin)

      val mockAkkaClient = mock[AkkaServiceClient]
      val mockAkkaClientFactory = mock[AkkaServiceClientFactory]
      when(mockAkkaClientFactory.newAkkaServiceClient(or(anyString(), isNull.asInstanceOf[String]))).thenReturn(mockAkkaClient)
      val testFilter = new KeystoneV2Filter(mockConfigurationService, mockAkkaClientFactory, mock[AtomFeedService], mockDatastoreService)
      testFilter.KeystoneV2ConfigListener.configurationUpdated(configuration)

      testFilter.destroy()

      verify(mockAkkaClient, times(1)).destroy()
    }

    it("should destroy the previous akka service client when configuration is updated") {
      def configuration = Marshaller.keystoneV2ConfigFromString(
        """<?xml version="1.0" encoding="UTF-8"?>
          |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
          |    <identity-service uri="https://some.identity.com"/>
          |</keystone-v2>
        """.stripMargin)

      val firstAkkaServiceClient = mock[AkkaServiceClient]
      val secondAkkaServiceClient = mock[AkkaServiceClient]
      val mockAkkaClientFactory = mock[AkkaServiceClientFactory]
      when(mockAkkaClientFactory.newAkkaServiceClient(or(anyString(), isNull.asInstanceOf[String])))
        .thenReturn(firstAkkaServiceClient)
        .thenReturn(secondAkkaServiceClient)
      val testFilter = new KeystoneV2Filter(mockConfigurationService, mockAkkaClientFactory, mock[AtomFeedService], mockDatastoreService)

      testFilter.KeystoneV2ConfigListener.configurationUpdated(configuration)
      testFilter.KeystoneV2ConfigListener.configurationUpdated(configuration)

      verify(mockAkkaClientFactory, times(2)).newAkkaServiceClient(or(anyString(), isNull.asInstanceOf[String]))
      verify(firstAkkaServiceClient, times(1)).destroy()
      verify(secondAkkaServiceClient, never()).destroy()
    }
  }

  describe("Configured connection pool id") {
    it("asks the akka service client factory for an instance with the configured connection pool id") {
      def configuration = Marshaller.keystoneV2ConfigFromString(
        """<?xml version="1.0" encoding="UTF-8"?>
          |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
          |    <identity-service uri="https://some.identity.com" connection-pool-id="potato_pool"/>
          |</keystone-v2>
        """.stripMargin)

      val mockAkkaClient = mock[AkkaServiceClient]
      val mockAkkaClientFactory = mock[AkkaServiceClientFactory]
      when(mockAkkaClientFactory.newAkkaServiceClient(or(anyString(), isNull.asInstanceOf[String]))).thenReturn(mockAkkaClient)
      val filter = new KeystoneV2Filter(mockConfigurationService, mockAkkaClientFactory, mock[AtomFeedService], mockDatastoreService)
      filter.KeystoneV2ConfigListener.configurationUpdated(configuration)

      verify(mockAkkaClientFactory).newAkkaServiceClient("potato_pool")
    }
  }

  describe("Configured simply to authenticate tokens") {
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

    val filter = new KeystoneV2Filter(mockConfigurationService, mockAkkaServiceClientFactory, mock[AtomFeedService], mockDatastoreService)

    filter.init(mockFilterConfig)
    filter.KeystoneV2ConfigListener.configurationUpdated(configuration)
    filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

    it("Validates a token allowing through the filter") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn(null, "glibglob")
      when(mockAkkaServiceClient.post(anyString(), anyString(), anyMapOf(classOf[String], classOf[String]), anyString(), any[MediaType], anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, adminAuthenticationTokenResponse()))
      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse()))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)
    }

    it("should handle identity service uri ending with a '/'") {
      val mockAkkaClient = mock[AkkaServiceClient]
      val mockAkkaClientFactory = mock[AkkaServiceClientFactory]
      when(mockAkkaClientFactory.newAkkaServiceClient(or(anyString(), isNull.asInstanceOf[String]))).thenReturn(mockAkkaClient)
      val keystoneFilter = new KeystoneV2Filter(mockConfigurationService, mockAkkaClientFactory, mock[AtomFeedService], mockDatastoreService)

      val modifiedConfig = configuration
      modifiedConfig.getIdentityService.setUri("https://some.identity.com/")
      keystoneFilter.KeystoneV2ConfigListener.configurationUpdated(modifiedConfig)
      keystoneFilter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockAkkaClient.get(anyString(), anyString(), anyMapOf(classOf[String], classOf[String]), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse()))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      keystoneFilter.doFilter(request, response, filterChain)

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)
      verify(mockAkkaClient).get(anyString(), mockitoEq(s"https://some.identity.com$TOKEN_ENDPOINT/$VALID_TOKEN"), any(), anyBoolean())
    }

    it("caches the admin token request for 10 minutes") {
      //Can only make sure it was put into the cache with a 10 minute timeout...
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn(null, "glibglob")

      when(mockAkkaServiceClient.post(anyString(), anyString(), anyMapOf(classOf[String], classOf[String]), anyString(), any[MediaType], anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, adminAuthenticationTokenResponse()))
      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse()))

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

      when(mockAkkaServiceClient.post(anyString(), anyString(), anyMapOf(classOf[String], classOf[String]), anyString(), any[MediaType], anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, adminAuthenticationTokenResponse()))
      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse(expires = dateTime)))

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

    it("Makes no other calls if the token is already cached with a valid result") {
      //Can only make sure it was put into the cache with a 10 minute timeout...
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //When the user's token details are cached, no calls to identity should take place

      //When we ask the cache for our token, it works
      // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures
      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(roles = Vector(Role("compute:admin"), Role("object-store:admin"))), Nil: _*)
      // Doesn't update the User to Token cache.
      verify(mockDatastore, never()).put(any(), any(), mockitoEq(600), mockitoEq(TimeUnit.SECONDS))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)
    }

    it("rejects with 401 an invalid token") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, "notValidToken")

      when(mockAkkaServiceClient.post(anyString(), anyString(), anyMapOf(classOf[String], classOf[String]), anyString(), any[MediaType], anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, adminAuthenticationTokenResponse()))
      when(mockAkkaServiceClient.get(mockitoEq(s"${TOKEN_KEY_PREFIX}notValidToken"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_NOT_FOUND, ""))

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

      response.getErrorCode shouldBe HttpServletResponse.SC_UNAUTHORIZED
      response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString) shouldBe "Keystone uri=https://some.identity.com"
    }

    it("retries authentication as the admin user if the admin token is not valid") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockAkkaServiceClient.post(anyString(), anyString(), anyMapOf(classOf[String], classOf[String]), anyString(), any[MediaType], anyBoolean()))
        .thenReturn(
          new ServiceClientResponse(HttpServletResponse.SC_OK, adminAuthenticationTokenResponse()),
          new ServiceClientResponse(HttpServletResponse.SC_OK, adminAuthenticationTokenResponse(token = "morty"))
        )
      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_UNAUTHORIZED, ""))
      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "morty")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse()))

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

      when(mockAkkaServiceClient.post(anyString(), anyString(), anyMapOf(classOf[String], classOf[String]), anyString(), any[MediaType], anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, adminAuthenticationTokenResponse()))
      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_FORBIDDEN, ""))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getErrorCode shouldBe HttpServletResponse.SC_INTERNAL_SERVER_ERROR

      filterChain.getLastRequest should be(null)
      filterChain.getLastResponse should be(null)
    }

    it("rejects with 500 if we cannot reach identity") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockAkkaServiceClient.post(anyString(), anyString(), anyMapOf(classOf[String], classOf[String]), anyString(), any[MediaType], anyBoolean()))
        .thenThrow(new AkkaServiceClientException("Unable to reach identity!", null))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getErrorCode shouldBe HttpServletResponse.SC_INTERNAL_SERVER_ERROR

      filterChain.getLastRequest should be(null)
      filterChain.getLastResponse should be(null)
    }

    it("rejects with 500 if we cannot authenticate as the admin user") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockAkkaServiceClient.post(anyString(), anyString(), anyMapOf(classOf[String], classOf[String]), anyString(), any[MediaType], anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, adminAuthenticationTokenResponse()))
      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenThrow(new AkkaServiceClientException("Unable to talk to identity!", null))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getErrorCode shouldBe HttpServletResponse.SC_INTERNAL_SERVER_ERROR

      filterChain.getLastRequest should be(null)
      filterChain.getLastResponse should be(null)
    }

    it("rejects with 503 if we are rate limited by identity (413)") {
      val retryValue = DateUtils.formatDate(new DateTime().plusHours(1).toDate)

      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockAkkaServiceClient.post(anyString(), anyString(), anyMapOf(classOf[String], classOf[String]), anyString(), any[MediaType], anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, Array(HttpHeaders.RETRY_AFTER -> retryValue), "Rate limited by identity!"))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getErrorCode shouldBe HttpServletResponse.SC_SERVICE_UNAVAILABLE
      response.getHeader(HttpHeaders.RETRY_AFTER) shouldBe retryValue

      filterChain.getLastRequest should be(null)
      filterChain.getLastResponse should be(null)
    }

    it("rejects with 503 if we are rate limited by identity (429)") {
      val retryValue = DateUtils.formatDate(new DateTime().plusHours(1).toDate)

      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockAkkaServiceClient.post(anyString(), anyString(), anyMapOf(classOf[String], classOf[String]), anyString(), any[MediaType], anyBoolean()))
        .thenReturn(new ServiceClientResponse(SC_TOO_MANY_REQUESTS, Array(HttpHeaders.RETRY_AFTER -> retryValue), "Rate limited by identity!"))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getErrorCode shouldBe HttpServletResponse.SC_SERVICE_UNAVAILABLE
      response.getHeader(HttpHeaders.RETRY_AFTER) shouldBe retryValue

      filterChain.getLastRequest should be(null)
      filterChain.getLastResponse should be(null)
    }

    it("does not forward the authenticatedBy field if it is an empty array") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)
      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponseAuthenticatedBy()))

      val response = new MockHttpServletResponse()
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest should not be null
      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.AUTHENTICATED_BY.toString) shouldBe null
    }

    it("forwards the authenticatedBy field if present and non-empty") {
      val authMethods = Seq("password")
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)
      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponseAuthenticatedBy(authenticatedBy = authMethods)))

      val response = new MockHttpServletResponse()
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest should not be null
      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.AUTHENTICATED_BY.toString) shouldBe "password"
    }

    it("forwards all values from the authenticatedBy field") {
      val authMethods = Seq("password", "MFA")
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)
      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponseAuthenticatedBy(authenticatedBy = authMethods)))

      val response = new MockHttpServletResponse()
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest should not be null
      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeaders(OpenStackServiceHeader.AUTHENTICATED_BY.toString).asScala.toSeq should contain only("password", "MFA")
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
        |    <require-service-endpoint public-url="https://compute.north.public.com/v1" region="Global" name="Compute" type="compute"/>
        |
        |    <pre-authorized-roles>
        |        <role>serviceAdmin</role>
        |        <role>racker</role>
        |    </pre-authorized-roles>
        |</keystone-v2>
      """.stripMargin)

    val filter = new KeystoneV2Filter(mockConfigurationService, mockAkkaServiceClientFactory, mock[AtomFeedService], mockDatastoreService)

    filter.init(mockFilterConfig)
    filter.KeystoneV2ConfigListener.configurationUpdated(configuration)
    filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

    it("allows a user through if they have the endpoint configured in their endpoints list") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)
      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse()))
      when(mockAkkaServiceClient.get(mockitoEq(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, endpointsResponse()))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      //Continues with the chain
      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)
    }

    it("allows a user through if they have the endpoint configured in their endpoints list with tenant appended") {
      def configurationDos = Marshaller.keystoneV2ConfigFromString(
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
          |    <require-service-endpoint public-url="https://compute.north.public.com/v1/appended" region="Global" name="Compute" type="compute"/>
          |</keystone-v2>
        """.stripMargin)

      val testFilter = new KeystoneV2Filter(mockConfigurationService, mockAkkaServiceClientFactory, mock[AtomFeedService], mockDatastoreService)
      testFilter.KeystoneV2ConfigListener.configurationUpdated(configurationDos)
      testFilter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)
      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse()))
      when(mockAkkaServiceClient.get(mockitoEq(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, endpointsResponse()))

      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      testFilter.doFilter(request, response, filterChain)

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
      when(mockAkkaServiceClient.get(mockitoEq(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION, endpointsResponse()))

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

      when(mockAkkaServiceClient.get(mockitoEq(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_FORBIDDEN, ""))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest should be(null)
      filterChain.getLastResponse should be(null)

      response.getErrorCode shouldBe HttpServletResponse.SC_INTERNAL_SERVER_ERROR
    }

    it("handles 401 response from endpoints call") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(TestValidToken(), Nil: _*)

      when(mockAkkaServiceClient.get(mockitoEq(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(
          new ServiceClientResponse(HttpServletResponse.SC_UNAUTHORIZED, endpointsResponse()),
          new ServiceClientResponse(HttpServletResponse.SC_UNAUTHORIZED, ""))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getErrorCode shouldBe HttpServletResponse.SC_INTERNAL_SERVER_ERROR
    }

    it("handles akka service client call failing") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(TestValidToken(), Nil: _*)

      when(mockAkkaServiceClient.get(mockitoEq(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenThrow(new AkkaServiceClientException("Unable to reach identity!", null))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      //Continues with the chain
      filterChain.getLastRequest shouldBe null
      filterChain.getLastResponse shouldBe null

      response.getErrorCode shouldBe HttpServletResponse.SC_INTERNAL_SERVER_ERROR
    }

    it("handles unexpected response from endpoints call") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(TestValidToken(), Nil: _*)

      when(mockAkkaServiceClient.get(mockitoEq(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_NOT_IMPLEMENTED, ""))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest shouldBe null
      filterChain.getLastResponse shouldBe null

      response.getErrorCode shouldBe HttpServletResponse.SC_BAD_GATEWAY
    }

    it("rejects with 403 if the user does not have the required endpoint") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(TestValidToken(), Nil: _*)
      when(mockAkkaServiceClient.get(mockitoEq(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, oneEndpointResponse()))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getErrorCode shouldBe HttpServletResponse.SC_FORBIDDEN
      //Continues with the chain
      filterChain.getLastRequest should be(null)
      filterChain.getLastResponse should be(null)
    }

    it("bypasses validation if the user has the role listed in pre-authorized-roles") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateRackerTokenResponse()))
      when(mockAkkaServiceClient.get(mockitoEq(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, oneEndpointResponse()))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)
    }

    it("rejects with 401 if the token is no longer valid when catalog variable is set") {
      val modifiedConfig = configuration
      modifiedConfig.setRequireServiceEndpoint(null)
      modifiedConfig.getIdentityService.setSetCatalogInHeader(true)
      filter.KeystoneV2ConfigListener.configurationUpdated(modifiedConfig)

      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(TestValidToken(), Nil: _*)

      when(mockAkkaServiceClient.get(mockitoEq(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_NOT_FOUND, ""))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.KeystoneV2ConfigListener.configurationUpdated(configuration)

      response.getErrorCode shouldBe HttpServletResponse.SC_UNAUTHORIZED
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

        when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
          .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse()))

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

        when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
          .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse()))

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

        when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
          .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateRackerTokenResponse()))

        val endpointsList = Vector(Endpoint(Some("DERP"), Some("LOLNOPE"), Some("compute"), "https://compute.north.public.com/v1"))
        when(mockDatastore.get(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")).thenReturn(EndpointsData("", endpointsList), Nil: _*)

        val response = new MockHttpServletResponse
        val filterChain = new MockFilterChain()
        filter.doFilter(request, response, filterChain)

        filterChain.getLastRequest shouldNot be(null)
        filterChain.getLastResponse shouldNot be(null)
      }

      it("rejects with 503 if we are rate limited by identity (413)") {
        val retryValue = DateUtils.formatDate(new DateTime().plusHours(1).toDate)

        val request = new MockHttpServletRequest()
        request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

        when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

        when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
          .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, Array(HttpHeaders.RETRY_AFTER -> retryValue), "Rate limited by identity!"))

        when(mockDatastore.get(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")).thenReturn(EndpointsData("", Vector.empty), Nil: _*)

        val response = new MockHttpServletResponse
        val filterChain = new MockFilterChain()
        filter.doFilter(request, response, filterChain)

        response.getErrorCode shouldBe HttpServletResponse.SC_SERVICE_UNAVAILABLE
        response.getHeader(HttpHeaders.RETRY_AFTER) shouldBe retryValue

        filterChain.getLastRequest should be(null)
        filterChain.getLastResponse should be(null)
      }

      it("rejects with 503 if we are rate limited by identity (429)") {
        val retryValue = DateUtils.formatDate(new DateTime().plusHours(1).toDate)

        val request = new MockHttpServletRequest()
        request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

        when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

        when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
          .thenReturn(new ServiceClientResponse(SC_TOO_MANY_REQUESTS, Array(HttpHeaders.RETRY_AFTER -> retryValue), "Rate limited by identity!"))

        when(mockDatastore.get(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")).thenReturn(EndpointsData("", Vector.empty), Nil: _*)

        val response = new MockHttpServletResponse
        val filterChain = new MockFilterChain()
        filter.doFilter(request, response, filterChain)

        response.getErrorCode shouldBe HttpServletResponse.SC_SERVICE_UNAVAILABLE
        response.getHeader(HttpHeaders.RETRY_AFTER) shouldBe retryValue

        filterChain.getLastRequest should be(null)
        filterChain.getLastResponse should be(null)
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

    val filter = new KeystoneV2Filter(mockConfigurationService, mockAkkaServiceClientFactory, mock[AtomFeedService], mockDatastoreService)

    filter.init(mockFilterConfig)
    filter.KeystoneV2ConfigListener.configurationUpdated(configuration)
    filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

    it("will allow through if the user has the group cached") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse()))

      val groupsList = Vector("DERP")
      when(mockDatastore.get(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")).thenReturn(groupsList, Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)

      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(PowerApiHeader.GROUPS.toString) shouldBe "DERP"
    }

    it("handles when serviceClientResponse.getStatus fails") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(TestValidToken(userId = VALID_USER_ID), Nil: _*)

      when(mockAkkaServiceClient.get(mockitoEq(s"$GROUPS_KEY_PREFIX$VALID_USER_ID"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenThrow(new AkkaServiceClientException("Unable to reach identity!", null))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getErrorCode shouldBe HttpServletResponse.SC_INTERNAL_SERVER_ERROR
      filterChain.getLastRequest shouldBe null
    }

    it("handles 401 response from groups call") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(TestValidToken(userId = VALID_USER_ID), Nil: _*)

      when(mockAkkaServiceClient.get(mockitoEq(s"$GROUPS_KEY_PREFIX$VALID_USER_ID"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(
          new ServiceClientResponse(HttpServletResponse.SC_UNAUTHORIZED, ""),
          new ServiceClientResponse(HttpServletResponse.SC_UNAUTHORIZED, ""))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getErrorCode shouldBe HttpServletResponse.SC_INTERNAL_SERVER_ERROR
      filterChain.getLastRequest shouldBe null
    }

    it("handles 403 response from groups call") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(TestValidToken(userId = VALID_USER_ID), Nil: _*)

      when(mockAkkaServiceClient.get(mockitoEq(s"$GROUPS_KEY_PREFIX$VALID_USER_ID"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_FORBIDDEN, ""))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getErrorCode shouldBe HttpServletResponse.SC_INTERNAL_SERVER_ERROR
      filterChain.getLastRequest shouldBe null
    }

    it("handles 413 response from groups call") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(TestValidToken(userId = VALID_USER_ID), Nil: _*)

      when(mockAkkaServiceClient.get(mockitoEq(s"$GROUPS_KEY_PREFIX$VALID_USER_ID"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Rate limited by identity!"))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getErrorCode shouldBe HttpServletResponse.SC_SERVICE_UNAVAILABLE
      filterChain.getLastRequest shouldBe null
    }

    it("handles 429 response from groups call") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(TestValidToken(userId = VALID_USER_ID), Nil: _*)

      when(mockAkkaServiceClient.get(mockitoEq(s"$GROUPS_KEY_PREFIX$VALID_USER_ID"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(SC_TOO_MANY_REQUESTS, "Rate limited by identity!"))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getErrorCode shouldBe HttpServletResponse.SC_SERVICE_UNAVAILABLE
      filterChain.getLastRequest shouldBe null
    }

    it("handles unexpected response from groups call") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(TestValidToken(userId = VALID_USER_ID), Nil: _*)

      when(mockAkkaServiceClient.get(mockitoEq(s"$GROUPS_KEY_PREFIX$VALID_USER_ID"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_NOT_IMPLEMENTED, ""))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getErrorCode shouldBe HttpServletResponse.SC_BAD_GATEWAY
      filterChain.getLastRequest shouldBe null
    }

    it("handles 404s from groups call by allowing users through with no X-PP-Groups") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(TestValidToken(userId = VALID_USER_ID, roles = Seq(Role("Racker"))), Nil: _*)

      when(mockAkkaServiceClient.get(mockitoEq(s"$GROUPS_KEY_PREFIX$VALID_USER_ID"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_NOT_FOUND, ""))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(PowerApiHeader.GROUPS.toString) shouldBe null
    }
  }

  describe("Configured to delegate") {
    //Configure the filter
    def configuration = Marshaller.keystoneV2ConfigFromString(
      """<?xml version="1.0" encoding="UTF-8"?>
        |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
        |    <identity-service
        |            username="username"
        |            password="password"
        |            uri="https://some.identity.com"
        |            set-groups-in-header="false"
        |            />
        |
        |     <delegating/>
        |</keystone-v2>
      """.stripMargin)

    val filter = new KeystoneV2Filter(mockConfigurationService, mockAkkaServiceClientFactory, mock[AtomFeedService], mockDatastoreService)

    filter.init(mockFilterConfig)
    filter.KeystoneV2ConfigListener.configurationUpdated(configuration)
    filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

    it("delegates with an invalid token and adds the header") {
      val request = new MockHttpServletRequest
      request.setRequestURL("http://www.sample.com/some/path/application.wadl")
      request.setRequestURI("/some/path/application.wadl")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, "INVALID_TOKEN")

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockAkkaServiceClient.get(mockitoEq(s"${TOKEN_KEY_PREFIX}INVALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_NOT_FOUND, ""))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      val delegationHeader = parseDelegationHeader(filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(HttpDelegationHeaderNames.Delegated))
      delegationHeader shouldBe a[Success[_]]
      delegationHeader.get.statusCode shouldBe HttpServletResponse.SC_UNAUTHORIZED
    }

    it("delegates if lacking the required service endpoint and all headers for data we have") {
      val modifiedConfig = configuration
      modifiedConfig.setRequireServiceEndpoint(new ServiceEndpointType().withPublicUrl("http://google.com/"))
      filter.KeystoneV2ConfigListener.configurationUpdated(modifiedConfig)

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
      filter.KeystoneV2ConfigListener.configurationUpdated(configuration)

      val lastRequest = filterChain.getLastRequest.asInstanceOf[HttpServletRequest]
      val delegationHeader = parseDelegationHeader(lastRequest.getHeader(HttpDelegationHeaderNames.Delegated))
      delegationHeader shouldBe a[Success[_]]
      delegationHeader.get.statusCode shouldBe HttpServletResponse.SC_FORBIDDEN
      lastRequest.getHeaderNames.asScala.toList should contain allOf(OpenStackServiceHeader.USER_ID.toString,
        OpenStackServiceHeader.X_EXPIRATION.toString,
        OpenStackServiceHeader.EXTENDED_AUTHORIZATION.toString)
    }

    it("delegates if identity doesn't respond properly") {
      val request = new MockHttpServletRequest
      request.setRequestURL("http://www.sample.com/some/path/application.wadl")
      request.setRequestURI("/some/path/application.wadl")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, "butts"))

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

      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "invalid")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_UNAUTHORIZED, ""))
      when(mockAkkaServiceClient.post(anyString(), anyString(), anyMapOf(classOf[String], classOf[String]), anyString(), any[MediaType], anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_NOT_FOUND, ""))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      val delegationHeader = parseDelegationHeader(filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(HttpDelegationHeaderNames.Delegated))
      delegationHeader shouldBe a[Success[_]]
      delegationHeader.get.statusCode shouldBe HttpServletResponse.SC_INTERNAL_SERVER_ERROR
    }

    it("forwards the identity status as Confirmed in the x-identity-status header when delegating success") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)
      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.IDENTITY_STATUS.toString) shouldBe IdentityStatus.Confirmed.toString
    }

    it("forwards the identity status as Indeterminate in the x-identity-status header when delegating failure") {
      val request = new MockHttpServletRequest
      request.setRequestURL("http://www.sample.com/some/path/application.wadl")
      request.setRequestURI("/some/path/application.wadl")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, "INVALID_TOKEN")

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockAkkaServiceClient.get(mockitoEq(s"${TOKEN_KEY_PREFIX}INVALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_NOT_FOUND, ""))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.IDENTITY_STATUS.toString) shouldBe IdentityStatus.Indeterminate.toString
    }

    it("responds with a www-authenticate header when delegating") {
      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      val mockServlet = mock[Servlet]
      doAnswer(new Answer[Unit] {
        override def answer(invocation: InvocationOnMock): Unit = {
          response.setHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString, "Delegated")
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)
        }
      }).when(mockServlet).service(any[ServletRequest](), any[ServletResponse]())

      filterChain.setServlet(mockServlet)

      val request = new MockHttpServletRequest
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, "notValidToken")

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)
      when(mockAkkaServiceClient.get(mockitoEq(s"${TOKEN_KEY_PREFIX}notValidToken"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_NOT_FOUND, ""))

      filter.doFilter(request, response, filterChain)

      response.getStatusCode shouldBe HttpServletResponse.SC_UNAUTHORIZED
      response.getHeaders(CommonHttpHeader.WWW_AUTHENTICATE.toString) should contain("Keystone uri=https://some.identity.com")
    }
  }

  describe("Configured to whitelist a particular URI") {
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

    val filter = new KeystoneV2Filter(mockConfigurationService, mockAkkaServiceClientFactory, mock[AtomFeedService], mockDatastoreService)

    filter.init(mockFilterConfig)
    filter.KeystoneV2ConfigListener.configurationUpdated(configuration)
    filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

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

  describe("Configured timeouts") {
    def configuration = Marshaller.keystoneV2ConfigFromString(
      """<?xml version="1.0" encoding="UTF-8"?>
        |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
        |    <identity-service username="username"
        |                      password="password"
        |                      uri="https://some.identity.com"
        |                      set-catalog-in-header="true"
        |    />
        |    <cache>
        |        <timeouts>
        |            <token>270</token>
        |            <group>300</group>
        |            <endpoints>330</endpoints>
        |        </timeouts>
        |        <atom-feed id="some-feed"/>
        |    </cache>
        |</keystone-v2>
      """.stripMargin)

    val userId = "TestUser123"
    val filter = new KeystoneV2Filter(mockConfigurationService, mockAkkaServiceClientFactory, mock[AtomFeedService], mockDatastoreService)

    filter.init(mockFilterConfig)
    filter.KeystoneV2ConfigListener.configurationUpdated(configuration)
    filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

    it("passes through the values to the distributed datastore for the proper cache timeouts") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn(null, "glibglob")

      when(mockAkkaServiceClient.post(anyString(), anyString(), anyMapOf(classOf[String], classOf[String]), anyString(), any[MediaType], anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, adminAuthenticationTokenResponse()))
      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse(userId = userId)))
      when(mockAkkaServiceClient.get(mockitoEq(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, endpointsResponse()))
      when(mockAkkaServiceClient.get(mockitoEq(s"$GROUPS_KEY_PREFIX$userId"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, groupsResponse()))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)
      verify(mockDatastore).put(mockitoEq(s"$ADMIN_TOKEN_KEY"), any())
      verify(mockDatastore).patch(mockitoEq(s"$USER_ID_KEY_PREFIX$userId"), isA(classOf[SetPatch[String]]), mockitoEq(270), mockitoEq(TimeUnit.SECONDS))
      verify(mockDatastore).put(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), any(), mockitoEq(270), mockitoEq(TimeUnit.SECONDS))
      verify(mockDatastore).put(mockitoEq(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN"), any(), mockitoEq(330), mockitoEq(TimeUnit.SECONDS))
      verify(mockDatastore).put(mockitoEq(s"$GROUPS_KEY_PREFIX$VALID_TOKEN"), any(), mockitoEq(300), mockitoEq(TimeUnit.SECONDS))
    }

    it("passes through variable offsets within a range to the distributed datastore") {
      val modifiedConfig = configuration
      modifiedConfig.getCache.getTimeouts.setVariability(1)
      filter.KeystoneV2ConfigListener.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn(null, "glibglob")

      when(mockAkkaServiceClient.post(anyString(), anyString(), anyMapOf(classOf[String], classOf[String]), anyString(), any[MediaType], anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, adminAuthenticationTokenResponse()))
      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse(userId = userId)))
      when(mockAkkaServiceClient.get(mockitoEq(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, endpointsResponse()))
      when(mockAkkaServiceClient.get(mockitoEq(s"$GROUPS_KEY_PREFIX$userId"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, groupsResponse()))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.KeystoneV2ConfigListener.configurationUpdated(configuration)

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)
      verify(mockDatastore).patch(mockitoEq(s"$USER_ID_KEY_PREFIX$userId"),
        isA(classOf[SetPatch[String]]),
        intThat(both(greaterThanOrEqualTo(Int.box(269))).and(lessThanOrEqualTo(Int.box(271)))),
        mockitoEq(TimeUnit.SECONDS))
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
      filter.KeystoneV2ConfigListener.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn(null, "glibglob")

      when(mockAkkaServiceClient.post(anyString(), anyString(), anyMapOf(classOf[String], classOf[String]), anyString(), any[MediaType], anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, adminAuthenticationTokenResponse()))
      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse(userId = userId)))
      when(mockAkkaServiceClient.get(mockitoEq(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, endpointsResponse()))
      when(mockAkkaServiceClient.get(mockitoEq(s"$GROUPS_KEY_PREFIX$userId"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, groupsResponse()))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)
      verify(mockDatastore).put(mockitoEq(s"$ADMIN_TOKEN_KEY"), any())
      verify(mockDatastore).patch(mockitoEq(s"$USER_ID_KEY_PREFIX$userId"), isA(classOf[SetPatch[String]]), mockitoEq(600), mockitoEq(TimeUnit.SECONDS))
      verify(mockDatastore).put(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), any(), mockitoEq(600), mockitoEq(TimeUnit.SECONDS))
      verify(mockDatastore).put(mockitoEq(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN"), any(), mockitoEq(600), mockitoEq(TimeUnit.SECONDS))
      verify(mockDatastore).put(mockitoEq(s"$GROUPS_KEY_PREFIX$VALID_TOKEN"), any(), mockitoEq(600), mockitoEq(TimeUnit.SECONDS))

      filter.KeystoneV2ConfigListener.configurationUpdated(configuration)
    }
  }

  describe("Tenant handling is enabled") {
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
        |        <validate-tenant strip-token-tenant-prefixes="foo:/bar:">
        |            <uri-extraction-regex>/(\w+)/.*</uri-extraction-regex>
        |        </validate-tenant>
        |        <send-tenant-id-quality default-tenant-quality="0.9" uri-tenant-quality="0.7" roles-tenant-quality="0.5"/>
        |    </tenant-handling>
        |    <pre-authorized-roles>
        |        <role>serviceAdmin</role>
        |        <role>racker</role>
        |    </pre-authorized-roles>
        |</keystone-v2>
      """.stripMargin)

    val filter = new KeystoneV2Filter(mockConfigurationService, mockAkkaServiceClientFactory, mock[AtomFeedService], mockDatastoreService)

    filter.init(mockFilterConfig)
    filter.KeystoneV2ConfigListener.configurationUpdated(configuration)
    filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

    it("will not require a default tenant ID") {
      val request = new MockHttpServletRequest()
      request.setRequestURL("http://www.sample.com/tenant/test")
      request.setRequestURI("/tenant/test")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(tenantIds = Seq("tenant")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)
    }

    it("will extract the tenant from the URI and validate that the user has that tenant in their list") {
      val request = new MockHttpServletRequest()
      request.setRequestURL("http://www.sample.com/tenant/test")
      request.setRequestURI("/tenant/test")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(defaultTenantId = Some("tenant")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)
    }

    it("will extract the tenant from the URI and validate that the user has a prefixed tenant as the default") {
      val request = new MockHttpServletRequest()
      request.setRequestURL("http://www.sample.com/tenant/test")
      request.setRequestURI("/tenant/test")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(defaultTenantId = Some("foo:tenant")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)
    }

    it("will extract the tenant from the URI and validate that the user has a prefixed tenant in their roles") {
      val request = new MockHttpServletRequest()
      request.setRequestURL("http://www.sample.com/tenant/test")
      request.setRequestURI("/tenant/test")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(defaultTenantId = Some("oof"), tenantIds = Seq("foo:tenant", "rab")), Nil: _*)

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

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(defaultTenantId = Some("not-tenant")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.wasErrorSent shouldBe true
      response.getErrorCode shouldBe HttpServletResponse.SC_UNAUTHORIZED
    }

    it("sends all tenant IDs when configured to") {
      val modifiedConfig = configuration
      modifiedConfig.getTenantHandling.setSendTenantIdQuality(null)
      filter.KeystoneV2ConfigListener.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest()
      request.setRequestURL("http://www.sample.com/tenant/test")
      request.setRequestURI("/tenant/test")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(defaultTenantId = Some("tenant"), tenantIds = Seq("rick", "morty")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.KeystoneV2ConfigListener.configurationUpdated(configuration)

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

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(defaultTenantId = Some("tenant"), tenantIds = Seq("rick", "morty")), Nil: _*)

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
      filter.KeystoneV2ConfigListener.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest()
      request.setRequestURL("http://www.sample.com/rick/test")
      request.setRequestURI("/rick/test")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(tenantIds = Seq("rick", "morty")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.KeystoneV2ConfigListener.configurationUpdated(configuration)

      val processedRequest = filterChain.getLastRequest.asInstanceOf[HttpServletRequest]
      processedRequest.getHeaders(OpenStackServiceHeader.TENANT_ID.toString).asScala.size shouldBe 1
      processedRequest.getHeader(OpenStackServiceHeader.TENANT_ID.toString) shouldBe "rick;q=0.7"
    }

    it("bypasses the URI tenant validation check when a user has a role in the pre-authorized-roles list") {
      val request = new MockHttpServletRequest()
      request.setRequestURL("http://www.sample.com/tenant/test")
      request.setRequestURI("/tenant/test")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(defaultTenantId = Some("not-tenant"), roles = Seq(Role("racker"))), Nil: _*)

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
      filter.KeystoneV2ConfigListener.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest()
      request.setRequestURL("http://www.sample.com/morty/test")
      request.setRequestURI("/morty/test")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(defaultTenantId = Some("tenant"), tenantIds = Seq("rick", "morty")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.KeystoneV2ConfigListener.configurationUpdated(configuration)

      val processedRequest = filterChain.getLastRequest.asInstanceOf[HttpServletRequest]
      processedRequest.getHeaders(OpenStackServiceHeader.TENANT_ID.toString).asScala.size shouldBe 1
      processedRequest.getHeader(OpenStackServiceHeader.TENANT_ID.toString) shouldBe "morty"
    }

    it("sends the user's default tenant, if validate-tenant is not enabled") {
      val modifiedConfig = configuration
      modifiedConfig.getTenantHandling.setValidateTenant(null)
      modifiedConfig.getTenantHandling.setSendAllTenantIds(false)
      modifiedConfig.getTenantHandling.setSendTenantIdQuality(null)
      filter.KeystoneV2ConfigListener.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest()
      request.setRequestURL("http://www.sample.com/years/test")
      request.setRequestURI("/years/test")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(defaultTenantId = Some("one"), tenantIds = Seq("hundred", "years")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.KeystoneV2ConfigListener.configurationUpdated(configuration)

      val processedRequest = filterChain.getLastRequest.asInstanceOf[HttpServletRequest]
      processedRequest.getHeaders(OpenStackServiceHeader.TENANT_ID.toString).asScala.size shouldBe 1
      processedRequest.getHeader(OpenStackServiceHeader.TENANT_ID.toString) shouldBe "one"
    }

    it("should return a failure if a tenant could not be parsed from the URI") {
      val request = new MockHttpServletRequest()
      request.setRequestURL("http://www.sample.com/bu-%tts/test")
      request.setRequestURI("/bu-%tts/test")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(defaultTenantId = Some("bu-%tts")), Nil: _*)

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
      filter.KeystoneV2ConfigListener.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest()
      request.setRequestURL("http://www.sample.com/years/test")
      request.setRequestURI("/years/test")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(tenantIds = Seq("hundred", "years"), defaultTenantId = Some("foo")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.KeystoneV2ConfigListener.configurationUpdated(configuration)

      val processedRequest = filterChain.getLastRequest.asInstanceOf[HttpServletRequest]
      processedRequest.getHeader(OpenStackServiceHeader.EXTENDED_AUTHORIZATION.toString) shouldBe "Proxy foo"
    }

    it("should send the X-Authorization header without a tenant if tenant validation is not used") {
      val modifiedConfig = configuration
      modifiedConfig.getTenantHandling.setValidateTenant(null)
      filter.KeystoneV2ConfigListener.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest()
      request.setRequestURL("http://www.sample.com/years/test")
      request.setRequestURI("/years/test")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(tenantIds = Seq("hundred", "years"), defaultTenantId = Some("foo")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.KeystoneV2ConfigListener.configurationUpdated(configuration)

      val processedRequest = filterChain.getLastRequest.asInstanceOf[HttpServletRequest]
      processedRequest.getHeader(OpenStackServiceHeader.EXTENDED_AUTHORIZATION.toString) shouldBe "Proxy foo"
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

    val filter = new KeystoneV2Filter(mockConfigurationService, mockAkkaServiceClientFactory, mock[AtomFeedService], mockDatastoreService)

    filter.init(mockFilterConfig)
    filter.KeystoneV2ConfigListener.configurationUpdated(configuration)
    filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

    it("forwards the user information in the x-pp-user, x-user-name, and x-user-id headers") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse()))
      when(mockAkkaServiceClient.get(mockitoEq(s"$GROUPS_KEY_PREFIX$VALID_USER_ID"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, groupsResponse()))

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

      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse()))
      when(mockAkkaServiceClient.get(mockitoEq(s"$GROUPS_KEY_PREFIX$VALID_USER_ID"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, groupsResponse()))

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

      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse()))
      when(mockAkkaServiceClient.get(mockitoEq(s"$GROUPS_KEY_PREFIX$VALID_USER_ID"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, groupsResponse()))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.CONTACT_ID.toString) shouldBe "abc123"
    }

    it("forwards the user's impersonator information in the x-impersonator-id, x-impersonator-name, and x-impersonator roles headers") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateImpersonatedTokenResponse()))
      when(mockAkkaServiceClient.get(mockitoEq(s"$GROUPS_KEY_PREFIX$VALID_USER_ID"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, groupsResponse()))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.IMPERSONATOR_ID.toString) shouldBe "567"
      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.IMPERSONATOR_NAME.toString) shouldBe "rick"
      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.IMPERSONATOR_ROLES.toString) should include("Racker")
      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.IMPERSONATOR_ROLES.toString) should include("object-store:admin")
    }

    it("forwards the user's default region information in the x-default-region header") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse()))
      when(mockAkkaServiceClient.get(mockitoEq(s"$GROUPS_KEY_PREFIX$VALID_USER_ID"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, groupsResponse()))

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

      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse(expires = dateTime)))
      when(mockAkkaServiceClient.get(mockitoEq(s"$GROUPS_KEY_PREFIX$VALID_USER_ID"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, groupsResponse()))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)


      filterChain.getLastRequest.asInstanceOf[HttpServletRequest]
        .getHeader(OpenStackServiceHeader.X_EXPIRATION.toString) shouldBe iso8601ToRfc1123(tokenDateFormat(dateTime))
    }

    it("forwards the groups in the x-pp-groups header by default") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse()))
      when(mockAkkaServiceClient.get(mockitoEq(s"$GROUPS_KEY_PREFIX$VALID_USER_ID"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, groupsResponse()))

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

      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse(expires = dateTime)))
      when(mockAkkaServiceClient.get(mockitoEq(s"$GROUPS_KEY_PREFIX$VALID_USER_ID"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse()))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(PowerApiHeader.GROUPS.toString) shouldBe null
    }

    it("should not add the roles in the x-roles header when isSetRolesInHeader is false") {
      val modifiedConfig = configuration
      modifiedConfig.getIdentityService.setSetRolesInHeader(false)
      filter.KeystoneV2ConfigListener.configurationUpdated(modifiedConfig)
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse()))
      when(mockAkkaServiceClient.get(mockitoEq(s"$GROUPS_KEY_PREFIX$VALID_USER_ID"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, groupsResponse()))

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
      filter.KeystoneV2ConfigListener.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)
      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(), Nil: _*)
      when(mockDatastore.get(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")).thenReturn(EndpointsData(endpointsResponse(), Vector(Endpoint(None, None, None, "example.com"))), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.KeystoneV2ConfigListener.configurationUpdated(configuration)

      val encodedEndpoints = Base64.encodeBase64String(endpointsResponse().getBytes)
      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(PowerApiHeader.X_CATALOG.toString) shouldBe encodedEndpoints
    }
  }

  describe("Handling tenanted roles") {
    it("should forward all roles if not in tenanted mode") {
      def configuration = Marshaller.keystoneV2ConfigFromString(
        """<?xml version="1.0" encoding="UTF-8"?>
          |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
          |    <identity-service
          |            username="username"
          |            password="password"
          |            uri="https://some.identity.com"
          |            set-groups-in-header="false"
          |            />
          |</keystone-v2>
        """.stripMargin)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponseTenantedRoles()))

      val filter = new KeystoneV2Filter(mockConfigurationService, mockAkkaServiceClientFactory, mock[AtomFeedService], mockDatastoreService)

      filter.init(mockFilterConfig)
      filter.KeystoneV2ConfigListener.configurationUpdated(configuration)
      filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

      val request = new MockHttpServletRequest()
      request.setRequestURL("http://www.sample.com/567/foo")
      request.setRequestURI("/567/foo")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      val response = new MockHttpServletResponse()
      val chain = new MockFilterChain()

      filter.doFilter(request, response, chain)

      val postFilterRequest = chain.getLastRequest.asInstanceOf[HttpServletRequest]
      postFilterRequest.getHeader(OpenStackServiceHeader.ROLES.toString) should include("role:123")
      postFilterRequest.getHeader(OpenStackServiceHeader.ROLES.toString) should include("role:234")
      postFilterRequest.getHeader(OpenStackServiceHeader.ROLES.toString) should include("role:345")
    }

    it("should forward all roles in tenanted mode if legacy mode is enabled") {
      def configuration = Marshaller.keystoneV2ConfigFromString(
        """<?xml version="1.0" encoding="UTF-8"?>
          |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
          |    <identity-service
          |            username="username"
          |            password="password"
          |            uri="https://some.identity.com"
          |            set-groups-in-header="false"
          |            />
          |    <tenant-handling>
          |        <validate-tenant enable-legacy-roles-mode="true">
          |            <uri-extraction-regex>/(.+)/.*</uri-extraction-regex>
          |        </validate-tenant>
          |    </tenant-handling>
          |</keystone-v2>
        """.stripMargin)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponseTenantedRoles()))

      val filter = new KeystoneV2Filter(mockConfigurationService, mockAkkaServiceClientFactory, mock[AtomFeedService], mockDatastoreService)

      filter.init(mockFilterConfig)
      filter.KeystoneV2ConfigListener.configurationUpdated(configuration)
      filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

      val request = new MockHttpServletRequest()
      request.setRequestURL("http://www.sample.com/456/foo")
      request.setRequestURI("/456/foo")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      val response = new MockHttpServletResponse()
      val chain = new MockFilterChain()

      filter.doFilter(request, response, chain)

      val postFilterRequest = chain.getLastRequest.asInstanceOf[HttpServletRequest]
      postFilterRequest.getHeader(OpenStackServiceHeader.ROLES.toString) should include("role:123")
      postFilterRequest.getHeader(OpenStackServiceHeader.ROLES.toString) should include("role:234")
      postFilterRequest.getHeader(OpenStackServiceHeader.ROLES.toString) should include("role:345")
    }

    it("should only forward matching tenant roles if legacy mode is disabled") {
      def configuration = Marshaller.keystoneV2ConfigFromString(
        """<?xml version="1.0" encoding="UTF-8"?>
          |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
          |    <identity-service
          |            username="username"
          |            password="password"
          |            uri="https://some.identity.com"
          |            set-groups-in-header="false"
          |            />
          |    <tenant-handling>
          |        <validate-tenant enable-legacy-roles-mode="false">
          |            <uri-extraction-regex>/(.+)/.*</uri-extraction-regex>
          |        </validate-tenant>
          |    </tenant-handling>
          |</keystone-v2>
        """.stripMargin)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponseTenantedRoles()))

      val filter = new KeystoneV2Filter(mockConfigurationService, mockAkkaServiceClientFactory, mock[AtomFeedService], mockDatastoreService)

      filter.init(mockFilterConfig)
      filter.KeystoneV2ConfigListener.configurationUpdated(configuration)
      filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

      val request = new MockHttpServletRequest()
      request.setRequestURL("http://www.sample.com/345/foo")
      request.setRequestURI("/345/foo")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      val response = new MockHttpServletResponse()
      val chain = new MockFilterChain()

      filter.doFilter(request, response, chain)

      val postFilterRequest = chain.getLastRequest.asInstanceOf[HttpServletRequest]
      postFilterRequest.getHeader(OpenStackServiceHeader.ROLES.toString) should include("role:123")
      postFilterRequest.getHeader(OpenStackServiceHeader.ROLES.toString) should include("role:345")
    }

    it("should forward all roles if legacy mode is disabled, but the user is pre-authorized") {
      def configuration = Marshaller.keystoneV2ConfigFromString(
        """<?xml version="1.0" encoding="UTF-8"?>
          |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
          |    <identity-service
          |            username="username"
          |            password="password"
          |            uri="https://some.identity.com"
          |            set-groups-in-header="false"
          |            />
          |    <tenant-handling>
          |        <validate-tenant enable-legacy-roles-mode="false">
          |            <uri-extraction-regex>/(.+)/.*</uri-extraction-regex>
          |        </validate-tenant>
          |    </tenant-handling>
          |    <pre-authorized-roles>
          |        <role>role:345</role>
          |    </pre-authorized-roles>
          |</keystone-v2>
        """.stripMargin)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponseTenantedRoles()))

      val filter = new KeystoneV2Filter(mockConfigurationService, mockAkkaServiceClientFactory, mock[AtomFeedService], mockDatastoreService)

      filter.init(mockFilterConfig)
      filter.KeystoneV2ConfigListener.configurationUpdated(configuration)
      filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

      val request = new MockHttpServletRequest()
      request.setRequestURL("http://www.sample.com/345/foo")
      request.setRequestURI("/345/foo")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      val response = new MockHttpServletResponse()
      val chain = new MockFilterChain()

      filter.doFilter(request, response, chain)

      val postFilterRequest = chain.getLastRequest.asInstanceOf[HttpServletRequest]
      postFilterRequest.getHeader(OpenStackServiceHeader.ROLES.toString) should include("role:123")
      postFilterRequest.getHeader(OpenStackServiceHeader.ROLES.toString) should include("role:234")
      postFilterRequest.getHeader(OpenStackServiceHeader.ROLES.toString) should include("role:345")
    }

    it("should only forward matching tenant roles if legacy mode is disabled, and the user is not pre-authorized") {
      def configuration = Marshaller.keystoneV2ConfigFromString(
        """<?xml version="1.0" encoding="UTF-8"?>
          |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
          |    <identity-service
          |            username="username"
          |            password="password"
          |            uri="https://some.identity.com"
          |            set-groups-in-header="false"
          |            />
          |    <tenant-handling>
          |        <validate-tenant enable-legacy-roles-mode="false">
          |            <uri-extraction-regex>/(.+)/.*</uri-extraction-regex>
          |        </validate-tenant>
          |    </tenant-handling>
          |    <pre-authorized-roles>
          |        <role>role:234</role>
          |    </pre-authorized-roles>
          |</keystone-v2>
        """.stripMargin)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponseTenantedRoles()))

      val filter = new KeystoneV2Filter(mockConfigurationService, mockAkkaServiceClientFactory, mock[AtomFeedService], mockDatastoreService)

      filter.init(mockFilterConfig)
      filter.KeystoneV2ConfigListener.configurationUpdated(configuration)
      filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

      val request = new MockHttpServletRequest()
      request.setRequestURL("http://www.sample.com/345/foo")
      request.setRequestURI("/345/foo")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      val response = new MockHttpServletResponse()
      val chain = new MockFilterChain()

      filter.doFilter(request, response, chain)

      val postFilterRequest = chain.getLastRequest.asInstanceOf[HttpServletRequest]
      postFilterRequest.getHeader(OpenStackServiceHeader.ROLES.toString) should include("role:123")
      postFilterRequest.getHeader(OpenStackServiceHeader.ROLES.toString) should include("role:345")
    }
  }

  describe("Configured tracing header") {
    def configuration = Marshaller.keystoneV2ConfigFromString(
      """<?xml version="1.0" encoding="UTF-8"?>
        |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
        |    <identity-service
        |            username="username"
        |            password="password"
        |            uri="https://some.identity.com"
        |            set-catalog-in-header="true"
        |            />
        |</keystone-v2>
      """.stripMargin)

    val filter = new KeystoneV2Filter(mockConfigurationService, mockAkkaServiceClientFactory, mock[AtomFeedService], mockDatastoreService)

    filter.init(mockFilterConfig)
    filter.KeystoneV2ConfigListener.configurationUpdated(configuration)
    filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

    it("should forward the x-trans-d header if enabled") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)
      request.addHeader(CommonHttpHeader.TRACE_GUID.toString, "test-guid")

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn(null, "glibglob")

      when(mockAkkaServiceClient.post(anyString(), anyString(), anyMapOf(classOf[String], classOf[String]), anyString(), any[MediaType], anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, adminAuthenticationTokenResponse()))
      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse()))
      when(mockAkkaServiceClient.get(mockitoEq(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, endpointsResponse()))
      when(mockAkkaServiceClient.get(mockitoEq(s"$GROUPS_KEY_PREFIX$VALID_USER_ID"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, groupsResponse()))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      verify(mockAkkaServiceClient)
        .post(anyString(), anyString(), argThat(hasEntry(CommonHttpHeader.TRACE_GUID.toString, "test-guid")), anyString(), any[MediaType], anyBoolean())
      verify(mockAkkaServiceClient)
        .get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.TRACE_GUID.toString, "test-guid")), anyBoolean())
      verify(mockAkkaServiceClient)
        .get(mockitoEq(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.TRACE_GUID.toString, "test-guid")), anyBoolean())
      verify(mockAkkaServiceClient)
        .get(mockitoEq(s"$GROUPS_KEY_PREFIX$VALID_USER_ID"), anyString(), argThat(hasEntry(CommonHttpHeader.TRACE_GUID.toString, "test-guid")), anyBoolean())
    }

    it("should not forward the x-trans-id header if disabled") {
      val mockSystemModelNoTracing = mock[SystemModel]
      val mockTracingHeaderConfig = mock[TracingHeaderConfig]
      when(mockSystemModelNoTracing.getTracingHeader).thenReturn(mockTracingHeaderConfig)
      when(mockTracingHeaderConfig.isEnabled).thenReturn(false)
      filter.SystemModelConfigListener.configurationUpdated(mockSystemModelNoTracing)

      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)
      request.addHeader(CommonHttpHeader.TRACE_GUID.toString, "test-guid")

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn(null, "glibglob")

      when(mockAkkaServiceClient.post(anyString(), anyString(), anyMapOf(classOf[String], classOf[String]), anyString(), any[MediaType], anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, adminAuthenticationTokenResponse()))
      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse()))
      when(mockAkkaServiceClient.get(mockitoEq(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, endpointsResponse()))
      when(mockAkkaServiceClient.get(mockitoEq(s"$GROUPS_KEY_PREFIX$VALID_USER_ID"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, groupsResponse()))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

      verify(mockAkkaServiceClient, never())
        .post(anyString(), anyString(), argThat(hasEntry(CommonHttpHeader.TRACE_GUID.toString, "test-guid")), anyString(), any[MediaType], anyBoolean())
      verify(mockAkkaServiceClient, never())
        .get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.TRACE_GUID.toString, "test-guid")), anyBoolean())
      verify(mockAkkaServiceClient, never())
        .get(mockitoEq(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.TRACE_GUID.toString, "test-guid")), anyBoolean())
      verify(mockAkkaServiceClient, never())
        .get(mockitoEq(s"$GROUPS_KEY_PREFIX$VALID_USER_ID"), anyString(), argThat(hasEntry(CommonHttpHeader.TRACE_GUID.toString, "test-guid")), anyBoolean())
    }
  }

  describe("Self-validating tokens") {
    def configuration = Marshaller.keystoneV2ConfigFromString(
      """<?xml version="1.0" encoding="UTF-8"?>
        |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
        |    <identity-service
        |            uri="https://some.identity.com"
        |            set-groups-in-header="false"
        |            />
        |</keystone-v2>
      """.stripMargin)

    val filter = new KeystoneV2Filter(mockConfigurationService, mockAkkaServiceClientFactory, mock[AtomFeedService], mockDatastoreService)

    filter.init(mockFilterConfig)
    filter.KeystoneV2ConfigListener.configurationUpdated(configuration)
    filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

    it("should validate a token without using an admin token") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse()))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)
    }

    it("rejects with 413 if we are rate limited by identity (413)") {
      val retryValue = DateUtils.formatDate(new DateTime().plusHours(1).toDate)

      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)), anyBoolean()))
        .thenReturn(new ServiceClientResponse(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, Array(HttpHeaders.RETRY_AFTER -> retryValue), "Rate limited by identity!"))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getErrorCode shouldBe HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE
      response.getHeader(HttpHeaders.RETRY_AFTER) shouldBe retryValue

      filterChain.getLastRequest should be(null)
      filterChain.getLastResponse should be(null)
    }

    it("rejects with 429 if we are rate limited by identity (429)") {
      val retryValue = DateUtils.formatDate(new DateTime().plusHours(1).toDate)

      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)), anyBoolean()))
        .thenReturn(new ServiceClientResponse(SC_TOO_MANY_REQUESTS, Array(HttpHeaders.RETRY_AFTER -> retryValue), "Rate limited by identity!"))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getErrorCode shouldBe SC_TOO_MANY_REQUESTS
      response.getHeader(HttpHeaders.RETRY_AFTER) shouldBe retryValue

      filterChain.getLastRequest should be(null)
      filterChain.getLastResponse should be(null)
    }
  }

  describe("Configured to make all requests") {
    def configuration = Marshaller.keystoneV2ConfigFromString(
      """<?xml version="1.0" encoding="UTF-8"?>
        |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
        |    <identity-service
        |            username="username"
        |            password="password"
        |            uri="https://some.identity.com"
        |            set-groups-in-header="true"
        |            set-catalog-in-header="true"
        |            />
        |</keystone-v2>
      """.stripMargin)

    val filter = new KeystoneV2Filter(mockConfigurationService, mockAkkaServiceClientFactory, mock[AtomFeedService], mockDatastoreService)

    filter.init(mockFilterConfig)
    filter.KeystoneV2ConfigListener.configurationUpdated(configuration)
    filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

    it("does not use the akka cache on retry for token validation") {
      val filterChain = new MockFilterChain()
      val response = new MockHttpServletResponse
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)
      when(mockAkkaServiceClient.get(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(
          new ServiceClientResponse(HttpServletResponse.SC_UNAUTHORIZED, ""),
          new ServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse()))
      when(mockDatastore.get(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")).thenReturn(EndpointsData("", Vector.empty[Endpoint]), Nil: _*)
      when(mockDatastore.get(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")).thenReturn(Vector("group"), Nil: _*)

      filter.doFilter(request, response, filterChain)

      verify(mockAkkaServiceClient).get(anyString(), anyString(), anyMapOf(classOf[String], classOf[String]), mockitoEq(true))
      verify(mockAkkaServiceClient).get(anyString(), anyString(), anyMapOf(classOf[String], classOf[String]), mockitoEq(false))
    }

    it("does not use the akka cache on retry for endpoint retrieval") {
      val filterChain = new MockFilterChain()
      val response = new MockHttpServletResponse
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)
      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(), Nil: _*)
      when(mockAkkaServiceClient.get(mockitoEq(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(
          new ServiceClientResponse(HttpServletResponse.SC_UNAUTHORIZED, ""),
          new ServiceClientResponse(HttpServletResponse.SC_OK, oneEndpointResponse()))
      when(mockDatastore.get(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")).thenReturn(Vector("group"), Nil: _*)

      filter.doFilter(request, response, filterChain)

      verify(mockAkkaServiceClient).get(anyString(), anyString(), anyMapOf(classOf[String], classOf[String]), mockitoEq(true))
      verify(mockAkkaServiceClient).get(anyString(), anyString(), anyMapOf(classOf[String], classOf[String]), mockitoEq(false))
    }

    it("does not use the akka cache on retry for groups retrieval") {
      val filterChain = new MockFilterChain()
      val response = new MockHttpServletResponse
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)
      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(userId = "userId"), Nil: _*)
      when(mockDatastore.get(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")).thenReturn(EndpointsData("", Vector.empty[Endpoint]), Nil: _*)
      when(mockAkkaServiceClient.get(mockitoEq(s"${GROUPS_KEY_PREFIX}userId"), anyString(), argThat(hasEntry(CommonHttpHeader.AUTH_TOKEN.toString, "glibglob")), anyBoolean()))
        .thenReturn(
          new ServiceClientResponse(HttpServletResponse.SC_UNAUTHORIZED, ""),
          new ServiceClientResponse(HttpServletResponse.SC_OK, groupsResponse()))

      filter.doFilter(request, response, filterChain)

      verify(mockAkkaServiceClient).get(anyString(), anyString(), anyMapOf(classOf[String], classOf[String]), mockitoEq(true))
      verify(mockAkkaServiceClient).get(anyString(), anyString(), anyMapOf(classOf[String], classOf[String]), mockitoEq(false))
    }
  }

  implicit def stringToInputStream(s: String): InputStream = new ByteArrayInputStream(s.getBytes)

  implicit def looseToStrictStringMap(sm: java.util.Map[_ <: String, _ <: String]): java.util.Map[String, String] =
    sm.asInstanceOf[java.util.Map[String, String]]

  implicit def tupleToHeader(t: (String, String)): Header = new BasicHeader(t._1, t._2)

  object TestValidToken {
    def apply(expirationDate: String = "",
              userId: String = "",
              roles: Seq[Role] = Seq.empty[Role],
              username: Option[String] = None,
              tenantName: Option[String] = None,
              defaultTenantId: Option[String] = None,
              tenantIds: Seq[String] = Seq.empty[String],
              impersonatorId: Option[String] = None,
              impersonatorName: Option[String] = None,
              impersonatorRoles: Seq[String] = Seq.empty[String],
              defaultRegion: Option[String] = None,
              contactId: Option[String] = None,
              authenticatedBy: Option[Seq[String]] = None) = {
      ValidToken(expirationDate,
        userId,
        roles,
        username,
        tenantName,
        defaultTenantId,
        tenantIds,
        impersonatorId,
        impersonatorName,
        impersonatorRoles,
        defaultRegion,
        contactId,
        authenticatedBy)
    }
  }

}
