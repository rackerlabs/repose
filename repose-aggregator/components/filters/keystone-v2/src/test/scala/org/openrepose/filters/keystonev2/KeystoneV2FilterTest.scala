package org.openrepose.filters.keystonev2

import java.net.URL
import java.util.concurrent.TimeUnit
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.mockrunner.mock.web.{MockFilterChain, MockFilterConfig, MockHttpServletRequest, MockHttpServletResponse}
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.mockito.{Matchers => MockMatchers, Mockito}
import org.openrepose.commons.utils.http.{CommonHttpHeader, OpenStackServiceHeader, PowerApiHeader}
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.{Datastore, DatastoreService}
import org.openrepose.filters.keystonev2.RequestHandler._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class KeystoneV2FilterTest extends FunSpec
with Matchers
with BeforeAndAfter
with MockitoSugar
with IdentityResponses
with MockedAkkaServiceClient {
  System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
    "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl")

  val mockDatastoreService = mock[DatastoreService]
  private val mockDatastore: Datastore = mock[Datastore]
  Mockito.when(mockDatastoreService.getDefaultDatastore).thenReturn(mockDatastore)
  val mockConfigService = mock[ConfigurationService]

  before {
    Mockito.reset(mockDatastore)
    Mockito.reset(mockConfigService)
    mockAkkaServiceClient.reset()
  }

  describe("Filter lifecycle") {
    val filter: KeystoneV2Filter = new KeystoneV2Filter(mockConfigService, mockAkkaServiceClient, mockDatastoreService)
    val config: MockFilterConfig = new MockFilterConfig

    it("should subscribe a listener to the configuration service on init") {
      filter.init(config)

      Mockito.verify(mockConfigService).subscribeTo(
        MockMatchers.anyString(),
        MockMatchers.anyString(),
        MockMatchers.any[URL],
        MockMatchers.any(),
        MockMatchers.any()
      )
    }

    it("should unsubscribe a listener to the configuration service on destroy") {
      filter.destroy()

      Mockito.verify(mockConfigService).unsubscribeFrom(
        MockMatchers.anyString(),
        MockMatchers.any()
      )
    }
  }

  describe("Configured simply to authenticate tokens, defaults for everything else") {
    //Configure the filter
    val configuration = Marshaller.keystoneV2ConfigFromString(
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

      Mockito.when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn(null, "glibglob")

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

      mockAkkaServiceClient.validate()
    }

    it("caches the admin token request for 10 minutes") {
      //Can only make sure it was put into the cache with a 10 minute timeout...
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      Mockito.when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn(null, "glibglob")

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

      Mockito.verify(mockDatastore).put(ADMIN_TOKEN_KEY, "glibglob", 600, TimeUnit.SECONDS)

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)

      mockAkkaServiceClient.validate()
    }

    it("caches a valid token for 10 minutes") {
      //Can only make sure it was put into the cache with a 10 minute timeout...
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      Mockito.when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn(null, "glibglob")

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

      Mockito.verify(mockDatastore).put(ADMIN_TOKEN_KEY, "glibglob", 600, TimeUnit.SECONDS)
      //Have to cache the result of the stuff
      Mockito.verify(mockDatastore).put(s"$TOKEN_KEY_PREFIX$VALID_TOKEN", ValidToken(s"${tokenDateFormat(DateTime.now().plusDays(1))}", "123", "testuser", "My Project", "345", List.empty[String], Vector("compute:admin", "object-store:admin"), None, None, Some("DFW")), 600, TimeUnit.SECONDS)

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)

      mockAkkaServiceClient.validate()
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

      Mockito.verify(mockDatastore).put(ADMIN_TOKEN_KEY, "glibglob", 600, TimeUnit.SECONDS)
      //Have to cache the result of the stuff
      Mockito.verify(mockDatastore).put(s"${TOKEN_KEY_PREFIX}INVALID_TOKEN", InvalidToken, 600, TimeUnit.SECONDS)

      filterChain.getLastRequest should be(null)
      filterChain.getLastResponse should be(null)

      response.getErrorCode shouldBe HttpServletResponse.SC_UNAUTHORIZED
      mockAkkaServiceClient.validate()
    }

    it("Makes no other calls if the token is already cached with a valid result") {
      //Can only make sure it was put into the cache with a 10 minute timeout...
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //When the user's token details are cached, no calls to identity should take place

      //When we ask the cache for our token, it works
      Mockito.when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(roles = Vector("compute:admin", "object-store:admin")), Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)

      //So because we didn't add any interactions, this guy will validate with no interactions
      mockAkkaServiceClient.validate()
    }

    it("Makes no other calls if the token is already cached with a token not valid result") {
      //Can only make sure it was put into the cache with a 10 minute timeout...
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, "LOLNOPE")

      //When the user's token details are cached, no calls to identity should take place

      //When we ask the cache for our token, it works
      Mockito.when(mockDatastore.get(s"${TOKEN_KEY_PREFIX}LOLNOPE")).thenReturn(InvalidToken, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest should be(null)
      filterChain.getLastResponse should be(null)

      response.getErrorCode shouldBe HttpServletResponse.SC_UNAUTHORIZED

      //So because we didn't add any interactions, this guy will validate with no interactions
      mockAkkaServiceClient.validate()
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
      mockAkkaServiceClient.validate()
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
      mockAkkaServiceClient.validate()
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
      mockAkkaServiceClient.validate()
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
      mockAkkaServiceClient.validate()
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
      mockAkkaServiceClient.validate()

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
      mockAkkaServiceClient.validate()
    }
  }

  describe("Configured to authenticate and authorize a specific service endpoint") {
    val configuration = Marshaller.keystoneV2ConfigFromString(
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
      Mockito.when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

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

      mockAkkaServiceClient.validate()
    }

    it("handles 203 response from endpoints call") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      Mockito.when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      Mockito.when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(), Nil: _*)

      mockAkkaGetResponse(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION, endpointsResponse())
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      //Continues with the chain
      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)

      mockAkkaServiceClient.validate()
    }

    it("handles 403 response from endpoints call") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      Mockito.when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      Mockito.when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
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
      mockAkkaServiceClient.validate()
    }

    it("handles 401 response from endpoints call") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      Mockito.when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      Mockito.when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
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
      mockAkkaServiceClient.validate()
    }

    it("Tests failure case when serviceClientResponse.getStatus fails") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      Mockito.when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      Mockito.when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
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

      mockAkkaServiceClient.validate()
    }

    it("handles unexpected response from endpoints call") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      Mockito.when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      Mockito.when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
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
      mockAkkaServiceClient.validate()
    }

    it("rejects with 403 if the user does not have the required endpoint") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      Mockito.when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      Mockito.when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
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

      mockAkkaServiceClient.validate()
    }

    it("bypasses validation if the user has the role listed in bypass-validation-roles") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      Mockito.when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

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

      mockAkkaServiceClient.validate()
    }

    describe("when endpoints are cached") {
      it("will reject if the user doesn't have the endpoint") {
        //make a request and validate that it called the akka service client?
        val request = new MockHttpServletRequest()
        request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

        //Pretend like the admin token is cached all the time
        Mockito.when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

        mockAkkaGetResponse(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")(
          "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse())
        )

        val endpointsList = Vector(Endpoint(Some("DERP"), Some("Compute"), Some("compute"), "https://compute.north.public.com/v1"))
        Mockito.when(mockDatastore.get(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")).thenReturn(endpointsList, Nil: _*)

        val response = new MockHttpServletResponse
        val filterChain = new MockFilterChain()
        filter.doFilter(request, response, filterChain)

        response.getErrorCode shouldBe HttpServletResponse.SC_FORBIDDEN
        filterChain.getLastRequest should be(null)
        filterChain.getLastResponse should be(null)

        mockAkkaServiceClient.validate()
      }

      it("will allow through if the user has the endpoint") {
        //make a request and validate that it called the akka service client?
        val request = new MockHttpServletRequest()
        request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

        //Pretend like the admin token is cached all the time
        Mockito.when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

        mockAkkaGetResponse(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")(
          "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse())
        )

        val endpointsList = Vector(Endpoint(Some("Global"), Some("Compute"), Some("compute"), "https://compute.north.public.com/v1"))
        Mockito.when(mockDatastore.get(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")).thenReturn(endpointsList, Nil: _*)

        val response = new MockHttpServletResponse
        val filterChain = new MockFilterChain()
        filter.doFilter(request, response, filterChain)

        filterChain.getLastRequest shouldNot be(null)
        filterChain.getLastResponse shouldNot be(null)

        mockAkkaServiceClient.validate()
      }

      it("will allow through if the user matches the bypass roles") {
        //make a request and validate that it called the akka service client?
        val request = new MockHttpServletRequest()
        request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

        //Pretend like the admin token is cached all the time
        Mockito.when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

        mockAkkaGetResponse(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")(
          "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, validateRackerTokenResponse())
        )

        val endpointsList = Vector(Endpoint(Some("DERP"), Some("LOLNOPE"), Some("compute"), "https://compute.north.public.com/v1"))
        Mockito.when(mockDatastore.get(s"$ENDPOINTS_KEY_PREFIX$VALID_TOKEN")).thenReturn(endpointsList, Nil: _*)

        val response = new MockHttpServletResponse
        val filterChain = new MockFilterChain()
        filter.doFilter(request, response, filterChain)

        filterChain.getLastRequest shouldNot be(null)
        filterChain.getLastResponse shouldNot be(null)

        mockAkkaServiceClient.validate()
      }
    }
  }

  describe("when delegating") {
    it("delegates with an invalid token and adds the header") {
      pending
    }

    it("delegates if lacking the required service endpoint and adds the header") {
      pending
    }

    it("delegates if identity doesn't respond properly") {
      pending
    }

    it("delegates if the admin token is invalid") {
      pending
    }
  }

  describe("when whitelist is configured for a particular URI") {
    val configuration = Marshaller.keystoneV2ConfigFromString(
      """<?xml version="1.0" encoding="UTF-8"?>
        |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
        |    <identity-service
        |            username="admin_username"
        |            password="password"
        |            uri="https://some.identity.com"
        |            set-groups-in-header="true"
        |            set-catalog-in-header="false"
        |            />
        |
        |    <white-list>
        |        <uri-regex>.*/application\.wadl$</uri-regex>
        |    </white-list>
        |</keystone-v2>
      """.stripMargin)

    val filter: KeystoneV2Filter = new KeystoneV2Filter(mockConfigService, mockAkkaServiceClient, mockDatastoreService)

    val config: MockFilterConfig = new MockFilterConfig
    filter.init(config)
    filter.configurationUpdated(configuration)

    it("will not perform authentication or authorization the URI that matches") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest
      request.setRequestURL("http://www.sample.com/some/path/application.wadl")
      request.setRequestURI("/some/path/application.wadl")

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)

      mockAkkaServiceClient.validate()
    }

    it("will not perform authentication or authorization for several URIS") {
      pending
    }
  }

  describe("configuring timeouts") {
    it("passes through the values to the distributed datastore for the proper cache timeouts") {
      pending
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
        |            set-groups-in-header="true"
        |            set-catalog-in-header="false"
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

      Mockito.when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(defaultTenantId = "tenant"), Nil: _*)

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

      Mockito.when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(defaultTenantId = "not-tenant"), Nil: _*)

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

      Mockito.when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(defaultTenantId = "tenant", tenantIds = Seq("rick", "morty")), Nil: _*)

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

      Mockito.when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(defaultTenantId = "tenant", tenantIds = Seq("rick", "morty")), Nil: _*)

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

      Mockito.when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(tenantIds = Seq("rick", "morty")), Nil: _*)

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

      Mockito.when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(defaultTenantId = "not-tenant", roles = Seq("racker")), Nil: _*)

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

      Mockito.when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(defaultTenantId = "tenant", tenantIds = Seq("rick", "morty")), Nil: _*)

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

      Mockito.when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(defaultTenantId = "one", tenantIds = Seq("hundred", "years")), Nil: _*)

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

      Mockito.when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(defaultTenantId = "bu-%tts"), Nil: _*)

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

      Mockito.when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(tenantIds = Seq("hundred", "years")), Nil: _*)

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

      Mockito.when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(tenantIds = Seq("hundred", "years")), Nil: _*)

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

      Mockito.when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")).thenReturn(TestValidToken(tenantIds = Seq("hundred", "years")), Nil: _*)

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
    val configuration = Marshaller.keystoneV2ConfigFromString(
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
      Mockito.when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

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

      mockAkkaServiceClient.validate()
    }

    it("forwards the user's roles information in the x-roles header") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      Mockito.when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

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

      mockAkkaServiceClient.validate()
    }

    it("forwards the user's impersonator information in the x-impersonator-id and x-impersonator-name headers") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      Mockito.when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

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

      mockAkkaServiceClient.validate()
    }

    it("forwards the user's default region information in the x-default-region header") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      Mockito.when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

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

      mockAkkaServiceClient.validate()
    }

    it("forwards the expiration date information in the x-expiration header") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      Mockito.when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      mockAkkaGetResponse(s"$TOKEN_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, validateTokenResponse())
      )

      mockAkkaGetResponse(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(HttpServletResponse.SC_OK, groupsResponse())
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(OpenStackServiceHeader.X_EXPIRATION.toString) shouldBe tokenDateFormat(DateTime.now().plusDays(1))

      mockAkkaServiceClient.validate()
    }

    it("forwards the groups in the x-pp-groups header by default") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      Mockito.when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

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

      mockAkkaServiceClient.validate()
    }

    it("Tests failure case in getGroupsForToken when serviceClientResponse.getStatus fails") {
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      Mockito.when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      Mockito.when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(TestValidToken(), Nil: _*)

      mockAkkaGetResponse(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse.failure("Unable to reach identity!")
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(PowerApiHeader.GROUPS.toString) shouldBe null

      mockAkkaServiceClient.validate()
    }

    it("handles 401 response from groups call") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      Mockito.when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      Mockito.when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
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
      mockAkkaServiceClient.validate()
    }

    it("handles 403 response from groups call") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      Mockito.when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      Mockito.when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(TestValidToken(), Nil: _*)

      mockAkkaGetResponses(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")(
        Seq(
          "glibglob" -> AkkaServiceClientResponse(HttpServletResponse.SC_FORBIDDEN, "")
        )
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(PowerApiHeader.GROUPS.toString) shouldBe null
      mockAkkaServiceClient.validate()
    }

    it("handles 413 response from groups call") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      Mockito.when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      Mockito.when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(TestValidToken(), Nil: _*)

      mockAkkaGetResponses(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")(
        Seq(
          "glibglob" -> AkkaServiceClientResponse(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "")
        )
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(PowerApiHeader.GROUPS.toString) shouldBe null
      mockAkkaServiceClient.validate()
    }

    it("handles 429 response from groups call") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      Mockito.when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      Mockito.when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(TestValidToken(), Nil: _*)

      mockAkkaGetResponses(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")(
        Seq(
          "glibglob" -> AkkaServiceClientResponse(SC_TOO_MANY_REQUESTS, "")
        )
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(PowerApiHeader.GROUPS.toString) shouldBe null
      mockAkkaServiceClient.validate()
    }

    it("handles unexpected response from groups call") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString, VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      Mockito.when(mockDatastore.get(ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      Mockito.when(mockDatastore.get(s"$TOKEN_KEY_PREFIX$VALID_TOKEN"))
        .thenReturn(TestValidToken(), Nil: _*)

      mockAkkaGetResponses(s"$GROUPS_KEY_PREFIX$VALID_TOKEN")(
        Seq(
          "glibglob" -> AkkaServiceClientResponse(HttpServletResponse.SC_NOT_IMPLEMENTED, "")
        )
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest.asInstanceOf[HttpServletRequest].getHeader(PowerApiHeader.GROUPS.toString) shouldBe null
      mockAkkaServiceClient.validate()
    }

    it("forwards the user's catalog in x-catalog header base64 JSON encoded by default") {
      pending
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
              defaultRegion: Option[String] = None) = {
      ValidToken(expirationDate,
        userId,
        username,
        tenantName,
        defaultTenantId,
        tenantIds,
        roles,
        impersonatorId,
        impersonatorName,
        defaultRegion)
    }
  }

}
