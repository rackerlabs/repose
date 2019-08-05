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
package org.openrepose.filters.simplerbac

import java.io.File
import java.net.URL
import javax.servlet.http.HttpServletResponse.{SC_FORBIDDEN, SC_METHOD_NOT_ALLOWED, SC_NOT_FOUND, SC_OK}

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.junit.runner.RunWith
import org.mockito.{ArgumentCaptor, Matchers, Mockito}
import org.openrepose.commons.config.resource.{ConfigurationResource, ConfigurationResourceResolver}
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.simplerbac.config.{DelegatingType, ResourcesType, SimpleRbacConfig}
import org.scalatest._
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.springframework.mock.web.{MockFilterChain, MockFilterConfig, MockHttpServletRequest, MockHttpServletResponse}

import scala.collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class SimpleRbacFilterTest extends FunSpec with BeforeAndAfterEach with GivenWhenThen with org.scalatest.Matchers with MockitoSugar {
  var filter: SimpleRbacFilter = _
  var config: SimpleRbacConfig = _
  var servletRequest: MockHttpServletRequest = _
  var servletResponse: MockHttpServletResponse = _
  var filterChain: MockFilterChain = _
  var mockConfigService: ConfigurationService = _
  var mockFilterConfig: MockFilterConfig = _

  override def beforeEach(): Unit = {
    servletRequest = new MockHttpServletRequest
    servletResponse = new MockHttpServletResponse
    filterChain = new MockFilterChain
    mockConfigService = mock[ConfigurationService]
    mockFilterConfig = new MockFilterConfig("SimpleRbacFilter")
    filter = new SimpleRbacFilter(mockConfigService, new File("./build").getAbsolutePath)
    config = new SimpleRbacConfig
    config.setWadlOutput("simple-rbac.wadl")
    config.setDotOutput("simple-rbac.dot")
    val resources = new ResourcesType
    resources.setValue(
      """/path/to/this  get       role1,role2,role3,role4
        |/path/to/this  PUT       role1,role2,role3
        |/path/to/this  POST      role1,role2
        |/path/to/this  DELETE    role1
        |/path/to/that  GET,put   any
        |/path/to/that  ALL       role1
        |""".stripMargin.trim()
    )
    config.setResources(resources)
  }

  override def afterEach(): Unit = {
    if (filter.isInitialized) filter.destroy()
  }

  def resultShould: Int => String = { int => if (int == SC_OK) "should" else "should not" }
  def maskWith: Boolean => String = { boolean => if (boolean) "with" else "without" }

  describe("when the configuration is updated") {
    it("should have a default Delegation Type") {
      Given("an un-initialized filter and the default configuration")
      filter.configuration shouldBe null
      !filter.isInitialized

      When("the configuration is updated")
      filter.configurationUpdated(config)

      Then("the Delegating Type should be default")
      filter.configuration.getDelegating shouldBe null
    }
    it("should have a default Delegation Quality") {
      Given("an un-initialized filter and the default configuration")
      filter.configuration shouldBe null
      !filter.isInitialized

      When("the Delegating Type is set with a default and the configuration is updated")
      config.setDelegating(new DelegatingType)
      filter.configurationUpdated(config)

      Then("the Delegating Quality should be default")
      filter.configuration.getDelegating.getQuality shouldBe .3
    }
    it("should have a default Roles Header Name") {
      Given("an un-initialized filter and the default configuration")
      filter.configuration shouldBe null
      !filter.isInitialized

      When("the configuration is updated")
      filter.configurationUpdated(config)

      Then("the Roles Header Name should be default")
      filter.configuration.getRolesHeaderName shouldBe "X-ROLES"
    }
    it("should have a default Enable Masking 403's") {
      Given("an un-initialized filter and the default configuration")
      filter.configuration shouldBe null
      !filter.isInitialized

      When("the configuration is updated")
      filter.configurationUpdated(config)

      Then("the Enable Masking 403's should be default")
      !filter.configuration.isMaskRaxRoles403
    }
    it("should set the current configuration on the filter with the defaults initially and flag that it is initialized") {
      Given("an un-initialized filter and a modified configuration")
      filter.configuration shouldBe null
      !filter.isInitialized
      val configuration = new SimpleRbacConfig
      val delegatingType = new DelegatingType
      delegatingType.setQuality(1.0d)
      configuration.setDelegating(delegatingType)
      configuration.setRolesHeaderName("NEW-HEADER-NAME")
      configuration.setMaskRaxRoles403(true)
      val resources = new ResourcesType
      resources.setValue("/path/to/good  ALL       ANY")
      configuration.setResources(resources)

      When("the configuration is updated")
      filter.configurationUpdated(configuration)

      Then("the filter's configuration should be modified")
      filter.isInitialized
      filter.configuration shouldBe configuration
      filter.configuration.getDelegating.getQuality shouldBe 1.0d
      filter.configuration.getRolesHeaderName shouldBe "NEW-HEADER-NAME"
      filter.configuration.isMaskRaxRoles403
    }
  }

  describe("when initializing the filter") {
    it("should initialize the configuration to the default configuration") {
      Given("an un-initialized filter and a mock'd Filter Config")
      filter.configuration shouldBe null
      !filter.isInitialized
      val resourceCaptor = ArgumentCaptor.forClass(classOf[URL])

      When("the filter is initialized")
      filter.init(mockFilterConfig)

      Then("the filter should register with the ConfigurationService")
      Mockito.verify(mockConfigService).subscribeTo(
        Matchers.eq("SimpleRbacFilter"),
        Matchers.eq("simple-rbac.cfg.xml"),
        resourceCaptor.capture,
        Matchers.eq(filter),
        Matchers.eq(classOf[SimpleRbacConfig]))

      resourceCaptor.getValue.toString.endsWith("/META-INF/schema/config/simple-rbac.xsd")
    }
    it("should initialize the configuration to the given configuration") {
      Given("an un-initialized filter and a mock'd Filter Config")
      filter.configuration shouldBe null
      !filter.isInitialized
      mockFilterConfig.addInitParameter("filter-config", "another-name.cfg.xml")

      When("the filter is initialized")
      filter.init(mockFilterConfig)

      Then("the filter should register with the ConfigurationService")
      Mockito.verify(mockConfigService).subscribeTo(
        Matchers.anyString,
        Matchers.eq("another-name.cfg.xml"),
        Matchers.any(classOf[URL]),
        Matchers.any(classOf[SimpleRbacFilter]),
        Matchers.eq(classOf[SimpleRbacConfig]))
    }
  }

  describe("when destroying the filter") {
    it("should unregister the configuration from the configuration service") {
      Given("an un-initialized filter and a mock'd Filter Config")
      filter.configuration shouldBe null
      !filter.isInitialized
      mockFilterConfig.addInitParameter("filter-config", "another-name.cfg.xml")

      When("the filter is initialized and destroyed")
      filter.init(mockFilterConfig)
      filter.destroy()

      Then("the filter should unregister with the ConfigurationService")
      Mockito.verify(mockConfigService).unsubscribeFrom("another-name.cfg.xml", filter)
    }
  }

  describe("when the configured resources list has a bad line") {
    it("should log the bad line") {
      Given("an un-initialized filter and a configuration with a malformed resource in the list")
      val ctx = LogManager.getContext(false).asInstanceOf[LoggerContext]
      val listAppender = ctx.getConfiguration.getAppender("List0").asInstanceOf[ListAppender].clear
      filter.configuration shouldBe null
      !filter.isInitialized
      val resources = new ResourcesType
      resources.setValue(
        """
          |/path/to/good  ALL       ANY
          |/path/to/bad   ALL
          | """.stripMargin.trim()
      )
      config.setResources(resources)

      When("the configuration is updated")
      filter.configurationUpdated(config)

      Then("the filter's configuration should be modified")
      filter.isInitialized
      val events = listAppender.getEvents.toList.map(_.getMessage.getFormattedMessage)
      events.count(_.contains("Malformed RBAC Resource: ")) shouldBe 1
    }
  }

  describe("when the configured resources are in a file") {
    it("should load the file") {
      Given("an un-initialized filter and a configuration with the resources in a file")
      val ctx = LogManager.getContext(false).asInstanceOf[LoggerContext]
      val listAppender = ctx.getConfiguration.getAppender("List0").asInstanceOf[ListAppender].clear
      filter.configuration shouldBe null
      !filter.isInitialized
      val configuration = new SimpleRbacConfig
      val rbacFileName = "/rbac/test.rbac"
      val resources = new ResourcesType
      resources.setHref(rbacFileName)
      configuration.setResources(resources)
      val resourceResolver: ConfigurationResourceResolver = mock[ConfigurationResourceResolver]
      val configurationResource: ConfigurationResource = mock[ConfigurationResource]
      org.mockito.Mockito.when(configurationResource.newInputStream).thenReturn(this.getClass.getResourceAsStream(rbacFileName))
      org.mockito.Mockito.when(resourceResolver.resolve(rbacFileName)).thenReturn(configurationResource)
      org.mockito.Mockito.when(mockConfigService.getResourceResolver).thenReturn(resourceResolver)

      When("the configuration is updated")
      filter.configurationUpdated(configuration)

      Then("the filter's configuration should be modified")
      filter.isInitialized
      val events = listAppender.getEvents.toList.map(_.getMessage.getFormattedMessage)
      events.count(_.contains("Malformed RBAC Resource: /path/to/file")) shouldBe 1
    }
  }

  List((false, 0), (true, 1)).foreach { case (isEnabled, total) =>
    val enableDis: Boolean => String = { boolean => if (boolean) "enables" else "disables" }
    val enableShould: Boolean => String = { boolean => if (boolean) "should" else "should not" }
    describe(s"when the configuration ${enableDis(isEnabled)} API Coverage") {
      List("GET", "PUT", "POST", "DELETE").foreach { case (method) =>
        it(s"${enableShould(isEnabled)} log the API Checker when enabled path when using HTTP method $method.") {
          Given(s"a request using HTTP method $method")
          val ctx = LogManager.getContext(false).asInstanceOf[LoggerContext]
          val listAppender = ctx.getConfiguration.getAppender("List0").asInstanceOf[ListAppender].clear
          servletRequest.setRequestURI("/path/to/bad")
          servletRequest.setMethod(method)
          servletRequest.addHeader(config.getRolesHeaderName, "role1")
          config.setEnableApiCoverage(isEnabled)
          filter.configurationUpdated(config)

          When("the resource is requested")
          filter.doFilter(servletRequest, servletResponse, filterChain)

          Then(s"the API Checker path ${enableShould(isEnabled)} be logged.")
          val events = listAppender.getEvents.toList.map(_.getMessage.getFormattedMessage)
          events.count(_.contains("""{"steps":["S0","""")) shouldBe total
        }
      }
    }
  }

  describe(s"when attempting to access a resource that is not in the list") {
    List("GET", "PUT", "POST", "DELETE").foreach { case (method) =>
      it(s"should not allow the request to the resource when using HTTP method $method.") {
        Given(s"a request using HTTP method $method")
        servletRequest.setRequestURI("/path/to/bad")
        servletRequest.setMethod(method)
        servletRequest.addHeader(config.getRolesHeaderName, "role1")
        filter.configurationUpdated(config)

        When("the protected resource is requested")
        filter.doFilter(servletRequest, servletResponse, filterChain)

        Then(s"the request should not be allowed access")
          servletResponse.getStatus shouldBe SC_NOT_FOUND
      }
    }
  }

  List(false, true).foreach { case (isMasked) =>
    describe(s"Simple RBAC ${maskWith(isMasked)} masked roles") {
      List(
        //Method    Role      Result        Masked
        ("GET",     "role1",  SC_OK,        SC_OK),
        ("PUT",     "role1",  SC_OK,        SC_OK),
        ("POST",    "role1",  SC_OK,        SC_OK),
        ("DELETE",  "role1",  SC_OK,        SC_OK),
        ("GET",     "role2",  SC_OK,        SC_OK),
        ("PUT",     "role2",  SC_OK,        SC_OK),
        ("POST",    "role2",  SC_OK,        SC_OK),
        ("DELETE",  "role2",  SC_FORBIDDEN, SC_METHOD_NOT_ALLOWED),
        ("GET",     "role3",  SC_OK,        SC_OK),
        ("PUT",     "role3",  SC_OK,        SC_OK),
        ("POST",    "role3",  SC_FORBIDDEN, SC_METHOD_NOT_ALLOWED),
        ("DELETE",  "role3",  SC_FORBIDDEN, SC_METHOD_NOT_ALLOWED),
        ("GET",     "role4",  SC_OK,        SC_OK),
        ("PUT",     "role4",  SC_FORBIDDEN, SC_METHOD_NOT_ALLOWED),
        ("POST",    "role4",  SC_FORBIDDEN, SC_METHOD_NOT_ALLOWED),
        ("DELETE",  "role4",  SC_FORBIDDEN, SC_METHOD_NOT_ALLOWED),
        ("GET",     "role5",  SC_FORBIDDEN, SC_NOT_FOUND),
        ("PUT",     "role5",  SC_FORBIDDEN, SC_NOT_FOUND),
        ("POST",    "role5",  SC_FORBIDDEN, SC_NOT_FOUND),
        ("DELETE",  "role5",  SC_FORBIDDEN, SC_NOT_FOUND)
      ).foreach { case (method, role, result, masked) =>
        it(s"${resultShould(result)} allow the request to THIS resource when using HTTP method $method and having role $role.") {
          Given(s"a request using HTTP method $method and having role $role")
          servletRequest.setRequestURI("/path/to/this")
          servletRequest.setMethod(method)
          servletRequest.addHeader(config.getRolesHeaderName, role)
          config.setMaskRaxRoles403(isMasked)
          filter.configurationUpdated(config)

          When("the protected resource is requested")
          filter.doFilter(servletRequest, servletResponse, filterChain)

          Then(s"the request ${resultShould(result)} be allowed access")
          if (isMasked) {
            servletResponse.getStatus shouldBe masked
          } else {
            servletResponse.getStatus shouldBe result
          }
        }
      }

      List(
        //Method    Role      Result        Masked
        ("GET",     "role1",  SC_OK,        SC_OK),
        ("PUT",     "role1",  SC_OK,        SC_OK),
        ("POST",    "role1",  SC_OK,        SC_OK),
        ("DELETE",  "role1",  SC_OK,        SC_OK),
        ("GET",     "role2",  SC_OK,        SC_OK),
        ("PUT",     "role2",  SC_OK,        SC_OK),
        ("POST",    "role2",  SC_FORBIDDEN, SC_METHOD_NOT_ALLOWED),
        ("DELETE",  "role2",  SC_FORBIDDEN, SC_METHOD_NOT_ALLOWED)
      ).foreach { case (method, role, result, masked) =>
        it(s"${resultShould(result)} allow the request to THAT resource when using HTTP method $method and having role $role.") {
          Given("a request with roles")
          servletRequest.setRequestURI("/path/to/that")
          servletRequest.setMethod(method)
          servletRequest.addHeader(config.getRolesHeaderName, role)
          config.setMaskRaxRoles403(isMasked)
          filter.configurationUpdated(config)

          When("the request is to access a protected resource")
          filter.doFilter(servletRequest, servletResponse, filterChain)

          Then(s"the request ${resultShould(result)} be allowed access")
          if (isMasked) {
            servletResponse.getStatus shouldBe masked
          } else {
            servletResponse.getStatus shouldBe result
          }
        }
      }

      List(
        //Method    Result        Masked
        ("GET",     SC_OK,        SC_OK),
        ("PUT",     SC_OK,        SC_OK),
        ("POST",    SC_FORBIDDEN, SC_METHOD_NOT_ALLOWED),
        ("DELETE",  SC_FORBIDDEN, SC_METHOD_NOT_ALLOWED)
      ).foreach { case (method, result, masked) =>
        it(s"${resultShould(result)} allow the request to THAT resource when using HTTP method $method and having no roles.") {
          Given("a request without roles")
          servletRequest.setRequestURI("/path/to/that")
          servletRequest.setMethod(method)
          config.setMaskRaxRoles403(isMasked)
          filter.configurationUpdated(config)

          When("the request is to access a protected resource")
          filter.doFilter(servletRequest, servletResponse, filterChain)

          Then(s"the request ${resultShould(result)} be allowed access")
          if (isMasked) {
            servletResponse.getStatus shouldBe masked
          } else {
            servletResponse.getStatus shouldBe result
          }
        }
      }

      List(
        //Method    RolesA          RolesB          Result        Masked
        ("GET",     "role1, roleA", "role5, roleB", SC_OK,        SC_OK),
        ("PUT",     "role1, roleA", "role5, roleB", SC_OK,        SC_OK),
        ("POST",    "role1, roleA", "role5, roleB", SC_OK,        SC_OK),
        ("DELETE",  "role1, roleA", "role5, roleB", SC_OK,        SC_OK),
        ("GET",     "role4, roleA", "role5, roleB", SC_OK,        SC_OK),
        ("PUT",     "role4, roleA", "role5, roleB", SC_FORBIDDEN, SC_METHOD_NOT_ALLOWED),
        ("POST",    "role4, roleA", "role5, roleB", SC_FORBIDDEN, SC_METHOD_NOT_ALLOWED),
        ("DELETE",  "role4, roleA", "role5, roleB", SC_FORBIDDEN, SC_METHOD_NOT_ALLOWED)
      ).foreach { case (method, rolesa, rolesb, result, masked) =>
        it(s"${resultShould(result)} allow the request to THIS resource when using HTTP method $method and having the roles $rolesa and $rolesb.") {
          Given("a request multiple roles")
          servletRequest.setRequestURI("/path/to/this")
          servletRequest.setMethod(method)
          servletRequest.addHeader(config.getRolesHeaderName, rolesa)
          servletRequest.addHeader(config.getRolesHeaderName, rolesb)
          config.setMaskRaxRoles403(isMasked)
          filter.configurationUpdated(config)

          When("the request is to access a protected resource")
          filter.doFilter(servletRequest, servletResponse, filterChain)

          Then(s"the request ${resultShould(result)} be allowed access")
          if (isMasked) {
            servletResponse.getStatus shouldBe masked
          } else {
            servletResponse.getStatus shouldBe result
          }
        }
      }
    }
  }

  List(
    //Path                Result
    ("/path/to/some/sub", SC_OK),
    ("/path/to-some/sub", SC_METHOD_NOT_ALLOWED)
  ).foreach { case (path, result) =>
    it(s"${resultShould(result)} allow the request to PARAM resource $path.") {
      Given("a request")
      val resources = new ResourcesType
      resources.setValue(
        """
          |/path/to/good               ALL       ANY
          |/path/{param1}/{param2}/sub GET       ANY
          |""".stripMargin.trim()
      )
      config.setResources(resources)
      servletRequest.setRequestURI(path)
      servletRequest.setMethod("GET")
      filter.configurationUpdated(config)

      When("the request is to access a parameterized resource")
      filter.doFilter(servletRequest, servletResponse, filterChain)

      Then("the request should be allowed access")
      servletResponse.getStatus shouldBe result
    }
  }

  List(
    //Role        Result
    ("role1 wsp", SC_OK       ),
    ("role2 wsp", SC_OK       ),
    ("role3 wsp", SC_OK       ),
    ("role1",     SC_FORBIDDEN),
    ("role2",     SC_FORBIDDEN),
    ("role3",     SC_FORBIDDEN),
    ("wsp",       SC_FORBIDDEN)
  ).foreach { case (role, result) =>
    it(s"${resultShould(result)} allow the request to resource /path/to/space with role $role.") {
      Given(s"a request with role $role")
      val resources = new ResourcesType
      resources.setValue("/path/to/space   GET   role1 wsp , role2 wsp,role3 wsp")
      config.setResources(resources)
      servletRequest.setRequestURI("/path/to/space")
      servletRequest.setMethod("GET")
      servletRequest.addHeader(config.getRolesHeaderName, role)
      filter.configurationUpdated(config)

      When("the request is to access the resource")
      filter.doFilter(servletRequest, servletResponse, filterChain)

      Then(s"the request ${resultShould(result)} be allowed access")
      servletResponse.getStatus shouldBe result
    }
  }
}
