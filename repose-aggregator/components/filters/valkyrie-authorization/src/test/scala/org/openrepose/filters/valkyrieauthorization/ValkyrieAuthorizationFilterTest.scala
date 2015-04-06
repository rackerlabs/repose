package org.openrepose.filters.valkyrieauthorization

import java.io.ByteArrayInputStream
import java.net.URL
import javax.servlet.http.HttpServletResponse
import javax.servlet.{FilterChain, ServletRequest}

import com.mockrunner.mock.web.{MockFilterConfig, MockHttpServletRequest, MockHttpServletResponse}
import org.junit.runner.RunWith
import org.mockito.{ArgumentCaptor, Matchers, Mockito}
import org.openrepose.commons.utils.http.ServiceClientResponse
import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient
import org.openrepose.filters.valkyrieauthorization.config.{ValkyrieServer, DelegatingType, ValkyrieAuthorizationConfig}
import org.scalatest.FunSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class ValkyrieAuthorizationFilterTest extends FunSpec with MockitoSugar {

  describe("when initializing the filter") {
    it("should initialize the configuration to a given configuration") {
      val mockAkkaServiceClient = mock[AkkaServiceClient]
      val mockConfigService = mock[ConfigurationService]
      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mockConfigService, mockAkkaServiceClient)

      val config: MockFilterConfig = new MockFilterConfig
      config.setFilterName("ValkyrieFilter")

      filter.init(config)

      val resourceCaptor = ArgumentCaptor.forClass(classOf[URL])
      Mockito.verify(mockConfigService).subscribeTo(
        Matchers.eq("ValkyrieFilter"),
        Matchers.eq("valkyrie-authorization.cfg.xml"),
        resourceCaptor.capture,
        Matchers.eq(filter),
        Matchers.eq(classOf[ValkyrieAuthorizationConfig]))

      assert(resourceCaptor.getValue.toString.endsWith("/META-INF/schema/config/valkyrie-authorization.xsd"))
    }
    it("should initialize the configuration to a given name") {
      val mockConfigService = mock[ConfigurationService]
      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mockConfigService, mock[AkkaServiceClient])

      val config: MockFilterConfig = new MockFilterConfig
      config.setInitParameter("filter-config", "another-name.cfg.xml")

      filter.init(config)

      Mockito.verify(mockConfigService).subscribeTo(
        Matchers.anyString,
        Matchers.eq("another-name.cfg.xml"),
        Matchers.any(classOf[URL]),
        Matchers.any(classOf[ValkyrieAuthorizationFilter]),
        Matchers.eq(classOf[ValkyrieAuthorizationConfig]))
    }
  }

  describe("when destroying the filter") {
    it("should deregister the configuration from the configuration service") {
      val mockConfigService = mock[ConfigurationService]
      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mockConfigService, mock[AkkaServiceClient])

      val config: MockFilterConfig = new MockFilterConfig
      filter.init(config)
      filter.destroy

      Mockito.verify(mockConfigService).unsubscribeFrom("valkyrie-authorization.cfg.xml", filter)
    }
  }

  describe("when the configuration is updated") {
    it("should set the current configuration on the filter with the defaults initially and flag that it is initialized") {
      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], mock[AkkaServiceClient])

      assert(!filter.isInitialized)

      val configuration = new ValkyrieAuthorizationConfig
      filter.configurationUpdated(configuration)

      assert(filter.configuration == configuration)
      assert(filter.configuration.getDelegating == null)
      assert(filter.configuration.getCacheTimeoutMillis == 300000)
      assert(filter.isInitialized)
    }
    it("should set the default delegation quality to .1") {
      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], mock[AkkaServiceClient])

      assert(filter.configuration == null)

      val configuration = new ValkyrieAuthorizationConfig
      val delegation = new DelegatingType
      configuration.setDelegating(delegation)
      filter.configurationUpdated(configuration)

      assert(filter.configuration.getDelegating.getQuality == .1)
    }
    it("should set the configuration to current") {
      val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], mock[AkkaServiceClient])

      val configuration = new ValkyrieAuthorizationConfig
      filter.configurationUpdated(configuration)

      assert(filter.configuration == configuration)
      assert(filter.isInitialized)

      val newConfiguration = new ValkyrieAuthorizationConfig
      filter.configurationUpdated(newConfiguration)

      assert(filter.configuration == newConfiguration)
      assert(filter.isInitialized)
    }
  }

  describe("when a request to authorize occurs") {
    case class RequestProcessor(method: String, tenantHeader: String, deviceHeader: String, contactHeader: String)
    case class ValkyrieResponse(code: Int, payload: String)
    List((RequestProcessor("GET", "hybrid:someTenant", "123456", "123456"), ValkyrieResponse(200, createValkyrieResponse("123456", "view_product")), 200), //With colon in tenant
         (RequestProcessor("GET", "someTenant", "123456", "123456"), ValkyrieResponse(200, createValkyrieResponse("123456", "view_product")), 200), //Without colon in tenant
         (RequestProcessor("GET", "application:someTenant", "123456", "123456"), ValkyrieResponse(200, createValkyrieResponse("111111", "view_product")), 403), //Non matching device
         (RequestProcessor("PUT", "application:someTenant", "123456", "123456"), ValkyrieResponse(200, createValkyrieResponse("123456", "view_product")), 403), //Non matching role
         (RequestProcessor("GET", "application:someTenant", "123456", "123456"), ValkyrieResponse(200, createValkyrieResponse("123456", "edit_product")), 200), //Edit role
         (RequestProcessor("GET", "application:someTenant", "123456", "123456"), ValkyrieResponse(200, createValkyrieResponse("123456", "admin_product")), 200), //Admin role
         (RequestProcessor("GET", "hybrid:someTenant", "123456", "123456"), ValkyrieResponse(403, ""), 502), //Bad Permissions to Valkyrie
         (RequestProcessor("GET", "", "123456", "123456"), ValkyrieResponse(404, ""), 502), //Missing Tenant
         (RequestProcessor("GET", "hybrid:someTenant", "", "123456"), ValkyrieResponse(200, createValkyrieResponse("123456", "view_product")), 502), //Missing Device
         (RequestProcessor("GET", "hybrid:someTenant", "123456", ""), ValkyrieResponse(404, ""), 403),  //Missing Contact
         (RequestProcessor("GET", "hybrid:someTenant", "123456", "123456"), ValkyrieResponse(200, createValkyrieResponse("", "view_product")), 502),  //Malformed Valkyrie Response - Missing Device
         (RequestProcessor("GET", "hybrid:someTenant", "123456", "123456"), ValkyrieResponse(200, "I'm not really json"), 502)  //Malformed Valkyrie Response - Bad Json
    ).foreach { case (request, valkyrie, result) =>
      it(s"should be $result for $request with Valkyrie response of $valkyrie") {
        val akkaServiceClient: AkkaServiceClient = mock[AkkaServiceClient]
        val modifiedTenant: String = "someTenant"
        Mockito.when(akkaServiceClient.get(
          modifiedTenant + request.contactHeader,
          s"http://foo.com:8080/account/$modifiedTenant/permissions/contacts/devices/by_contact/${request.contactHeader}/effective",
          Map("X-Auth-User" -> "someUser", "X-Auth-Token" -> "somePassword")))
          .thenReturn(new ServiceClientResponse(valkyrie.code, new ByteArrayInputStream(valkyrie.payload.getBytes)))
        val filter: ValkyrieAuthorizationFilter = new ValkyrieAuthorizationFilter(mock[ConfigurationService], akkaServiceClient)

        val configuration = new ValkyrieAuthorizationConfig
        val server = new ValkyrieServer
        server.setUri("http://foo.com:8080")
        server.setUsername("someUser")
        server.setPassword("somePassword")
        configuration.setValkyrieServer(server)
        filter.configurationUpdated(configuration)

        val mockServletRequest = new MockHttpServletRequest
        mockServletRequest.setMethod(request.method)
        if(!request.tenantHeader.isEmpty) mockServletRequest.addHeader("X-Tenant-Id", request.tenantHeader)
        if(!request.deviceHeader.isEmpty) mockServletRequest.addHeader("X-Device-Id", request.deviceHeader)
        if(!request.contactHeader.isEmpty) mockServletRequest.addHeader("X-Contact-Id", request.contactHeader)

        val mockServletResponse = new MockHttpServletResponse
        val mockFilterChain = mock[FilterChain]

        filter.doFilter(mockServletRequest, mockServletResponse, mockFilterChain)

        if (result == 200) {
          val responseCaptor = ArgumentCaptor.forClass(classOf[MutableHttpServletResponse])
          Mockito.verify(mockFilterChain).doFilter(Matchers.any(classOf[ServletRequest]), responseCaptor.capture())
          assert(responseCaptor.getValue.getStatus == result)
        } else {
          assert(mockServletResponse.getStatusCode == result)
        }
      }
    }

//    it("should be able to delegate failures"){}
    //    it("should be able to cache"){}
    //    it("should be able to mask"){}
  }

  def createValkyrieResponse(deviceId: String, permissionName: String): String = {
    s"""{
         "contact_permissions" :[
           {
             "account_number":862323,
             "contact_id": 818029,
             "id": 0,
             ${ if(deviceId != "") "\"item_id\": " + deviceId + "," else "" }
             "item_type_id" : 1,
             "item_type_name" : "devices",
             "permission_name" : "$permissionName",
             "permission_type_id" : 12
           },
           {
             "account_number":862323,
             "contact_id": 818029,
             "id": 0,
             "item_id": ${deviceId}1,
             "item_type_id" : 1,
             "item_type_name" : "devices",
             "permission_name" : "${permissionName}1",
             "permission_type_id" : 12
           }
         ]
       }"""
  }
}
