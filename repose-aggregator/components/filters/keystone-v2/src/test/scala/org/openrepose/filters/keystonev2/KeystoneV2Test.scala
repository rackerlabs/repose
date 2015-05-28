package org.openrepose.filters.keystonev2

import java.io.ByteArrayInputStream

import com.mockrunner.mock.web.{MockFilterChain, MockFilterConfig, MockHttpServletRequest, MockHttpServletResponse}
import org.junit.runner.RunWith
import org.mockito.{Matchers => MockitoMatchers, Mockito}
import org.openrepose.commons.utils.http.ServiceClientResponse
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.{Datastore, DatastoreService}
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient
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

  before {
    Mockito.reset(mockDatastore)
  }


  describe("Performing simple authentication of tokens") {
    it("Validates a token allowing through the filter") {
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

      val mockAkkaServiceClient = mock[AkkaServiceClient]
      val mockConfigService = mock[ConfigurationService]
      val filter: KeystoneV2Filter = new KeystoneV2Filter(mockConfigService, mockAkkaServiceClient, mockDatastoreService)

      val config: MockFilterConfig = new MockFilterConfig
      filter.init(config)
      filter.configurationUpdated(configuration)

      //make a request and validate that it called the akka service client?
      val request = new MockHttpServletRequest()
      request.addHeader("x-auth-token", "aValidToken")

      //Pretend like identity is going to give us a valid admin token
      Mockito.when(mockAkkaServiceClient.post(MockitoMatchers.eq("v2AdminTokenAuthentication"),
        MockitoMatchers.any(),
        MockitoMatchers.any(),
        MockitoMatchers.any(),
        MockitoMatchers.any())).thenReturn(
          new ServiceClientResponse(200, new ByteArrayInputStream(authenticateTokenResponse().getBytes))
        )
      //Urgh, I have to hit the akka service client twice
      Mockito.when(mockAkkaServiceClient.get(MockitoMatchers.eq("aValidToken"),
        MockitoMatchers.any(),
        MockitoMatchers.any())).thenReturn(
          new ServiceClientResponse(200, new ByteArrayInputStream("".getBytes))
        )

      val response = new MockHttpServletResponse
      val filterChain = new MockFilterChain()
      filter.doFilter(request, response, filterChain)

      filterChain.getLastRequest shouldNot be(null)
      filterChain.getLastResponse shouldNot be(null)
    }
    it("rejects with 403 an invalid token") {
      pending
    }
    it("validates only once for many requests within a a second") {
      pending
    }
    it("rejects with 403 if no x-auth-token is present") {
      pending
    }
    it("retries authentication as the admin user if the admin token is not valid") {
      pending
    }
    it("rejects with 403 if akka is unable to authorize") {
      pending
    }
    it("rejects with 502 if we cannot reach identity") {
      pending
    }
    it("rejects with 502 if we cannot authenticate as the admin user") {
      pending
    }
  }

  describe("Authorization parts") {
    it("ensures that a valid user has the required service endpoint") {
      pending
    }
    it("bypasses validation if the user has the role listed in bypass-validation-roles") {
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
