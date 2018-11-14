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

import java.io.{ByteArrayInputStream, IOException, InputStream}
import java.net.URL
import java.util.concurrent.TimeUnit

import com.rackspace.httpdelegation.{HttpDelegationHeaderNames, HttpDelegationManager}
import javax.servlet.http.HttpServletResponse.{SC_UNAUTHORIZED, _}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.servlet.{FilterConfig, Servlet, ServletRequest, ServletResponse}
import javax.ws.rs.core.HttpHeaders._
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.{CloseableHttpResponse, HttpGet, HttpPost, HttpUriRequest}
import org.apache.http.client.utils.DateUtils
import org.apache.http.message.{BasicHeader, BasicHttpResponse}
import org.apache.http.protocol.HttpContext
import org.apache.http.{Header, HttpEntity, HttpVersion}
import org.hamcrest.Matchers.{both, greaterThanOrEqualTo, lessThanOrEqualTo}
import org.hamcrest.{Matchers => HC}
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.mockito.AdditionalMatchers._
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => mockitoEq, _}
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.openrepose.commons.test.HttpContextMatchers._
import org.openrepose.commons.test.HttpUriRequestMatchers._
import org.openrepose.commons.utils.http._
import org.openrepose.commons.utils.string.Base64Helper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.types.SetPatch
import org.openrepose.core.services.datastore.{Datastore, DatastoreService}
import org.openrepose.core.services.httpclient.{CachingHttpClientContext, HttpClientService, HttpClientServiceClient}
import org.openrepose.core.systemmodel.config.{SystemModel, TracingHeaderConfig}
import org.openrepose.filters.keystonev2.KeystoneRequestHandler._
import org.openrepose.filters.keystonev2.KeystoneV2Common._
import org.openrepose.filters.keystonev2.KeystoneV2TestCommon.createValidToken
import org.openrepose.filters.keystonev2.config.{KeystoneV2AuthenticationConfig, ServiceEndpointType}
import org.openrepose.nodeservice.atomfeed.AtomFeedService
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec}
import org.springframework.mock.web.{MockFilterChain, MockHttpServletRequest, MockHttpServletResponse}
import play.api.libs.json.Json

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
  private val mockHttpClient = mock[HttpClientServiceClient]
  private val mockHttpClientService = mock[HttpClientService]
  when(mockHttpClientService.getClient(or(anyString(), isNull.asInstanceOf[String]))).thenReturn(mockHttpClient)
  private val mockDatastore = mock[Datastore]
  private val mockDatastoreService = mock[DatastoreService]
  private val mockConfigurationService = mock[ConfigurationService]
  when(mockDatastoreService.getDefaultDatastore).thenReturn(mockDatastore)
  private val mockSystemModel = mock[SystemModel]
  private val mockTracingHeader = mock[TracingHeaderConfig]
  when(mockSystemModel.getTracingHeader).thenReturn(mockTracingHeader)
  when(mockTracingHeader.isEnabled).thenReturn(true, Nil: _*)
  private val mockFilterConfig = mock[FilterConfig]

  override def beforeEach(): Unit = {
    reset(mockDatastore)
    reset(mockConfigurationService)
    reset(mockHttpClient)
  }

  describe("Filter lifecycle") {
    val filter = new KeystoneV2Filter(mockConfigurationService, mockHttpClientService, mock[AtomFeedService], mockDatastoreService)

    it("should throw 500 if filter is not initialized") {
      val request = new MockHttpServletRequest
      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain
      filter.isInitialized shouldBe false
      filter.doFilter(request, response, filterChain)
      response.getStatus shouldBe SC_INTERNAL_SERVER_ERROR
    }

    it("should subscribe a listener to the configuration service on init") {
      filter.init(mockFilterConfig)

      verify(mockConfigurationService).subscribeTo(
        anyString(),
        anyString(),
        any[URL],
        any(),
        any[Class[KeystoneV2AuthenticationConfig]]
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
  }

  describe("Configured connection pool id") {
    it("asks the akka service client factory for an instance with the configured connection pool id") {
      def configuration = Marshaller.keystoneV2ConfigFromString(
        """<?xml version="1.0" encoding="UTF-8"?>
          |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
          |    <identity-service uri="https://some.identity.com" connection-pool-id="potato_pool"/>
          |</keystone-v2>
        """.stripMargin)

      val mockHttpClient = mock[HttpClientServiceClient]
      val mockHttpService = mock[HttpClientService]
      when(mockHttpService.getClient(or(anyString(), isNull.asInstanceOf[String]))).thenReturn(mockHttpClient)
      val filter = new KeystoneV2Filter(mockConfigurationService, mockHttpClientService, mock[AtomFeedService], mockDatastoreService)
      filter.configurationUpdated(configuration)

      verify(mockHttpClientService).getClient("potato_pool")
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

    val filter = new KeystoneV2Filter(mockConfigurationService, mockHttpClientService, mock[AtomFeedService], mockDatastoreService)

    filter.init(mockFilterConfig)
    filter.configurationUpdated(configuration)
    filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

    it("Validates a token allowing through the filter") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn(null, "glibglob")
      when(mockHttpClient.execute(argThat(hasMethod(HttpPost.METHOD_NAME)), any[HttpContext]))
        .thenReturn(makeResponse(
          SC_OK,
          EntityBuilder.create()
            .setText(adminAuthenticationTokenResponse())
            .build()
        ))
      when(mockHttpClient.execute(argThat(hasMethod(HttpGet.METHOD_NAME)), any[HttpContext]))
        .thenReturn(makeResponse(
          SC_OK,
          EntityBuilder.create()
            .setText(validateTokenResponse())
            .build()
        ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest shouldNot be(null)
      filterChain.getResponse shouldNot be(null)
    }

    it("should handle identity service uri ending with a '/'") {
      val keystoneFilter = new KeystoneV2Filter(mockConfigurationService, mockHttpClientService, mock[AtomFeedService], mockDatastoreService)

      val modifiedConfig = configuration
      modifiedConfig.getIdentityService.setUri("https://some.identity.com/")
      keystoneFilter.configurationUpdated(modifiedConfig)
      keystoneFilter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockHttpClient.execute(any[HttpUriRequest], any[HttpContext]))
        .thenReturn(makeResponse(SC_OK, EntityBuilder.create().setText(validateTokenResponse()).build()))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      keystoneFilter.doFilter(request, response, filterChain)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpUriRequest])
      verify(mockHttpClient).execute(
        requestCaptor.capture(),
        any[HttpContext]
      )

      val capturedRequest = requestCaptor.getValue
      filterChain.getRequest shouldNot be(null)
      filterChain.getResponse shouldNot be(null)
      capturedRequest.getMethod shouldEqual HttpGet.METHOD_NAME
      capturedRequest.getURI.toString shouldEqual s"https://some.identity.com$TOKEN_ENDPOINT/$VALID_TOKEN"
    }

    it("caches the admin token request for 10 minutes") {
      //Can only make sure it was put into the cache with a 10 minute timeout...
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn(null, "glibglob")

      when(mockHttpClient.execute(argThat(hasMethod(HttpPost.METHOD_NAME)), any[HttpContext]))
        .thenReturn(makeResponse(
          SC_OK,
          EntityBuilder.create()
            .setText(adminAuthenticationTokenResponse())
            .build()
        ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponse())
          .build()
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      verify(mockDatastore).put(ADMIN_TOKEN_KEY, "glibglob")

      filterChain.getRequest shouldNot be(null)
      filterChain.getResponse shouldNot be(null)
    }

    it("caches a valid token for 10 minutes") {
      //Can only make sure it was put into the cache with a 10 minute timeout...
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn(null, "glibglob")

      when(mockHttpClient.execute(argThat(hasMethod(HttpPost.METHOD_NAME)), any[HttpContext]))
        .thenReturn(makeResponse(
          SC_OK,
          EntityBuilder.create()
            .setText(adminAuthenticationTokenResponse())
            .build()
        ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponse())
          .build()
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      verify(mockDatastore).put(ADMIN_TOKEN_KEY, "glibglob")
      //Have to cache the result of the stuff
      verify(mockDatastore).put(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"),
        any(classOf[ValidToken]),
        mockitoEq(600),
        mockitoEq(TimeUnit.SECONDS))

      filterChain.getRequest shouldNot be(null)
      filterChain.getResponse shouldNot be(null)
    }

    it("Makes no other calls if the token is already cached with a valid result") {
      //Can only make sure it was put into the cache with a 10 minute timeout...
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      //When the user's token details are cached, no calls to identity should take place

      //When we ask the cache for our token, it works
      // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures
      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(createValidToken(roles = Vector(Role("compute:admin"), Role("object-store:admin"))), Nil: _*)
      // Doesn't update the User to Token cache.
      verify(mockDatastore, never()).put(any(), any(), mockitoEq(600), mockitoEq(TimeUnit.SECONDS))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest shouldNot be(null)
      filterChain.getResponse shouldNot be(null)
    }

    it("rejects with 401 an invalid token") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, "notValidToken")

      when(mockHttpClient.execute(argThat(hasMethod(HttpPost.METHOD_NAME)), any[HttpContext]))
        .thenReturn(makeResponse(
          SC_OK,
          EntityBuilder.create()
            .setText(adminAuthenticationTokenResponse())
            .build()
        ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"${TOKEN_KEY_PREFIX}notValidToken")
        ))
      )).thenReturn(makeResponse(SC_NOT_FOUND))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest shouldBe null
      filterChain.getResponse shouldBe null

      response.getStatus shouldBe SC_UNAUTHORIZED
      response.getHeader(WWW_AUTHENTICATE) shouldBe "Keystone uri=https://some.identity.com"
    }

    it("rejects with 403 if no x-auth-token is present") {
      //No auth token, no interactions with identity at all!
      val request = new MockHttpServletRequest()

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest shouldBe null
      filterChain.getResponse shouldBe null

      response.getStatus shouldBe SC_UNAUTHORIZED
      response.getHeader(WWW_AUTHENTICATE) shouldBe "Keystone uri=https://some.identity.com"
    }

    it("retries authentication as the admin user if the admin token is not valid") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockHttpClient.execute(argThat(hasMethod(HttpPost.METHOD_NAME)), any[HttpContext]))
        .thenReturn(
          makeResponse(
            SC_OK,
            EntityBuilder.create()
              .setText(adminAuthenticationTokenResponse())
              .build()
          ),
          makeResponse(
            SC_OK,
            EntityBuilder.create()
              .setText(adminAuthenticationTokenResponse(token = "morty"))
              .build()
          ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(SC_UNAUTHORIZED))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("morty"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponse())
          .build()
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest shouldNot be(null)
      filterChain.getResponse shouldNot be(null)
    }

    it("rejects with 500 if the admin token is not authorized to validate tokens (401)") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockHttpClient.execute(argThat(hasMethod(HttpPost.METHOD_NAME)), any[HttpContext]))
        .thenReturn(makeResponse(
          SC_OK,
          EntityBuilder.create()
            .setText(adminAuthenticationTokenResponse())
            .build()
        ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(SC_UNAUTHORIZED))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getStatus shouldBe SC_INTERNAL_SERVER_ERROR

      filterChain.getRequest shouldBe null
      filterChain.getResponse shouldBe null
    }

    it("rejects with 500 if the admin token is not authorized to validate tokens (403)") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockHttpClient.execute(argThat(hasMethod(HttpPost.METHOD_NAME)), any[HttpContext]))
        .thenReturn(makeResponse(
          SC_OK,
          EntityBuilder.create()
            .setText(adminAuthenticationTokenResponse())
            .build()
        ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(SC_FORBIDDEN))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getStatus shouldBe SC_INTERNAL_SERVER_ERROR

      filterChain.getRequest shouldBe null
      filterChain.getResponse shouldBe null
    }

    it("rejects with 500 if we cannot reach identity") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockHttpClient.execute(argThat(hasMethod(HttpGet.METHOD_NAME)), any[HttpContext]))
        .thenThrow(new IOException("Unable to reach identity!"))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getStatus shouldBe SC_INTERNAL_SERVER_ERROR

      filterChain.getRequest shouldBe null
      filterChain.getResponse shouldBe null
    }

    it("rejects with 500 if we cannot authenticate as the admin user") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockHttpClient.execute(argThat(hasMethod(HttpPost.METHOD_NAME)), any[HttpContext]))
        .thenReturn(makeResponse(
          SC_OK,
          EntityBuilder.create()
            .setText(adminAuthenticationTokenResponse())
            .build()
        ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenThrow(new IOException("Unable to talk to identity!"))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getStatus shouldBe SC_INTERNAL_SERVER_ERROR

      filterChain.getRequest shouldBe null
      filterChain.getResponse shouldBe null
    }

    it("rejects with 503 if we are rate limited by identity (413)") {
      val retryValue = DateUtils.formatDate(new DateTime().plusHours(1).toDate)

      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockHttpClient.execute(argThat(hasMethod(HttpPost.METHOD_NAME)), any[HttpContext]))
        .thenReturn(makeResponse(
          SC_REQUEST_ENTITY_TOO_LARGE,
          EntityBuilder.create()
            .setText("Rate limited by identity!")
            .build(),
          Array[Header](RETRY_AFTER -> retryValue)
        ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getStatus shouldBe SC_SERVICE_UNAVAILABLE
      response.getHeader(RETRY_AFTER) shouldBe retryValue

      filterChain.getRequest shouldBe null
      filterChain.getResponse shouldBe null
    }

    it("rejects with 503 if we are rate limited by identity (429)") {
      val retryValue = DateUtils.formatDate(new DateTime().plusHours(1).toDate)

      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockHttpClient.execute(argThat(hasMethod(HttpPost.METHOD_NAME)), any[HttpContext]))
        .thenReturn(makeResponse(
          SC_TOO_MANY_REQUESTS,
          EntityBuilder.create()
            .setText("Rate limited by identity!")
            .build(),
          Array[Header](RETRY_AFTER -> retryValue)
        ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getStatus shouldBe SC_SERVICE_UNAVAILABLE
      response.getHeader(RETRY_AFTER) shouldBe retryValue

      filterChain.getRequest shouldBe null
      filterChain.getResponse shouldBe null
    }

    it("does not forward the authenticatedBy field if it is an empty array") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)


      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponseAuthenticatedBy())
          .build()
      ))

      val response = new MockHttpServletResponse()
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest should not be null
      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.AUTHENTICATED_BY) shouldBe null
    }

    it("forwards the authenticatedBy field if present and non-empty") {
      val authMethods = Seq("password")
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponseAuthenticatedBy(authenticatedBy = authMethods))
          .build()
      ))

      val response = new MockHttpServletResponse()
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest should not be null
      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.AUTHENTICATED_BY) shouldBe "password"
    }

    it("forwards all values from the authenticatedBy field") {
      val authMethods = Seq("PASSWORD", "PASSCODE")
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponseAuthenticatedBy(authenticatedBy = authMethods))
          .build()
      ))

      val response = new MockHttpServletResponse()
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest should not be null
      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeaders(OpenStackServiceHeader.AUTHENTICATED_BY).asScala.toSeq should contain only("PASSWORD", "PASSCODE")
    }

    it("responds with a www-authenticate header when unauthorized down stream") {
      val response = new MockHttpServletResponse
      val mockServlet = mock[Servlet]
      doAnswer(new Answer[Unit] {
        override def answer(invocation: InvocationOnMock): Unit = {
          val resp = invocation.getArguments()(1).asInstanceOf[HttpServletResponse]
          resp.setHeader(WWW_AUTHENTICATE, "Delegated")
          resp.setStatus(SC_UNAUTHORIZED)
        }
      }).when(mockServlet).service(any[ServletRequest](), any[ServletResponse]())
      val filterChain = new MockFilterChain(mockServlet)

      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn(null, "glibglob")
      when(mockHttpClient.execute(argThat(hasMethod(HttpPost.METHOD_NAME)), any[HttpContext]))
        .thenReturn(makeResponse(
          SC_OK,
          EntityBuilder.create()
            .setText(adminAuthenticationTokenResponse())
            .build()
        ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponse())
          .build()
      ))

      filter.doFilter(request, response, filterChain)

      response.getStatus shouldBe SC_UNAUTHORIZED
      response.getHeaders(WWW_AUTHENTICATE) should contain("Keystone uri=https://some.identity.com")
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

    val filter = new KeystoneV2Filter(mockConfigurationService, mockHttpClientService, mock[AtomFeedService], mockDatastoreService)

    filter.init(mockFilterConfig)
    filter.configurationUpdated(configuration)
    filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

    it("allows a user through if they have the endpoint configured in their endpoints list") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponse())
          .build()
      ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(endpointsResponse())
          .build()
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      //Continues with the chain
      filterChain.getRequest shouldNot be(null)
      filterChain.getResponse shouldNot be(null)
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

      val testFilter = new KeystoneV2Filter(mockConfigurationService, mockHttpClientService, mock[AtomFeedService], mockDatastoreService)
      testFilter.configurationUpdated(configurationDos)
      testFilter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponse())
          .build()
      ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(endpointsResponse())
          .build()
      ))

      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      testFilter.doFilter(request, response, filterChain)

      //Continues with the chain
      filterChain.getRequest shouldNot be(null)
      filterChain.getResponse shouldNot be(null)
    }

    it("handles 203 response from endpoints call") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(createValidToken(), Nil: _*)
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_NON_AUTHORITATIVE_INFORMATION,
        EntityBuilder.create()
          .setText(endpointsResponse())
          .build()
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      //Continues with the chain
      filterChain.getRequest shouldNot be(null)
      filterChain.getResponse shouldNot be(null)
    }

    it("handles 403 response from endpoints call") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(createValidToken(), Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(SC_FORBIDDEN))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest shouldBe null
      filterChain.getResponse shouldBe null

      response.getStatus shouldBe SC_INTERNAL_SERVER_ERROR
    }

    it("handles 401 response from endpoints call") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(createValidToken(), Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(
        makeResponse(
          SC_UNAUTHORIZED,
          EntityBuilder.create()
            .setText(endpointsResponse())
            .build()),
        makeResponse(
          SC_UNAUTHORIZED
        )
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getStatus shouldBe SC_INTERNAL_SERVER_ERROR
    }

    it("handles akka service client call failing") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(createValidToken(), Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenThrow(new IOException("Unable to reach identity!"))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      //Continues with the chain
      filterChain.getRequest shouldBe null
      filterChain.getResponse shouldBe null

      response.getStatus shouldBe SC_INTERNAL_SERVER_ERROR
    }

    it("handles unexpected response from endpoints call") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(createValidToken(), Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(SC_NOT_IMPLEMENTED))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest shouldBe null
      filterChain.getResponse shouldBe null

      response.getStatus shouldBe SC_BAD_GATEWAY
    }

    it("rejects with 403 if the user does not have the required endpoint") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(createValidToken(), Nil: _*)
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(oneEndpointResponse())
          .build()
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getStatus shouldBe SC_FORBIDDEN
      //Continues with the chain
      filterChain.getRequest shouldBe null
      filterChain.getResponse shouldBe null
    }

    it("bypasses validation if the user has the role listed in pre-authorized-roles") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateRackerTokenResponse())
          .build()
      ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(oneEndpointResponse())
          .build()
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest shouldNot be(null)
      filterChain.getResponse shouldNot be(null)
    }

    it("rejects with 401 if the token is no longer valid when catalog variable is set") {
      val modifiedConfig = configuration
      modifiedConfig.getIdentityService.setSetCatalogInHeader(true)
      filter.configurationUpdated(modifiedConfig)

      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(createValidToken(), Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(SC_NOT_FOUND))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.configurationUpdated(configuration)

      response.getStatus shouldBe SC_UNAUTHORIZED
      //Continues with the chain
      filterChain.getRequest shouldBe null
      filterChain.getResponse shouldBe null
    }

    describe("when endpoints are cached") {
      it("will reject if the user doesn't have the endpoint") {
        //make a request and validate that it called the akka service client?
        val request = new MockHttpServletRequest()
        request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

        //Pretend like the admin token is cached all the time
        when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

        when(mockHttpClient.execute(
          argThat(HC.allOf[HttpUriRequest](
            hasMethod(HttpGet.METHOD_NAME),
            hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
          )),
          argThat(hasAttribute(
            CachingHttpClientContext.CACHE_KEY,
            HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
          ))
        )).thenReturn(makeResponse(
          SC_OK,
          EntityBuilder.create()
            .setText(validateTokenResponse())
            .build()
        ))

        val endpointsList = Vector(Endpoint(Some("DERP"), Some("Compute"), Some("compute"), "https://compute.north.public.com/v1"))
        when(mockDatastore.get(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")).thenReturn(EndpointsData("", endpointsList), Nil: _*)

        val response = new MockHttpServletResponse
        val filterChain = new MockFilterChain()
        filter.doFilter(request, response, filterChain)

        response.getStatus shouldBe SC_FORBIDDEN
        filterChain.getRequest shouldBe null
        filterChain.getResponse shouldBe null
      }

      it("will allow through if the user has the endpoint") {
        //make a request and validate that it called the akka service client?
        val request = new MockHttpServletRequest()
        request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

        //Pretend like the admin token is cached all the time
        when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

        when(mockHttpClient.execute(
          argThat(HC.allOf[HttpUriRequest](
            hasMethod(HttpGet.METHOD_NAME),
            hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
          )),
          argThat(hasAttribute(
            CachingHttpClientContext.CACHE_KEY,
            HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
          ))
        )).thenReturn(makeResponse(
          SC_OK,
          EntityBuilder.create()
            .setText(validateTokenResponse())
            .build()
        ))

        val endpointsList = Vector(Endpoint(Some("Global"), Some("Compute"), Some("compute"), "https://compute.north.public.com/v1"))
        when(mockDatastore.get(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")).thenReturn(EndpointsData("", endpointsList), Nil: _*)

        val response = new MockHttpServletResponse
        val filterChain = new MockFilterChain()
        filter.doFilter(request, response, filterChain)

        filterChain.getRequest shouldNot be(null)
        filterChain.getResponse shouldNot be(null)
      }

      it("will allow through if the user matches the bypass roles") {
        //make a request and validate that it called the akka service client?
        val request = new MockHttpServletRequest()
        request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

        //Pretend like the admin token is cached all the time
        when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

        when(mockHttpClient.execute(
          argThat(HC.allOf[HttpUriRequest](
            hasMethod(HttpGet.METHOD_NAME),
            hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
          )),
          argThat(hasAttribute(
            CachingHttpClientContext.CACHE_KEY,
            HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
          ))
        )).thenReturn(makeResponse(
          SC_OK,
          EntityBuilder.create()
            .setText(validateRackerTokenResponse())
            .build()
        ))

        val endpointsList = Vector(Endpoint(Some("DERP"), Some("LOLNOPE"), Some("compute"), "https://compute.north.public.com/v1"))
        when(mockDatastore.get(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")).thenReturn(EndpointsData("", endpointsList), Nil: _*)

        val response = new MockHttpServletResponse
        val filterChain = new MockFilterChain()
        filter.doFilter(request, response, filterChain)

        filterChain.getRequest shouldNot be(null)
        filterChain.getResponse shouldNot be(null)
      }

      it("rejects with 503 if we are rate limited by identity (413)") {
        val retryValue = DateUtils.formatDate(new DateTime().plusHours(1).toDate)

        val request = new MockHttpServletRequest()
        request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

        when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

        when(mockHttpClient.execute(
          argThat(HC.allOf[HttpUriRequest](
            hasMethod(HttpGet.METHOD_NAME),
            hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
          )),
          argThat(hasAttribute(
            CachingHttpClientContext.CACHE_KEY,
            HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
          ))
        )).thenReturn(makeResponse(
          SC_REQUEST_ENTITY_TOO_LARGE,
          EntityBuilder.create()
            .setText("Rate limited by identity!")
            .build(),
          Array[Header](RETRY_AFTER -> retryValue)
        ))

        when(mockDatastore.get(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")).thenReturn(EndpointsData("", Vector.empty), Nil: _*)

        val response = new MockHttpServletResponse
        val filterChain = new MockFilterChain()
        filter.doFilter(request, response, filterChain)

        response.getStatus shouldBe SC_SERVICE_UNAVAILABLE
        response.getHeader(RETRY_AFTER) shouldBe retryValue

        filterChain.getRequest shouldBe null
        filterChain.getResponse shouldBe null
      }

      it("rejects with 503 if we are rate limited by identity (429)") {
        val retryValue = DateUtils.formatDate(new DateTime().plusHours(1).toDate)

        val request = new MockHttpServletRequest()
        request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

        when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

        when(mockHttpClient.execute(
          argThat(HC.allOf[HttpUriRequest](
            hasMethod(HttpGet.METHOD_NAME),
            hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
          )),
          argThat(hasAttribute(
            CachingHttpClientContext.CACHE_KEY,
            HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
          ))
        )).thenReturn(makeResponse(
          SC_TOO_MANY_REQUESTS,
          EntityBuilder.create()
            .setText("Rate limited by identity!")
            .build(),
          Array[Header](RETRY_AFTER -> retryValue)
        ))

        when(mockDatastore.get(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")).thenReturn(EndpointsData("", Vector.empty), Nil: _*)

        val response = new MockHttpServletResponse
        val filterChain = new MockFilterChain()
        filter.doFilter(request, response, filterChain)

        response.getStatus shouldBe SC_SERVICE_UNAVAILABLE
        response.getHeader(RETRY_AFTER) shouldBe retryValue

        filterChain.getRequest shouldBe null
        filterChain.getResponse shouldBe null
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

    val filter = new KeystoneV2Filter(mockConfigurationService, mockHttpClientService, mock[AtomFeedService], mockDatastoreService)

    filter.init(mockFilterConfig)
    filter.configurationUpdated(configuration)
    filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

    it("will allow through if the user has the group cached") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponse())
          .build()
      ))

      val groupsList = Vector("DERP")
      when(mockDatastore.get(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")).thenReturn(groupsList, Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest shouldNot be(null)
      filterChain.getResponse shouldNot be(null)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(PowerApiHeader.GROUPS) shouldBe "DERP"
    }

    it("handles when serviceClientResponse.getStatus fails") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(createValidToken(userId = VALID_USER_ID), Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$GROUPS_KEY_PREFIX$VALID_USER_ID")
        ))
      )).thenThrow(new IOException("Unable to reach identity!"))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getStatus shouldBe SC_INTERNAL_SERVER_ERROR
      filterChain.getRequest shouldBe null
    }

    it("handles 401 response from groups call") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(createValidToken(userId = VALID_USER_ID), Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$GROUPS_KEY_PREFIX$VALID_USER_ID")
        ))
      )).thenReturn(makeResponse(SC_UNAUTHORIZED))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getStatus shouldBe SC_INTERNAL_SERVER_ERROR
      filterChain.getRequest shouldBe null
    }

    it("handles 403 response from groups call") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(createValidToken(userId = VALID_USER_ID), Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$GROUPS_KEY_PREFIX$VALID_USER_ID")
        ))
      )).thenReturn(makeResponse(SC_FORBIDDEN))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getStatus shouldBe SC_INTERNAL_SERVER_ERROR
      filterChain.getRequest shouldBe null
    }

    it("handles 413 response from groups call") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(createValidToken(userId = VALID_USER_ID), Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$GROUPS_KEY_PREFIX$VALID_USER_ID")
        ))
      )).thenReturn(makeResponse(
        SC_REQUEST_ENTITY_TOO_LARGE,
        EntityBuilder.create()
          .setText("Rate limited by identity!")
          .build()
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getStatus shouldBe SC_SERVICE_UNAVAILABLE
      filterChain.getRequest shouldBe null
    }

    it("handles 429 response from groups call") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(createValidToken(userId = VALID_USER_ID), Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$GROUPS_KEY_PREFIX$VALID_USER_ID")
        ))
      )).thenReturn(makeResponse(
        SC_TOO_MANY_REQUESTS,
        EntityBuilder.create()
          .setText("Rate limited by identity!")
          .build()
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getStatus shouldBe SC_SERVICE_UNAVAILABLE
      filterChain.getRequest shouldBe null
    }

    it("handles unexpected response from groups call") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(createValidToken(userId = VALID_USER_ID), Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$GROUPS_KEY_PREFIX$VALID_USER_ID")
        ))
      )).thenReturn(makeResponse(SC_NOT_IMPLEMENTED))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getStatus shouldBe SC_BAD_GATEWAY
      filterChain.getRequest shouldBe null
    }

    it("handles 404s from groups call by allowing users through with no X-PP-Groups") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(createValidToken(userId = VALID_USER_ID, roles = Seq(Role("Racker"))), Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$GROUPS_KEY_PREFIX$VALID_USER_ID")
        ))
      )).thenReturn(makeResponse(SC_NOT_FOUND))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(PowerApiHeader.GROUPS) shouldBe null
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

    val filter = new KeystoneV2Filter(mockConfigurationService, mockHttpClientService, mock[AtomFeedService], mockDatastoreService)

    filter.init(mockFilterConfig)
    filter.configurationUpdated(configuration)
    filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

    it("delegates with an invalid token and adds the header") {
      val request = new MockHttpServletRequest
      request.setServerName("www.sample.com")
      request.setRequestURI("/some/path/application.wadl")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, "INVALID_TOKEN")

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"${TOKEN_KEY_PREFIX}INVALID_TOKEN")
        ))
      )).thenReturn(makeResponse(SC_NOT_FOUND))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      val delegationHeader = parseDelegationHeader(filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(HttpDelegationHeaderNames.Delegated))
      delegationHeader shouldBe a[Success[_]]
      delegationHeader.get.statusCode shouldBe SC_UNAUTHORIZED
    }

    it("delegates if lacking the required service endpoint and all headers for data we have") {
      val modifiedConfig = configuration
      modifiedConfig.setRequireServiceEndpoint(new ServiceEndpointType().withPublicUrl("http://google.com/"))
      filter.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest
      request.setServerName("www.sample.com")
      request.setRequestURI("/some/path/application.wadl")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)
      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(createValidToken(), Nil: _*)
      when(mockDatastore.get(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")).thenReturn(EndpointsData("", Vector.empty[Endpoint]), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.configurationUpdated(configuration)

      val lastRequest = filterChain.getRequest.asInstanceOf[HttpServletRequest]
      val delegationHeader = parseDelegationHeader(lastRequest.getHeader(HttpDelegationHeaderNames.Delegated))
      delegationHeader shouldBe a[Success[_]]
      delegationHeader.get.statusCode shouldBe SC_FORBIDDEN
      lastRequest.getHeaderNames.asScala.toList should contain allOf(OpenStackServiceHeader.USER_ID,
        OpenStackServiceHeader.X_EXPIRATION,
        OpenStackServiceHeader.EXTENDED_AUTHORIZATION)
    }

    it("delegates if identity doesn't respond properly") {
      val request = new MockHttpServletRequest
      request.setServerName("www.sample.com")
      request.setRequestURI("/some/path/application.wadl")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText("butts")
          .build()
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      val delegationHeader = parseDelegationHeader(filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(HttpDelegationHeaderNames.Delegated))
      delegationHeader shouldBe a[Success[_]]
      delegationHeader.get.statusCode shouldBe SC_BAD_GATEWAY
    }

    it("delegates if the admin token is invalid") {
      val request = new MockHttpServletRequest
      request.setServerName("www.sample.com")
      request.setRequestURI("/some/path/application.wadl")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("invalid", null)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("invalid"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(SC_UNAUTHORIZED))
      when(mockHttpClient.execute(argThat(hasMethod(HttpPost.METHOD_NAME)), any[HttpContext]))
        .thenReturn(makeResponse(SC_NOT_FOUND))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      val delegationHeader = parseDelegationHeader(filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(HttpDelegationHeaderNames.Delegated))
      delegationHeader shouldBe a[Success[_]]
      delegationHeader.get.statusCode shouldBe SC_INTERNAL_SERVER_ERROR
    }

    it("forwards the identity status as Confirmed in the x-identity-status header when delegating success") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)
      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(createValidToken(), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.IDENTITY_STATUS) shouldBe IdentityStatus.CONFIRMED
    }

    it("forwards the identity status as Indeterminate in the x-identity-status header when delegating failure") {
      val request = new MockHttpServletRequest
      request.setServerName("www.sample.com")
      request.setRequestURI("/some/path/application.wadl")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, "INVALID_TOKEN")

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"${TOKEN_KEY_PREFIX}INVALID_TOKEN")
        ))
      )).thenReturn(makeResponse(SC_NOT_FOUND))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.IDENTITY_STATUS) shouldBe IdentityStatus.INDETERMINATE
    }

    it("responds with a www-authenticate header when delegating") {
      val response = new MockHttpServletResponse
      val mockServlet = mock[Servlet]
      doAnswer(new Answer[Unit] {
        override def answer(invocation: InvocationOnMock): Unit = {
          val resp = invocation.getArguments()(1).asInstanceOf[HttpServletResponse]
          resp.setHeader(WWW_AUTHENTICATE, "Delegated")
          resp.setStatus(SC_UNAUTHORIZED)
        }
      }).when(mockServlet).service(any[ServletRequest](), any[ServletResponse]())
      val filterChain = new MockFilterChain(mockServlet)

      val request = new MockHttpServletRequest
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, "notValidToken")

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"${TOKEN_KEY_PREFIX}notValidToken")
        ))
      )).thenReturn(makeResponse(SC_NOT_FOUND))

      filter.doFilter(request, response, filterChain)

      response.getStatus shouldBe SC_UNAUTHORIZED
      response.getHeaders(WWW_AUTHENTICATE) should contain("Keystone uri=https://some.identity.com")
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

    val filter = new KeystoneV2Filter(mockConfigurationService, mockHttpClientService, mock[AtomFeedService], mockDatastoreService)

    filter.init(mockFilterConfig)
    filter.configurationUpdated(configuration)
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

        filterChain.getRequest shouldNot be(null)
        filterChain.getResponse shouldNot be(null)
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
    val filter = new KeystoneV2Filter(mockConfigurationService, mockHttpClientService, mock[AtomFeedService], mockDatastoreService)

    filter.init(mockFilterConfig)
    filter.configurationUpdated(configuration)
    filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

    it("passes through the values to the distributed datastore for the proper cache timeouts") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn(null, "glibglob")

      when(mockHttpClient.execute(argThat(hasMethod(HttpPost.METHOD_NAME)), any[HttpContext]))
        .thenReturn(makeResponse(
          SC_OK,
          EntityBuilder.create()
            .setText(adminAuthenticationTokenResponse())
            .build()
        ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponse(userId = userId))
          .build()
      ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(endpointsResponse())
          .build()
      ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$GROUPS_KEY_PREFIX$userId")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(groupsResponse())
          .build()
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest shouldNot be(null)
      filterChain.getResponse shouldNot be(null)
      verify(mockDatastore).put(mockitoEq(s"$ADMIN_TOKEN_KEY"), any())
      verify(mockDatastore).patch(mockitoEq(s"$USER_ID_KEY_PREFIX$userId"), isA(classOf[SetPatch[String]]), mockitoEq(270), mockitoEq(TimeUnit.SECONDS))
      verify(mockDatastore).put(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), any(), mockitoEq(270), mockitoEq(TimeUnit.SECONDS))
      verify(mockDatastore).put(mockitoEq(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN"), any(), mockitoEq(330), mockitoEq(TimeUnit.SECONDS))
      verify(mockDatastore).put(mockitoEq(s"$GROUPS_KEY_PREFIX$VALID_TOKEN"), any(), mockitoEq(300), mockitoEq(TimeUnit.SECONDS))
    }

    it("passes through variable offsets within a range to the distributed datastore") {
      val modifiedConfig = configuration
      modifiedConfig.getCache.getTimeouts.setVariability(1)
      filter.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn(null, "glibglob")

      when(mockHttpClient.execute(argThat(hasMethod(HttpPost.METHOD_NAME)), any[HttpContext]))
        .thenReturn(makeResponse(
          SC_OK,
          EntityBuilder.create()
            .setText(adminAuthenticationTokenResponse())
            .build()
        ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponse(userId = userId))
          .build()
      ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(endpointsResponse())
          .build()
      ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$GROUPS_KEY_PREFIX$userId")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(groupsResponse())
          .build()
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.configurationUpdated(configuration)

      filterChain.getRequest shouldNot be(null)
      filterChain.getResponse shouldNot be(null)
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
      filter.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn(null, "glibglob")

      when(mockHttpClient.execute(argThat(hasMethod(HttpPost.METHOD_NAME)), any[HttpContext]))
        .thenReturn(makeResponse(
          SC_OK,
          EntityBuilder.create()
            .setText(adminAuthenticationTokenResponse())
            .build()
        ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponse(userId = userId))
          .build()
      ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(endpointsResponse())
          .build()
      ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$GROUPS_KEY_PREFIX$userId")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(groupsResponse())
          .build()
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest shouldNot be(null)
      filterChain.getResponse shouldNot be(null)
      verify(mockDatastore).put(mockitoEq(s"$ADMIN_TOKEN_KEY"), any())
      verify(mockDatastore).patch(mockitoEq(s"$USER_ID_KEY_PREFIX$userId"), isA(classOf[SetPatch[String]]), mockitoEq(600), mockitoEq(TimeUnit.SECONDS))
      verify(mockDatastore).put(mockitoEq(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"), any(), mockitoEq(600), mockitoEq(TimeUnit.SECONDS))
      verify(mockDatastore).put(mockitoEq(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN"), any(), mockitoEq(600), mockitoEq(TimeUnit.SECONDS))
      verify(mockDatastore).put(mockitoEq(s"$GROUPS_KEY_PREFIX$VALID_TOKEN"), any(), mockitoEq(600), mockitoEq(TimeUnit.SECONDS))

      filter.configurationUpdated(configuration)
    }
  }

  describe("Tenant handling is enabled") {
    val expectedTenantHeaderName = "X-Expected-Tenant"

    def configuration = Marshaller.keystoneV2ConfigFromString(
      s"""<?xml version="1.0" encoding="UTF-8"?>
        |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
        |    <identity-service
        |            username="username"
        |            password="password"
        |            uri="https://some.identity.com"
        |            set-groups-in-header="false"
        |            />
        |    <tenant-handling send-all-tenant-ids="true">
        |        <validate-tenant strip-token-tenant-prefixes="foo:/bar:">
        |            <header-extraction-name>$expectedTenantHeaderName</header-extraction-name>
        |        </validate-tenant>
        |        <send-tenant-id-quality default-tenant-quality="0.9" uri-tenant-quality="0.7" roles-tenant-quality="0.5"/>
        |    </tenant-handling>
        |    <pre-authorized-roles>
        |        <role>serviceAdmin</role>
        |        <role>racker</role>
        |    </pre-authorized-roles>
        |</keystone-v2>
      """.stripMargin)

    val filter = new KeystoneV2Filter(mockConfigurationService, mockHttpClientService, mock[AtomFeedService], mockDatastoreService)

    filter.init(mockFilterConfig)
    filter.configurationUpdated(configuration)
    filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

    it("will not require a default tenant ID") {
      val request = new MockHttpServletRequest()
      request.setServerName("www.sample.com")
      request.addHeader(expectedTenantHeaderName, "tenant")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(createValidToken(roles = Seq(Role("role1", Some("tenant"))), tenantIds = Seq("tenant")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest shouldNot be(null)
      filterChain.getResponse shouldNot be(null)
    }

    it("will extract the tenant from a header and validate that the user has that tenant in their list") {
      val request = new MockHttpServletRequest()
      request.setServerName("www.sample.com")
      request.addHeader(expectedTenantHeaderName, "tenant")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(createValidToken(defaultTenantId = Some("tenant")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest shouldNot be(null)
      filterChain.getResponse shouldNot be(null)
    }

    it("will extract the tenant from a header and validate that the user has a prefixed tenant as the default") {
      val request = new MockHttpServletRequest()
      request.setServerName("www.sample.com")
      request.addHeader(expectedTenantHeaderName, "tenant")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(createValidToken(defaultTenantId = Some("foo:tenant")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest shouldNot be(null)
      filterChain.getResponse shouldNot be(null)
    }

    it("will extract the tenant from a header and validate that the user has a prefixed tenant in their roles") {
      val request = new MockHttpServletRequest()
      request.setServerName("www.sample.com")
      request.addHeader(expectedTenantHeaderName, "tenant")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(createValidToken(defaultTenantId = Some("oof"), roles = Seq(Role("role1", Some("foo:tenant")), Role("role2", Some("rab"))), tenantIds = Seq("foo:tenant", "rab")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest shouldNot be(null)
      filterChain.getResponse shouldNot be(null)
    }

    it("will extract the tenant from a header and reject if the user does not have that tenant in their list") {
      val request = new MockHttpServletRequest()
      request.setServerName("www.sample.com")
      request.addHeader(expectedTenantHeaderName, "tenant")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(createValidToken(defaultTenantId = Some("not-tenant")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getStatus shouldBe SC_UNAUTHORIZED
      response.getErrorMessage shouldBe "A tenant from the configured header does not match any of the user's tenants"
    }

    it("will extract the tenant from a header and reject if the user only use the tenant on an ignored role") {
      def configuration = Marshaller.keystoneV2ConfigFromString(
        s"""<?xml version="1.0" encoding="UTF-8"?>
          |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
          |    <identity-service
          |            username="username"
          |            password="password"
          |            uri="https://some.identity.com"
          |            set-groups-in-header="false"
          |            />
          |    <tenant-handling>
          |        <validate-tenant>
          |            <header-extraction-name>$expectedTenantHeaderName</header-extraction-name>
          |        </validate-tenant>
          |    </tenant-handling>
          |</keystone-v2>
        """.stripMargin)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponseTenantedRoles())
          .build()
      ))

      val filter = new KeystoneV2Filter(mockConfigurationService, mockHttpClientService, mock[AtomFeedService], mockDatastoreService)

      filter.init(mockFilterConfig)
      filter.configurationUpdated(configuration)
      filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

      val request = new MockHttpServletRequest()
      request.setServerName("www.sample.com")
      request.addHeader(expectedTenantHeaderName, "678")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      val response = new MockHttpServletResponse()
      val chain = new MockFilterChain()

      filter.doFilter(request, response, chain)

      response.getStatus shouldBe SC_UNAUTHORIZED
      response.getErrorMessage shouldBe "A tenant from the configured header does not match any of the user's tenants"
    }

    it("will extract the tenant from a header and reject if the user only use the tenant on a configured ignored role") {
      def configuration = Marshaller.keystoneV2ConfigFromString(
        s"""<?xml version="1.0" encoding="UTF-8"?>
          |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0" ignored-roles="role:234">
          |    <identity-service
          |            username="username"
          |            password="password"
          |            uri="https://some.identity.com"
          |            set-groups-in-header="false"
          |            />
          |    <tenant-handling>
          |        <validate-tenant>
          |            <header-extraction-name>$expectedTenantHeaderName</header-extraction-name>
          |        </validate-tenant>
          |    </tenant-handling>
          |</keystone-v2>
        """.stripMargin)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponseTenantedRoles())
          .build()
      ))

      val filter = new KeystoneV2Filter(mockConfigurationService, mockHttpClientService, mock[AtomFeedService], mockDatastoreService)

      filter.init(mockFilterConfig)
      filter.configurationUpdated(configuration)
      filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

      val request = new MockHttpServletRequest()
      request.setServerName("www.sample.com")
      request.addHeader(expectedTenantHeaderName, "234")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      val response = new MockHttpServletResponse()
      val chain = new MockFilterChain()

      filter.doFilter(request, response, chain)

      response.getStatus shouldBe SC_UNAUTHORIZED
      response.getErrorMessage shouldBe "A tenant from the configured header does not match any of the user's tenants"
    }

    it("sends all tenant IDs when configured to") {
      val modifiedConfig = configuration
      modifiedConfig.getTenantHandling.setSendTenantIdQuality(null)
      filter.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest()
      request.setServerName("www.sample.com")
      request.addHeader(expectedTenantHeaderName, "tenant")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(createValidToken(defaultTenantId = Some("tenant"), roles = Seq(Role("role1", Some("rick")), Role("role2", Some("morty"))), tenantIds = Seq("rick", "morty")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.configurationUpdated(configuration)

      val processedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequest]
      val tenantToRolesMap = Json.parse(Base64Helper.base64DecodeUtf8(processedRequest.getHeader(OpenStackServiceHeader.TENANT_ROLES_MAP))).as[TenantToRolesMap]
      processedRequest.getHeader(OpenStackServiceHeader.TENANT_ID) should include("tenant")
      processedRequest.getHeader(OpenStackServiceHeader.TENANT_ID) should include("rick")
      processedRequest.getHeader(OpenStackServiceHeader.TENANT_ID) should include("morty")
      tenantToRolesMap.keySet should contain only ("tenant", "rick", "morty")
    }

    it("sends all tenant IDs with a quality when all three are configured") {
      val request = new MockHttpServletRequest()
      request.setServerName("www.sample.com")
      request.addHeader(expectedTenantHeaderName, "rick")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(createValidToken(defaultTenantId = Some("tenant"), roles = Seq(Role("role1", Some("rick")), Role("role2", Some("morty"))), tenantIds = Seq("rick", "morty")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      val processedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequest]
      processedRequest.getHeader(OpenStackServiceHeader.TENANT_ID) should include("tenant;q=0.9")
      processedRequest.getHeader(OpenStackServiceHeader.TENANT_ID) should include("rick;q=0.7")
      processedRequest.getHeader(OpenStackServiceHeader.TENANT_ID) should include("morty;q=0.5")
    }

    it("sends tenant quality when not configured to send all tenant IDs") {
      val modifiedConfig = configuration
      modifiedConfig.getTenantHandling.setSendAllTenantIds(false)
      filter.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest()
      request.setServerName("www.sample.com")
      request.addHeader(expectedTenantHeaderName, "rick")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(createValidToken(roles = Seq(Role("role1", Some("rick")), Role("role2", Some("morty"))), tenantIds = Seq("rick", "morty")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.configurationUpdated(configuration)

      val processedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequest]
      processedRequest.getHeaders(OpenStackServiceHeader.TENANT_ID).asScala.size shouldBe 1
      processedRequest.getHeader(OpenStackServiceHeader.TENANT_ID) shouldBe "rick;q=0.7"
    }

    it("bypasses the header tenant validation check when a user has a role in the pre-authorized-roles list") {
      val request = new MockHttpServletRequest()
      request.setServerName("www.sample.com")
      request.addHeader(expectedTenantHeaderName, "tenant")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(createValidToken(defaultTenantId = Some("not-tenant"), roles = Seq(Role("racker")), tenantIds = Seq("racker")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest shouldNot be(null)
      filterChain.getResponse shouldNot be(null)
    }

    it("sends the tenant matching the header when send all tenants is false and validate-tenant is enabled") {
      val modifiedConfig = configuration
      modifiedConfig.getTenantHandling.setSendAllTenantIds(false)
      modifiedConfig.getTenantHandling.setSendTenantIdQuality(null)
      filter.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest()
      request.setServerName("www.sample.com")
      request.addHeader(expectedTenantHeaderName, "morty")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(createValidToken(defaultTenantId = Some("tenant"), roles = Seq(Role("role1", Some("rick")), Role("role2", Some("morty"))), tenantIds = Seq("rick", "morty")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.configurationUpdated(configuration)

      val processedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequest]
      val tenantToRolesMap = Json.parse(Base64Helper.base64DecodeUtf8(processedRequest.getHeader(OpenStackServiceHeader.TENANT_ROLES_MAP))).as[TenantToRolesMap]
      processedRequest.getHeaders(OpenStackServiceHeader.TENANT_ID).asScala.size shouldBe 1
      processedRequest.getHeader(OpenStackServiceHeader.TENANT_ID) shouldBe "morty"
      tenantToRolesMap.keySet should contain only "morty"
    }

    it("sends all matching tenants when send all tenants is false") {
      val modifiedConfig = configuration
      modifiedConfig.getTenantHandling.setSendAllTenantIds(false)
      modifiedConfig.getTenantHandling.setSendTenantIdQuality(null)
      modifiedConfig.getTenantHandling.getValidateTenant.getHeaderExtractionName.add("Tenant-Out")
      filter.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest()
      request.setServerName("www.sample.com")
      request.addHeader(expectedTenantHeaderName, "morty")
      request.addHeader("Tenant-Out", "rick")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(createValidToken(defaultTenantId = Some("tenant"), roles = Seq(Role("role1", Some("rick")), Role("role2", Some("morty"))), tenantIds = Seq("rick", "morty")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.configurationUpdated(configuration)

      val processedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequest]
      processedRequest.getHeaders(OpenStackServiceHeader.TENANT_ID).asScala.size shouldBe 1
      processedRequest.getHeader(OpenStackServiceHeader.TENANT_ID).split(",") should contain only ("rick", "morty")
    }

    it("sends the user's default tenant, if validate-tenant is not enabled") {
      val modifiedConfig = configuration
      modifiedConfig.getTenantHandling.setValidateTenant(null)
      modifiedConfig.getTenantHandling.setSendAllTenantIds(false)
      modifiedConfig.getTenantHandling.setSendTenantIdQuality(null)
      filter.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest()
      request.setServerName("www.sample.com")
      request.addHeader(expectedTenantHeaderName, "years")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(createValidToken(defaultTenantId = Some("one"), roles = Seq(Role("role1", Some("hundred")), Role("role2", Some("years"))), tenantIds = Seq("hundred", "years")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.configurationUpdated(configuration)

      val processedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequest]
      processedRequest.getHeaders(OpenStackServiceHeader.TENANT_ID).asScala.size shouldBe 1
      processedRequest.getHeader(OpenStackServiceHeader.TENANT_ID) shouldBe "one"
    }

    it("should send the X-Authorization header with the tenant in the header") {
      val request = new MockHttpServletRequest()
      request.setServerName("www.sample.com")
      request.addHeader(expectedTenantHeaderName, "years")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(createValidToken(roles = Seq(Role("role1", Some("hundred")), Role("role2", Some("years"))), tenantIds = Seq("hundred", "years")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      val processedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequest]
      processedRequest.getHeader(OpenStackServiceHeader.EXTENDED_AUTHORIZATION) shouldBe "Proxy years"
    }

    it("should send the X-Authorization header with the default user tenant if no matching tenants exist") {
      val modifiedConfig = configuration
      modifiedConfig.getTenantHandling.setValidateTenant(null)
      filter.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest()
      request.setServerName("www.sample.com")
      request.addHeader(expectedTenantHeaderName, "foo")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(createValidToken(tenantIds = Seq("hundred", "years"), defaultTenantId = Some("defaultTenant")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.configurationUpdated(configuration)

      val processedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequest]
      processedRequest.getHeader(OpenStackServiceHeader.EXTENDED_AUTHORIZATION) shouldBe "Proxy defaultTenant"
    }

    it("should send the X-Authorization header with multiple matching tenants") {
      val modifiedConfig = configuration
      modifiedConfig.getTenantHandling.getValidateTenant.getHeaderExtractionName.add(OpenStackServiceHeader.TENANT_ID)
      filter.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest()
      request.setServerName("www.sample.com")
      request.addHeader(expectedTenantHeaderName, "expectedHeaderTenant")
      request.addHeader(OpenStackServiceHeader.TENANT_ID, "headerTenant")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(createValidToken(roles = Seq(Role("role1", Some("expectedHeaderTenant")), Role("role2", Some("headerTenant")), Role("role3", Some("nonMatchingTenant"))), tenantIds = Seq("expectedHeaderTenant", "headerTenant", "nonMatchingTenant")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.configurationUpdated(configuration)

      val processedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequest]
      processedRequest.getHeaders(OpenStackServiceHeader.EXTENDED_AUTHORIZATION).asScala.toSeq should contain only("Proxy expectedHeaderTenant", "Proxy headerTenant")
    }

    it("should send the X-Authorization header without a tenant if tenant handling is not used and no default tenant exists") {
      val modifiedConfig = configuration
      modifiedConfig.setTenantHandling(null)
      filter.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest()
      request.setServerName("www.sample.com")
      request.addHeader(expectedTenantHeaderName, "years")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(createValidToken(tenantIds = Seq("hundred", "years")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.configurationUpdated(configuration)

      val processedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequest]
      processedRequest.getHeader(OpenStackServiceHeader.EXTENDED_AUTHORIZATION) shouldBe "Proxy"
    }

    it("should send the X-Authorization header without a tenant if tenant validation is not used and no default tenant exists") {
      val modifiedConfig = configuration
      modifiedConfig.getTenantHandling.setValidateTenant(null)
      filter.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest()
      request.setServerName("www.sample.com")
      request.addHeader(expectedTenantHeaderName, "years")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(createValidToken(tenantIds = Seq("hundred", "years")), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.configurationUpdated(configuration)

      val processedRequest = filterChain.getRequest.asInstanceOf[HttpServletRequest]
      processedRequest.getHeader(OpenStackServiceHeader.EXTENDED_AUTHORIZATION) shouldBe "Proxy"
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

    val filter = new KeystoneV2Filter(mockConfigurationService, mockHttpClientService, mock[AtomFeedService], mockDatastoreService)

    filter.init(mockFilterConfig)
    filter.configurationUpdated(configuration)
    filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

    it("forwards the user information in the x-pp-user, x-user-name, and x-user-id headers") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponse())
          .build()
      ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$GROUPS_KEY_PREFIX$VALID_USER_ID")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(groupsResponse())
          .build()
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(PowerApiHeader.USER) shouldBe "testuser"
      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.USER_NAME) shouldBe "testuser"
      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.USER_ID) shouldBe "123"
    }

    it("forwards the user's roles information in the x-roles header") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponse())
          .build()
      ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$GROUPS_KEY_PREFIX$VALID_USER_ID")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(groupsResponse())
          .build()
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.ROLES) should include("compute:admin")
      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.ROLES) should include("object-store:admin")
    }

    it("forwards the user's tenant-to-roles mapping in the x-map-roles header") {
      val defaultTenantId = "defTenant"
      val tenantToRoleMap = Map(
        Some("tenant1") -> Seq("role1", "role2"),
        Some("tenant3") -> Seq("role3"),
        None -> Seq("role4", "role5")
      )
      val token = createValidToken(
        roles = tenantToRoleMap.flatMap({ case (tenantId, roleNames) => roleNames.map(role => Role(role, tenantId)) }).toSeq,
        defaultTenantId = Some(defaultTenantId))
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)
      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(token, Nil: _*)
      when(mockDatastore.get(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")).thenReturn(Vector("group"), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      val tenantRolesMapHeader = filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.TENANT_ROLES_MAP)
      val headerMap = Json.parse(Base64Helper.base64DecodeUtf8(tenantRolesMapHeader))

      headerMap shouldEqual Json.toJson(Map(defaultTenantId -> Seq.empty[String]) ++ tenantToRoleMap.map({ case (tenantId, roleNames) => tenantId.getOrElse(DomainRoleTenantKey) -> roleNames }))
    }

    it("forwards the user's contact id information in the x-contact-id header") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponse())
          .build()
      ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$GROUPS_KEY_PREFIX$VALID_USER_ID")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(groupsResponse())
          .build()
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.CONTACT_ID) shouldBe "abc123"
    }

    it("forwards the user's impersonator information in the x-impersonator-id, x-impersonator-name, and x-impersonator roles headers") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateImpersonatedTokenResponse())
          .build()
      ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$GROUPS_KEY_PREFIX$VALID_USER_ID")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(groupsResponse())
          .build()
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.IMPERSONATOR_ID) shouldBe "567"
      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.IMPERSONATOR_NAME) shouldBe "rick"
      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.IMPERSONATOR_ROLES) should include("Racker")
      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.IMPERSONATOR_ROLES) should include("object-store:admin")
    }

    it("forwards the user's domain ID information in the x-domain-id header") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponse())
          .build()
      ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$GROUPS_KEY_PREFIX$VALID_USER_ID")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(groupsResponse())
          .build()
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.DOMAIN_ID) shouldBe "909989"
    }

    it("forwards the user's default region information in the x-default-region header") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponse())
          .build()
      ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$GROUPS_KEY_PREFIX$VALID_USER_ID")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(groupsResponse())
          .build()
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.DEFAULT_REGION) shouldBe "DFW"
    }

    it("forwards the expiration date information in the x-expiration header") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponse(expires = dateTime))
          .build()
      ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$GROUPS_KEY_PREFIX$VALID_USER_ID")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(groupsResponse())
          .build()
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)


      filterChain.getRequest.asInstanceOf[HttpServletRequest]
        .getHeader(OpenStackServiceHeader.X_EXPIRATION) shouldBe iso8601ToRfc1123(tokenDateFormat(dateTime))
    }

    it("forwards the groups in the x-pp-groups header by default") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponse())
          .build()
      ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$GROUPS_KEY_PREFIX$VALID_USER_ID")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(groupsResponse())
          .build()
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(PowerApiHeader.GROUPS) should include("test-group-id")
    }

    it("should not add the groups in the x-pp-groups header when RAX-KSGRP:groups not defined") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponse(expires = dateTime))
          .build()
      ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$GROUPS_KEY_PREFIX$VALID_USER_ID")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponse())
          .build()
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(PowerApiHeader.GROUPS) shouldBe null
    }

    it("should not add the roles in the x-roles header when isSetRolesInHeader is false") {
      val modifiedConfig = configuration
      modifiedConfig.getIdentityService.setSetRolesInHeader(false)
      filter.configurationUpdated(modifiedConfig)
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponse())
          .build()
      ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$GROUPS_KEY_PREFIX$VALID_USER_ID")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponse())
          .build()
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.ROLES) shouldBe null
    }

    it("forwards the user's catalog in x-catalog header base64 JSON encoded by default") {
      val modifiedConfig = configuration
      modifiedConfig.getIdentityService.setSetCatalogInHeader(true)
      modifiedConfig.getIdentityService.setSetGroupsInHeader(false)
      modifiedConfig.setRequireServiceEndpoint(new ServiceEndpointType().withPublicUrl("example.com"))
      filter.configurationUpdated(modifiedConfig)

      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)
      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(createValidToken(), Nil: _*)
      when(mockDatastore.get(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")).thenReturn(EndpointsData(endpointsResponse(), Vector(Endpoint(None, None, None, "example.com"))), Nil: _*)

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.configurationUpdated(configuration)

      val encodedEndpoints = Base64Helper.base64EncodeUtf8(endpointsResponse())
      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(PowerApiHeader.X_CATALOG) shouldBe encodedEndpoints
    }

    it("forwards the data store key for the parsed user token") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponse())
          .build()
      ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$GROUPS_KEY_PREFIX$VALID_USER_ID")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponse())
          .build()
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(KeystoneV2Filter.AuthTokenKey) shouldBe s"$TOKEN_KEY_PREFIX$VALID_TOKEN"
    }
  }

  describe("Handling tenanted roles") {
    val expectedTenantHeaderName = "X-Expected-Tenant"

    it("should forward all roles that are not ignored if not in tenanted mode") {
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

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponseTenantedRoles())
          .build()
      ))

      val filter = new KeystoneV2Filter(mockConfigurationService, mockHttpClientService, mock[AtomFeedService], mockDatastoreService)

      filter.init(mockFilterConfig)
      filter.configurationUpdated(configuration)
      filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

      val request = new MockHttpServletRequest()
      request.setServerName("www.sample.com")
      request.setRequestURI("/567/foo")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      val response = new MockHttpServletResponse()
      val chain = new MockFilterChain()

      filter.doFilter(request, response, chain)

      val postFilterRequest = chain.getRequest.asInstanceOf[HttpServletRequest]
      postFilterRequest.getHeader(OpenStackServiceHeader.ROLES) should include("role:123")
      postFilterRequest.getHeader(OpenStackServiceHeader.ROLES) should include("role:234")
      postFilterRequest.getHeader(OpenStackServiceHeader.ROLES) should include("role:345")
      postFilterRequest.getHeader(OpenStackServiceHeader.ROLES) should not include "identity:tenant-access"
    }

    it("should forward all roles that are not ignored in tenanted mode if legacy mode is enabled") {
      def configuration = Marshaller.keystoneV2ConfigFromString(
        s"""<?xml version="1.0" encoding="UTF-8"?>
          |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
          |    <identity-service
          |            username="username"
          |            password="password"
          |            uri="https://some.identity.com"
          |            set-groups-in-header="false"
          |            />
          |    <tenant-handling>
          |        <validate-tenant enable-legacy-roles-mode="true">
          |            <header-extraction-name>$expectedTenantHeaderName</header-extraction-name>
          |        </validate-tenant>
          |    </tenant-handling>
          |</keystone-v2>
        """.stripMargin)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponseTenantedRoles())
          .build()
      ))

      val filter = new KeystoneV2Filter(mockConfigurationService, mockHttpClientService, mock[AtomFeedService], mockDatastoreService)

      filter.init(mockFilterConfig)
      filter.configurationUpdated(configuration)
      filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

      val request = new MockHttpServletRequest()
      request.setServerName("www.sample.com")
      request.addHeader(expectedTenantHeaderName, "456")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      val response = new MockHttpServletResponse()
      val chain = new MockFilterChain()

      filter.doFilter(request, response, chain)

      val postFilterRequest = chain.getRequest.asInstanceOf[HttpServletRequest]
      postFilterRequest.getHeader(OpenStackServiceHeader.ROLES) should include("role:123")
      postFilterRequest.getHeader(OpenStackServiceHeader.ROLES) should include("role:234")
      postFilterRequest.getHeader(OpenStackServiceHeader.ROLES) should include("role:345")
      postFilterRequest.getHeader(OpenStackServiceHeader.ROLES) should not include "identity:tenant-access"
    }

    it("should only forward matching tenant roles (excluding ignored roles) if legacy mode is disabled") {
      def configuration = Marshaller.keystoneV2ConfigFromString(
        s"""<?xml version="1.0" encoding="UTF-8"?>
          |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
          |    <identity-service
          |            username="username"
          |            password="password"
          |            uri="https://some.identity.com"
          |            set-groups-in-header="false"
          |            />
          |    <tenant-handling>
          |        <validate-tenant enable-legacy-roles-mode="false">
          |            <header-extraction-name>$expectedTenantHeaderName</header-extraction-name>
          |        </validate-tenant>
          |    </tenant-handling>
          |</keystone-v2>
        """.stripMargin)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponseTenantedRoles())
          .build()
      ))

      val filter = new KeystoneV2Filter(mockConfigurationService, mockHttpClientService, mock[AtomFeedService], mockDatastoreService)

      filter.init(mockFilterConfig)
      filter.configurationUpdated(configuration)
      filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

      val request = new MockHttpServletRequest()
      request.setServerName("www.sample.com")
      request.addHeader(expectedTenantHeaderName, "345")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      val response = new MockHttpServletResponse()
      val chain = new MockFilterChain()

      filter.doFilter(request, response, chain)

      val postFilterRequest = chain.getRequest.asInstanceOf[HttpServletRequest]
      postFilterRequest.getHeader(OpenStackServiceHeader.ROLES) should include("role:123")
      postFilterRequest.getHeader(OpenStackServiceHeader.ROLES) should include("role:345")
      postFilterRequest.getHeader(OpenStackServiceHeader.ROLES) should not include "identity:tenant-access"
    }

    it("should forward all roles if legacy mode is disabled, but the user is pre-authorized") {
      def configuration = Marshaller.keystoneV2ConfigFromString(
        s"""<?xml version="1.0" encoding="UTF-8"?>
          |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
          |    <identity-service
          |            username="username"
          |            password="password"
          |            uri="https://some.identity.com"
          |            set-groups-in-header="false"
          |            />
          |    <tenant-handling>
          |        <validate-tenant enable-legacy-roles-mode="false">
          |            <header-extraction-name>$expectedTenantHeaderName</header-extraction-name>
          |        </validate-tenant>
          |    </tenant-handling>
          |    <pre-authorized-roles>
          |        <role>role:345</role>
          |    </pre-authorized-roles>
          |</keystone-v2>
        """.stripMargin)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponseTenantedRoles())
          .build()
      ))

      val filter = new KeystoneV2Filter(mockConfigurationService, mockHttpClientService, mock[AtomFeedService], mockDatastoreService)

      filter.init(mockFilterConfig)
      filter.configurationUpdated(configuration)
      filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

      val request = new MockHttpServletRequest()
      request.setServerName("www.sample.com")
      request.addHeader(expectedTenantHeaderName, "345")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      val response = new MockHttpServletResponse()
      val chain = new MockFilterChain()

      filter.doFilter(request, response, chain)

      val postFilterRequest = chain.getRequest.asInstanceOf[HttpServletRequest]
      postFilterRequest.getHeader(OpenStackServiceHeader.ROLES) should include("role:123")
      postFilterRequest.getHeader(OpenStackServiceHeader.ROLES) should include("role:234")
      postFilterRequest.getHeader(OpenStackServiceHeader.ROLES) should include("role:345")
    }

    it("should only forward matching tenant roles if legacy mode is disabled, and the user is not pre-authorized") {
      def configuration = Marshaller.keystoneV2ConfigFromString(
        s"""<?xml version="1.0" encoding="UTF-8"?>
          |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
          |    <identity-service
          |            username="username"
          |            password="password"
          |            uri="https://some.identity.com"
          |            set-groups-in-header="false"
          |            />
          |    <tenant-handling>
          |        <validate-tenant enable-legacy-roles-mode="false">
          |            <header-extraction-name>$expectedTenantHeaderName</header-extraction-name>
          |        </validate-tenant>
          |    </tenant-handling>
          |    <pre-authorized-roles>
          |        <role>role:234</role>
          |    </pre-authorized-roles>
          |</keystone-v2>
        """.stripMargin)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponseTenantedRoles())
          .build()
      ))

      val filter = new KeystoneV2Filter(mockConfigurationService, mockHttpClientService, mock[AtomFeedService], mockDatastoreService)

      filter.init(mockFilterConfig)
      filter.configurationUpdated(configuration)
      filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

      val request = new MockHttpServletRequest()
      request.setServerName("www.sample.com")
      request.addHeader(expectedTenantHeaderName, "345")
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      val response = new MockHttpServletResponse()
      val chain = new MockFilterChain()

      filter.doFilter(request, response, chain)

      val postFilterRequest = chain.getRequest.asInstanceOf[HttpServletRequest]
      postFilterRequest.getHeader(OpenStackServiceHeader.ROLES) should include("role:123")
      postFilterRequest.getHeader(OpenStackServiceHeader.ROLES) should include("role:345")
    }
  }

  describe("Handling multiple tenant extraction") {
    def configuration = Marshaller.keystoneV2ConfigFromString(
      """<?xml version="1.0" encoding="UTF-8"?>
        |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
        |    <identity-service
        |        uri="https://some.identity.com"
        |        set-groups-in-header="false"
        |    />
        |    <tenant-handling>
        |        <validate-tenant>
        |            <header-extraction-name>X-Expected-Tenant-One</header-extraction-name>
        |            <header-extraction-name>X-Expected-Tenant-Two</header-extraction-name>
        |            <header-extraction-name>X-Expected-Tenant-Too</header-extraction-name>
        |        </validate-tenant>
        |    </tenant-handling>
        |</keystone-v2>
      """.stripMargin)

    val filter = new KeystoneV2Filter(mockConfigurationService, mockHttpClientService, mock[AtomFeedService], mockDatastoreService)

    filter.init(mockFilterConfig)
    filter.configurationUpdated(configuration)
    filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

    Seq("One", "Two", "Too") foreach { header =>
      it(s"will authenticate and authorize if able to extract a tenant from the header: X-Expected-Tenant-$header") {
        //make a request and validate that it called the akka service client?
        val request = new MockHttpServletRequest
        request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)
        request.addHeader(s"X-Expected-Tenant-$header", "345")

        when(mockHttpClient.execute(
          argThat(HC.allOf[HttpUriRequest](
            hasMethod(HttpGet.METHOD_NAME),
            hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo(VALID_TOKEN))
          )),
          argThat(hasAttribute(
            CachingHttpClientContext.CACHE_KEY,
            HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
          ))
        )).thenReturn(makeResponse(
          SC_OK,
          EntityBuilder.create()
            .setText(validateTokenResponseTenantedRoles())
            .build()
        ))

        val response = new MockHttpServletResponse
        val filterChain = new MockFilterChain()
        filter.doFilter(request, response, filterChain)

        val postFilterRequest = filterChain.getRequest.asInstanceOf[HttpServletRequest]
        postFilterRequest.getHeader(OpenStackServiceHeader.ROLES) should include("role:123")
        postFilterRequest.getHeader(OpenStackServiceHeader.ROLES) should include("role:345")
      }
    }

    it(s"will neither authenticate nor authorize if not able to extract a tenant from the header: X-Expected-Tenant-Bad") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)
      request.addHeader("X-Expected-Tenant-Bad", "345")

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo(VALID_TOKEN))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponseTenantedRoles())
          .build()
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getStatus shouldBe SC_UNAUTHORIZED
      response.getErrorMessage shouldBe "Could not parse tenant from the configured header"
      response.getHeaders(WWW_AUTHENTICATE) should contain("Keystone uri=https://some.identity.com")
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

    val filter = new KeystoneV2Filter(mockConfigurationService, mockHttpClientService, mock[AtomFeedService], mockDatastoreService)

    filter.init(mockFilterConfig)
    filter.configurationUpdated(configuration)
    filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

    it("should forward the x-trans-d header if enabled") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)
      request.addHeader(CommonHttpHeader.TRACE_GUID, "test-guid")

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn(null, "glibglob")

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(endpointsResponse())
          .build()
      ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$GROUPS_KEY_PREFIX$VALID_USER_ID")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(groupsResponse())
          .build()
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpUriRequest])
      verify(mockHttpClient).execute(
        requestCaptor.capture(),
        any[HttpContext]
      )

      requestCaptor.getAllValues.asScala.forall(_.getHeaders(CommonHttpHeader.TRACE_GUID).map(_.getValue).contains("test-guid")) shouldBe true
    }

    it("should not forward the x-trans-id header if disabled") {
      val mockSystemModelNoTracing = mock[SystemModel]
      val mockTracingHeaderConfig = mock[TracingHeaderConfig]
      when(mockSystemModelNoTracing.getTracingHeader).thenReturn(mockTracingHeaderConfig)
      when(mockTracingHeaderConfig.isEnabled).thenReturn(false)
      filter.SystemModelConfigListener.configurationUpdated(mockSystemModelNoTracing)

      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)
      request.addHeader(CommonHttpHeader.TRACE_GUID, "test-guid")

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn(null, "glibglob")

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(endpointsResponse())
          .build()
      ))
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$GROUPS_KEY_PREFIX$VALID_USER_ID")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(groupsResponse())
          .build()
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)
      filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

      val requestCaptor = ArgumentCaptor.forClass(classOf[HttpUriRequest])
      verify(mockHttpClient).execute(
        requestCaptor.capture(),
        any[HttpContext]
      )

      requestCaptor.getAllValues.asScala.exists(_.getHeaders(CommonHttpHeader.TRACE_GUID).map(_.getValue).contains("test-guid")) shouldBe false
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

    val filter = new KeystoneV2Filter(mockConfigurationService, mockHttpClientService, mock[AtomFeedService], mockDatastoreService)

    filter.init(mockFilterConfig)
    filter.configurationUpdated(configuration)
    filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

    it("should validate a token without using an admin token") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo(VALID_TOKEN))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_OK,
        EntityBuilder.create()
          .setText(validateTokenResponse())
          .build()
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getRequest shouldNot be(null)
      filterChain.getResponse shouldNot be(null)
    }

    it("rejects with 401 if we receive unauthorized from Identity (401)") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo(VALID_TOKEN))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_UNAUTHORIZED,
        EntityBuilder.create()
          .setText("Unauthorized from Identity!")
          .build()
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getStatus shouldBe SC_UNAUTHORIZED
      response.getHeader(WWW_AUTHENTICATE) shouldBe "Keystone uri=https://some.identity.com"

      filterChain.getRequest shouldBe null
      filterChain.getResponse shouldBe null
    }

    it("rejects with 413 if we are rate limited by identity (413)") {
      val retryValue = DateUtils.formatDate(new DateTime().plusHours(1).toDate)

      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo(VALID_TOKEN))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_REQUEST_ENTITY_TOO_LARGE,
        EntityBuilder.create()
          .setText("Rate limited by identity!")
          .build(),
        Array[Header](RETRY_AFTER -> retryValue)
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getStatus shouldBe SC_REQUEST_ENTITY_TOO_LARGE
      response.getHeader(RETRY_AFTER) shouldBe retryValue

      filterChain.getRequest shouldBe null
      filterChain.getResponse shouldBe null
    }

    it("rejects with 429 if we are rate limited by identity (429)") {
      val retryValue = DateUtils.formatDate(new DateTime().plusHours(1).toDate)

      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo(VALID_TOKEN))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(makeResponse(
        SC_TOO_MANY_REQUESTS,
        EntityBuilder.create()
          .setText("Rate limited by identity!")
          .build(),
        Array[Header](RETRY_AFTER -> retryValue)
      ))

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getStatus shouldBe SC_TOO_MANY_REQUESTS
      response.getHeader(RETRY_AFTER) shouldBe retryValue

      filterChain.getRequest shouldBe null
      filterChain.getResponse shouldBe null
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

    val filter = new KeystoneV2Filter(mockConfigurationService, mockHttpClientService, mock[AtomFeedService], mockDatastoreService)

    filter.init(mockFilterConfig)
    filter.configurationUpdated(configuration)
    filter.SystemModelConfigListener.configurationUpdated(mockSystemModel)

    it("does not use the retry cache for token validation") {
      val filterChain = new MockFilterChain()
      val response = new MockHttpServletResponse
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(
        makeResponse(SC_UNAUTHORIZED),
        makeResponse(
          SC_OK,
          EntityBuilder.create()
            .setText(validateTokenResponse())
            .build()
        )
      )
      when(mockDatastore.get(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")).thenReturn(EndpointsData("", Vector.empty[Endpoint]), Nil: _*)
      when(mockDatastore.get(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")).thenReturn(Vector("group"), Nil: _*)

      filter.doFilter(request, response, filterChain)

      val contextCaptor = ArgumentCaptor.forClass(classOf[CachingHttpClientContext])
      verify(mockHttpClient, times(2)).execute(
        any[HttpUriRequest],
        contextCaptor.capture()
      )

      contextCaptor.getAllValues.asScala(0).getUseCache shouldBe true
      contextCaptor.getAllValues.asScala(1).getUseCache shouldBe false
    }

    it("does not use the retry cache for endpoint retrieval") {
      val filterChain = new MockFilterChain()
      val response = new MockHttpServletResponse
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)
      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(createValidToken(), Nil: _*)
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")
        ))
      )).thenReturn(
        makeResponse(SC_UNAUTHORIZED),
        makeResponse(
          SC_OK,
          EntityBuilder.create()
            .setText(oneEndpointResponse())
            .build()
        )
      )
      when(mockDatastore.get(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")).thenReturn(Vector("group"), Nil: _*)

      filter.doFilter(request, response, filterChain)

      val contextCaptor = ArgumentCaptor.forClass(classOf[CachingHttpClientContext])
      verify(mockHttpClient, times(2)).execute(
        any[HttpUriRequest],
        contextCaptor.capture()
      )

      contextCaptor.getAllValues.asScala(0).getUseCache shouldBe true
      contextCaptor.getAllValues.asScala(1).getUseCache shouldBe false
    }

    it("does not use the retry cache for groups retrieval") {
      val filterChain = new MockFilterChain()
      val response = new MockHttpServletResponse
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN, VALID_TOKEN)

      when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)
      when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(createValidToken(userId = "userId"), Nil: _*)
      when(mockDatastore.get(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")).thenReturn(EndpointsData("", Vector.empty[Endpoint]), Nil: _*)
      when(mockHttpClient.execute(
        argThat(HC.allOf[HttpUriRequest](
          hasMethod(HttpGet.METHOD_NAME),
          hasHeader(CommonHttpHeader.AUTH_TOKEN, HC.equalTo("glibglob"))
        )),
        argThat(hasAttribute(
          CachingHttpClientContext.CACHE_KEY,
          HC.equalTo(s"${GROUPS_KEY_PREFIX}userId")
        ))
      )).thenReturn(
        makeResponse(SC_UNAUTHORIZED),
        makeResponse(
          SC_OK,
          EntityBuilder.create()
            .setText(groupsResponse())
            .build()
        )
      )

      filter.doFilter(request, response, filterChain)

      val contextCaptor = ArgumentCaptor.forClass(classOf[CachingHttpClientContext])
      verify(mockHttpClient, times(2)).execute(
        any[HttpUriRequest],
        contextCaptor.capture()
      )

      contextCaptor.getAllValues.asScala(0).getUseCache shouldBe true
      contextCaptor.getAllValues.asScala(1).getUseCache shouldBe false
    }
  }

  def makeResponse(statusCode: Int, entity: HttpEntity = null, headers: Array[Header] = Array.empty): CloseableHttpResponse = {
    val response = new BasicHttpResponse(HttpVersion.HTTP_1_1, statusCode, null) with CloseableHttpResponse {
      override def close(): Unit = {}
    }
    Option(entity).foreach(response.setEntity)
    headers.foreach(response.addHeader)
    response
  }

  implicit def stringToInputStream(s: String): InputStream = new ByteArrayInputStream(s.getBytes)

  implicit def looseToStrictStringMap(sm: java.util.Map[_ <: String, _ <: String]): java.util.Map[String, String] =
    sm.asInstanceOf[java.util.Map[String, String]]

  implicit def tupleToHeader(t: (String, String)): Header = new BasicHeader(t._1, t._2)
}
