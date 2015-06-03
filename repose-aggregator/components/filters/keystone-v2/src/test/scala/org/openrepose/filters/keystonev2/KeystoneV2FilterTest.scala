package org.openrepose.filters.keystonev2

import java.util.concurrent.TimeUnit
import javax.servlet.http.HttpServletRequest

import com.mockrunner.mock.web.{MockFilterChain, MockFilterConfig, MockHttpServletRequest, MockHttpServletResponse}
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.{Matchers => MockitoMatchers}
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.{Datastore, DatastoreService}
import org.openrepose.filters.Keystonev2.KeystoneV2Filter
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

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


  describe("Configured simply to authenticate tokens, defaults for everything else") {
    //Configure the filter
    val configuration = Marshaller.keystoneV2ConfigFromString(
      """<?xml version="1.0" encoding="UTF-8"?>
        |<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
        |    <identity-service
        |            username="username"
        |            password="password"
        |            uri="https://some.identity.com"
        |            set-groups-in-header="true"
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
      request.addHeader("x-auth-token", VALID_TOKEN)

      //Pretend like identity is going to give us a valid admin token
      mockAkkaPostResponse {
        AkkaServiceClientResponse(200, adminAuthenticationTokenResponse())
      }

      //Urgh, I have to hit the akka service client twice
      mockAkkaGetResponse(VALID_TOKEN)(
        "glibglob", AkkaServiceClientResponse(200, validateTokenResponse())
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
      request.addHeader("x-auth-token", VALID_TOKEN)

      //Pretend like identity is going to give us a valid admin token
      mockAkkaPostResponse {
        AkkaServiceClientResponse(200, adminAuthenticationTokenResponse())
      }

      mockAkkaGetResponse(VALID_TOKEN)(
        "glibglob", AkkaServiceClientResponse(200, validateTokenResponse())
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      Mockito.verify(mockDatastore).put(filter.ADMIN_TOKEN_KEY, "glibglob", 600, TimeUnit.SECONDS)

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)

      mockAkkaServiceClient.validate()
    }

    it("caches a valid token for 10 minutes") {
      //Can only make sure it was put into the cache with a 10 minute timeout...
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader("x-auth-token", VALID_TOKEN)

      //Pretend like identity is going to give us a valid admin token
      mockAkkaPostResponse {
        AkkaServiceClientResponse(200, adminAuthenticationTokenResponse())
      }

      mockAkkaGetResponse(VALID_TOKEN)(
        "glibglob", AkkaServiceClientResponse(200, validateTokenResponse())
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      Mockito.verify(mockDatastore).put(filter.ADMIN_TOKEN_KEY, "glibglob", 600, TimeUnit.SECONDS)
      //Have to cache the result of the stuff
      Mockito.verify(mockDatastore).put(VALID_TOKEN, filter.ValidToken(Vector("compute:admin", "object-store:admin")), 600, TimeUnit.SECONDS)

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)

      mockAkkaServiceClient.validate()
    }

    it("caches an INvalid token for 10 minutes") {
      //Can only make sure it was put into the cache with a 10 minute timeout...
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader("x-auth-token", "INVALID_TOKEN")

      //Pretend like identity is going to give us a valid admin token
      mockAkkaPostResponse {
        AkkaServiceClientResponse(200, adminAuthenticationTokenResponse())
      }

      mockAkkaGetResponse("INVALID_TOKEN")(
        "glibglob", AkkaServiceClientResponse(404, "")
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      Mockito.verify(mockDatastore).put(filter.ADMIN_TOKEN_KEY, "glibglob", 600, TimeUnit.SECONDS)
      //Have to cache the result of the stuff
      Mockito.verify(mockDatastore).put("INVALID_TOKEN", filter.InvalidToken, 600, TimeUnit.SECONDS)

      filterChain.getLastRequest should be(null)
      filterChain.getLastResponse should be(null)

      response.getStatus should be(401)
      mockAkkaServiceClient.validate()
    }

    it("Makes no other calls if the token is already cached with a valid result") {
      //Can only make sure it was put into the cache with a 10 minute timeout...
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader("x-auth-token", VALID_TOKEN)

      //When the user's token details are cached, no calls to identity should take place

      //When we ask the cache for our token, it works
      Mockito.when(mockDatastore.get(VALID_TOKEN)).thenReturn(filter.ValidToken(Vector("compute:admin", "object-store:admin")), Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

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
      request.addHeader("x-auth-token", "LOLNOPE")

      //When the user's token details are cached, no calls to identity should take place

      //When we ask the cache for our token, it works
      Mockito.when(mockDatastore.get("LOLNOPE")).thenReturn(filter.InvalidToken, Nil: _*) // Note: Nil was passed to resolve the ambiguity between Mockito's multiple method signatures

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest should be(null)
      filterChain.getLastResponse should be(null)

      response.getStatus should be(401)

      //So because we didn't add any interactions, this guy will validate with no interactions
      mockAkkaServiceClient.validate()
    }


    it("rejects with 401 an invalid token") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader("x-auth-token", "notValidToken")

      //Pretend like identity is going to give us a valid admin token
      mockAkkaPostResponse {
        AkkaServiceClientResponse(200, adminAuthenticationTokenResponse())
      }
      //Urgh, I have to hit the akka service client twice
      mockAkkaGetResponse("notValidToken")(
        "glibglob", AkkaServiceClientResponse(404, "")
      )
      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest should be(null)
      filterChain.getLastResponse should be(null)

      response.getStatus should be(401)
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

      response.getStatus should be(403)
      mockAkkaServiceClient.validate()

    }

    it("retries authentication as the admin user if the admin token is not valid") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader("x-auth-token", VALID_TOKEN)

      //Our admin token is good every time
      mockAkkaAdminTokenResponses {
        Seq(
          AkkaServiceClientResponse(200, adminAuthenticationTokenResponse()),
          AkkaServiceClientResponse(200, adminAuthenticationTokenResponse(token = "morty"))
        )
      }

      //When validating a token, we're going to not be authorized the first time,
      // Then we'll be authorized
      mockAkkaGetResponses(VALID_TOKEN) {
        Seq(
          "glibglob" -> AkkaServiceClientResponse(401, ""),
          "morty" -> AkkaServiceClientResponse(200, validateTokenResponse())
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
      request.addHeader("x-auth-token", VALID_TOKEN)

      //Our admin token is good every time
      mockAkkaAdminTokenResponses {
        Seq(
          AkkaServiceClientResponse(200, adminAuthenticationTokenResponse())
        )
      }

      //When validating a token, we're going to not be authorized the first time,
      // Then we'll be authorized
      mockAkkaGetResponses(VALID_TOKEN) {
        Seq(
          "glibglob" -> AkkaServiceClientResponse(403, "")
        )
      }
      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getStatus should be(500)

      filterChain.getLastRequest should be(null)
      filterChain.getLastResponse should be(null)
      mockAkkaServiceClient.validate()
    }

    it("rejects with 502 if we cannot reach identity") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader("x-auth-token", VALID_TOKEN)

      //Our admin token is good every time
      //Need to throw an exception from akka when trying to talk to it
      //The admin token retry logic doesn't retry when it's a 500 class error
      mockAkkaAdminTokenResponses {
        Seq(
          AkkaServiceClientResponse.failure("Unable to reach identity!")
        )
      }

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getStatus should be(502)

      filterChain.getLastRequest should be(null)
      filterChain.getLastResponse should be(null)
      mockAkkaServiceClient.validate()

    }

    it("rejects with 502 if we cannot authenticate as the admin user") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader("x-auth-token", VALID_TOKEN)

      //Our admin token is good every time
      mockAkkaAdminTokenResponses {
        Seq(
          AkkaServiceClientResponse(200, adminAuthenticationTokenResponse())
        )
      }

      //When validating a token, we're going to not be authorized the first time,
      // Then we'll be authorized
      mockAkkaGetResponses(VALID_TOKEN) {
        Seq(
          "glibglob" -> AkkaServiceClientResponse.failure("Unable to talk to identity!")
        )
      }
      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getStatus should be(502)

      filterChain.getLastRequest should be(null)
      filterChain.getLastResponse should be(null)
      mockAkkaServiceClient.validate()
    }

    it("includes the user's groups in the x-pp-group header in the request") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader("x-auth-token", VALID_TOKEN)

      //Our admin token is good every time
      mockAkkaAdminTokenResponses {
        Seq(
          AkkaServiceClientResponse(200, adminAuthenticationTokenResponse())
        )
      }

      //Validate the token response with groups to grab them!
      mockAkkaGetResponses(VALID_TOKEN) {
        Seq(
          "glibglob" -> AkkaServiceClientResponse(200, validateTokenResponse())
        )
      }
      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      val processedRequest = filterChain.getLastRequest.asInstanceOf[HttpServletRequest]
      processedRequest.getHeader("x-pp-groups") should be("compute:admin,object-store:admin")

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
        |            connection-pool-id="wat"
        |            set-groups-in-header="true"
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
      request.addHeader("x-auth-token", VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      Mockito.when(mockDatastore.get(filter.ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      //Urgh, I have to hit the akka service client twice
      mockAkkaGetResponses(VALID_TOKEN)(
        Seq(
          "glibglob" -> AkkaServiceClientResponse(200, validateTokenResponse())
        )
      )
      mockAkkaGetResponse("validTokenEndpoints")(
        "glibglob", AkkaServiceClientResponse(200, endpointsResponse())
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      //Continues with the chain
      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)

      mockAkkaServiceClient.validate()
    }
    it("rejects with 403 if the user does not have the required endpoint") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader("x-auth-token", VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      Mockito.when(mockDatastore.get(filter.ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      //Urgh, I have to hit the akka service client twice
      mockAkkaGetResponses(VALID_TOKEN)(
        Seq(
          "glibglob" -> AkkaServiceClientResponse(200, validateTokenResponse())
        )
      )
      mockAkkaGetResponse("validTokenEndpoints")(
        "glibglob", AkkaServiceClientResponse(200, oneEndpointResponse())
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      response.getStatus shouldBe 403
      //Continues with the chain
      filterChain.getLastRequest should be(null)
      filterChain.getLastResponse should be(null)

      mockAkkaServiceClient.validate()
    }
    it("bypasses validation if the user has the role listed in bypass-validation-roles") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader("x-auth-token", VALID_TOKEN)

      //Pretend like the admin token is cached all the time
      Mockito.when(mockDatastore.get(filter.ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

      //Urgh, I have to hit the akka service client twice
      mockAkkaGetResponses(VALID_TOKEN)(
        Seq(
          "glibglob" -> AkkaServiceClientResponse(200, validateRackerTokenResponse())
        )
      )
      mockAkkaGetResponse("validTokenEndpoints")(
        "glibglob", AkkaServiceClientResponse(200, oneEndpointResponse())
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
        request.addHeader("x-auth-token", VALID_TOKEN)

        //Pretend like the admin token is cached all the time
        Mockito.when(mockDatastore.get(filter.ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

        //Urgh, I have to hit the akka service client twice
        mockAkkaGetResponses(VALID_TOKEN)(
          Seq(
            "glibglob" -> AkkaServiceClientResponse(200, validateTokenResponse())
          )
        )

        val endpointsList = Vector(filter.Endpoint(Some("DERP"), Some("Compute"), Some("compute"), "https://compute.north.public.com/v1"))
        Mockito.when(mockDatastore.get("validTokenEndpoints")).thenReturn(endpointsList, Nil: _*)

        val response = new MockHttpServletResponse
        val filterChain = new MockFilterChain()
        filter.doFilter(request, response, filterChain)

        response.getStatus shouldBe 403
        filterChain.getLastRequest should be(null)
        filterChain.getLastResponse should be(null)

        mockAkkaServiceClient.validate()
      }
      it("will allow through if the user has the endpoint") {
        //make a request and validate that it called the akka service client?
        val request = new MockHttpServletRequest()
        request.addHeader("x-auth-token", VALID_TOKEN)

        //Pretend like the admin token is cached all the time
        Mockito.when(mockDatastore.get(filter.ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

        //Urgh, I have to hit the akka service client twice
        mockAkkaGetResponses(VALID_TOKEN)(
          Seq(
            "glibglob" -> AkkaServiceClientResponse(200, validateTokenResponse())
          )
        )

        val endpointsList = Vector(filter.Endpoint(Some("Global"), Some("Compute"), Some("compute"), "https://compute.north.public.com/v1"))
        Mockito.when(mockDatastore.get("validTokenEndpoints")).thenReturn(endpointsList, Nil: _*)

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
        request.addHeader("x-auth-token", VALID_TOKEN)

        //Pretend like the admin token is cached all the time
        Mockito.when(mockDatastore.get(filter.ADMIN_TOKEN_KEY)).thenReturn("glibglob", Nil: _*)

        //Urgh, I have to hit the akka service client twice
        mockAkkaGetResponses(VALID_TOKEN)(
          Seq(
            "glibglob" -> AkkaServiceClientResponse(200, validateRackerTokenResponse())
          )
        )

        val endpointsList = Vector(filter.Endpoint(Some("DERP"), Some("LOLNOPE"), Some("compute"), "https://compute.north.public.com/v1"))
        Mockito.when(mockDatastore.get("validTokenEndpoints")).thenReturn(endpointsList, Nil: _*)

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
    it("will not perform authentication or authorization for that URI") {
      pending
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
    it("will extract the tenant from the URI and validate that the user has that tenant in their list") {
      pending
    }
    it("sends all tenant IDs when configured to") {
      pending
    }
    it("sends all tenant IDs with a quality when both are configured") {
      pending
    }
    it("sends tenant quality when not configured to send all tenant IDs") {
      pending
    }
    it("bypasses the URI tenant validation check when a user has a role in the bypass-validation-roles list") {
      pending
    }
    it("does not fail if the user doesn't have any tenants") {
      pending
    }
    it("sends the tenant matching the URI when send all tenants is false and validate-tenant is enabled") {
      pending
    }
    it("sends the user's default tenant, if validate-tenant is not enabled") {
      pending
    }
    describe("sending all tenant ids") {
      it("sends the URI tenant with the highest quality") {
        pending
      }
      it("sends the default tenant with a lower quality than the one in the URI, but higher than the rest") {
        pending
      }
      it("sends the remaining tenants with the lowest quality than the others") {
        pending
      }
    }
  }

  describe("Forwarding information enabled") {
    it("forwards the groups in the x-pp-groups header by default") {
      pending
    }
    it("forwards the user's catalog in x-catalog header base64 JSON encoded by default") {
      pending
    }
  }


}
