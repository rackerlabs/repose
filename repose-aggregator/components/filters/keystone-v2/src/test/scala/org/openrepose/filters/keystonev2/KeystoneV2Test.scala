package org.openrepose.filters.keystonev2

import java.io.ByteArrayInputStream
import javax.servlet.http.HttpServletRequest

import com.mockrunner.mock.web.{MockFilterChain, MockFilterConfig, MockHttpServletRequest, MockHttpServletResponse}
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.openrepose.commons.utils.http.ServiceClientResponse
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.{Datastore, DatastoreService}
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClientException
import org.openrepose.filters.Keystonev2.KeystoneV2Filter
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class KeystoneV2Test extends FunSpec
with Matchers
with BeforeAndAfter
with MockitoSugar
with IdentityResponses {
  System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
    "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl")

  val mockDatastoreService = mock[DatastoreService]
  private val mockDatastore: Datastore = mock[Datastore]
  Mockito.when(mockDatastoreService.getDefaultDatastore).thenReturn(mockDatastore)
  val mockAkkaServiceClient = new MockAkkaServiceClient
  val mockConfigService = mock[ConfigurationService]

  before {
    Mockito.reset(mockDatastore)
    Mockito.reset(mockConfigService)
    mockAkkaServiceClient.reset()
  }

  //TODO: pull all this up into some trait for use in testing
  //Used to unify an exception and a proper response to make it easier to handle in code
  trait AkkaParent

  object AkkaServiceClientResponse {
    def apply(status: Int, body: String): ServiceClientResponse with AkkaParent = {
      new ServiceClientResponse(status, new ByteArrayInputStream(body.getBytes)) with AkkaParent
    }

    def failure(reason: String, parent: Throwable = null) = {
      new AkkaServiceClientException(reason, parent) with AkkaParent
    }
  }

  def mockAkkaAdminTokenResponse(response: AkkaParent): Unit = {
    mockAkkaAdminTokenResponses(Seq(response))
  }

  def mockAkkaAdminTokenResponses(responses: Seq[AkkaParent]): Unit = {
    mockAkkaServiceClient.postResponses ++= responses.reverse.map {
      case x: ServiceClientResponse => Left(x)
      case x: AkkaServiceClientException => Right(x)
    }
  }

  def mockAkkaValidateTokenResponse(forToken: String)(adminToken: String, response: AkkaParent): Unit = {
    mockAkkaValidateTokenResponses(forToken)(Seq(adminToken -> response))
  }

  def mockAkkaValidateTokenResponses(forToken: String)(responses: Seq[(String, AkkaParent)]): Unit = {
    responses.foreach { case (adminToken, response) =>
      val key = (adminToken, forToken)
      val value = response match {
        case x: ServiceClientResponse => Left(x)
        case x: AkkaServiceClientException => Right(x)
      }
      mockAkkaServiceClient.getResponses.put(key, value)
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
      mockAkkaAdminTokenResponse {
        AkkaServiceClientResponse(200, adminAuthenticationTokenResponse())
      }

      //Urgh, I have to hit the akka service client twice
      mockAkkaValidateTokenResponse(VALID_TOKEN)(
        "glibglob", AkkaServiceClientResponse(200, validateTokenResponse())
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)

      mockAkkaServiceClient.validate()
    }
    it("rejects with 403 an invalid token") {
      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader("x-auth-token", "notValidToken")

      //Pretend like identity is going to give us a valid admin token
      mockAkkaAdminTokenResponse {
        AkkaServiceClientResponse(200, adminAuthenticationTokenResponse())
      }
      //Urgh, I have to hit the akka service client twice
      mockAkkaValidateTokenResponse("notValidToken")(
        "glibglob", AkkaServiceClientResponse(404, "")
      )
      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest should be(null)
      filterChain.getLastResponse should be(null)

      response.getStatus should be(403)
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
      mockAkkaValidateTokenResponses(VALID_TOKEN) {
        Seq(
          "glibglob" -> AkkaServiceClientResponse(403, ""),
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
          AkkaServiceClientResponse(200, adminAuthenticationTokenResponse()),
          AkkaServiceClientResponse(200, adminAuthenticationTokenResponse(token = "morty"))
        )
      }

      //When validating a token, we're going to not be authorized the first time,
      // Then we'll be authorized
      mockAkkaValidateTokenResponses(VALID_TOKEN) {
        Seq(
          "glibglob" -> AkkaServiceClientResponse(403, ""),
          "morty" -> AkkaServiceClientResponse(403, "")
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
      mockAkkaValidateTokenResponses(VALID_TOKEN) {
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
      mockAkkaValidateTokenResponses(VALID_TOKEN) {
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

      //Pretend like identity is going to give us a valid admin token
      mockAkkaAdminTokenResponse {
        AkkaServiceClientResponse(200, adminAuthenticationTokenResponse())
      }

      //Urgh, I have to hit the akka service client twice
      mockAkkaValidateTokenResponses(VALID_TOKEN)(
        Seq(
          "glibglob" -> AkkaServiceClientResponse(200, validateTokenResponse()),
          "glibglobEndpoints" -> AkkaServiceClientResponse(200, endpointsResponse())
        )
      )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      //Continues with the chain
      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)

      mockAkkaServiceClient.validate()
    }
    it("bypasses validation if the user has the role listed in bypass-validation-roles") {
      pending
    }
    it("rejects with 403 if the user does not have the required endpoint") {
      pending
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
