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
package org.openrepose.filters.regexrbac

import java.net.URL
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse._

import com.rackspace.httpdelegation.{HttpDelegationHeaderNames, HttpDelegationManager}
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.hamcrest.Matchers.{endsWith, hasProperty}
import org.junit.runner.RunWith
import org.mockito.Matchers.{any, anyString, argThat, same, eq => eql}
import org.mockito.Mockito
import org.mockito.Mockito.{when => whenMock, _}
import org.openrepose.commons.config.manager.UpdateFailedException
import org.openrepose.commons.config.resource.{ConfigurationResource, ConfigurationResourceResolver}
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.regexrbac.config.{DelegatingType, RegexRbacConfig, ResourcesType}
import org.scalatest._
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.springframework.mock.web.{MockFilterChain, MockFilterConfig, MockHttpServletRequest, MockHttpServletResponse}

import scala.collection.JavaConversions._
import scala.language.postfixOps
import scala.util.Success
import org.openrepose.commons.utils.http.OpenStackServiceHeader.ROLES
import org.openrepose.commons.utils.http.PowerApiHeader.RELEVANT_ROLES

@RunWith(classOf[JUnitRunner])
class RegexRbacFilterTest
  extends FunSpec
    with Matchers
    with MockitoSugar
    with BeforeAndAfterEach
    with GivenWhenThen
    with HttpDelegationManager {

  var filter: RegexRbacFilter = _
  var config: RegexRbacConfig = _
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
    mockFilterConfig = new MockFilterConfig("RegexRbacFilter")
    filter = new RegexRbacFilter(mockConfigService)
    config = new RegexRbacConfig
    val resources = new ResourcesType
    resources.setValue(
      """/path/[^/]+/.* get       role1,role2,role3,role4
        |/path/[^/]+/.* PUT       role1,role2,role3
        |/path/[^/]+/.* POST      role1,role2
        |/path/[^/]+/.* DELETE    role1
        |/Path/[^/]+/.* GET,put   any
        |/Path/[^/]+    ALL       ANY
        |/TEST/[^/]+    ALL       ANY
        |""".stripMargin.trim()
    )
    config.setResources(resources)
  }

  describe("when initializing the filter") {
    it("should initialize the configuration to the default configuration") {
      Given("an un-initialized filter and a mock'd Filter Config")
      filter.configuration shouldBe null
      !filter.isInitialized

      When("the filter is initialized")
      filter.init(mockFilterConfig)

      Then("the filter should register with the ConfigurationService")
      verify(mockConfigService).subscribeTo(
        eql("RegexRbacFilter"),
        eql("regex-rbac.cfg.xml"),
        argThat(hasProperty("path", endsWith("/META-INF/schema/config/regex-rbac.xsd"))),
        same(filter),
        any(classOf[Class[RegexRbacConfig]]))
      !filter.isInitialized
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
        anyString,
        eql("another-name.cfg.xml"),
        any(classOf[URL]),
        any(classOf[RegexRbacFilter]),
        eql(classOf[RegexRbacConfig]))
      !filter.isInitialized
    }
  }

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
    it("should have a default Enable Masking 403's") {
      Given("an un-initialized filter and the default configuration")
      filter.configuration shouldBe null
      !filter.isInitialized

      When("the configuration is updated")
      filter.configurationUpdated(config)

      Then("the Enable Masking 403's should be default")
      !filter.configuration.isMaskRaxRoles403
    }
    it("should set the current configuration on the filter with the defaults initially and indicate that it is initialized") {
      Given("an un-initialized filter and a modified configuration")
      filter.configuration shouldBe null
      !filter.isInitialized
      val configuration = new RegexRbacConfig
      val delegatingType = new DelegatingType
      delegatingType.setQuality(1.0d)
      configuration.setDelegating(delegatingType)
      configuration.setMaskRaxRoles403(true)
      val resources = new ResourcesType
      resources.setValue("/path/[^/]+/.* ALL ANY")
      configuration.setResources(resources)

      When("the configuration is updated")
      filter.configurationUpdated(configuration)

      Then("the filter's configuration should be modified")
      filter.isInitialized
      filter.configuration should not be config
      filter.configuration shouldBe configuration
      filter.configuration.getDelegating.getQuality shouldBe 1.0d
      filter.configuration.isMaskRaxRoles403
    }
    it("should log if the resources list has a bad line without enough elements") {
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
      val exception = intercept[UpdateFailedException] {
        filter.configurationUpdated(config)
      }

      Then("the filter's configuration should be modified")
      !filter.isInitialized
      exception.getLocalizedMessage should include("Malformed RBAC Resource")
      val events = listAppender.getEvents.toList.map(_.getMessage.getFormattedMessage)
      events.count(_.contains("Malformed RBAC Resource: /path/to/bad")) shouldBe 1
    }
    it("should log if the resources list has a bad line with to many elements") {
      Given("an un-initialized filter and a configuration with a malformed resource in the list")
      val ctx = LogManager.getContext(false).asInstanceOf[LoggerContext]
      val listAppender = ctx.getConfiguration.getAppender("List0").asInstanceOf[ListAppender].clear
      filter.configuration shouldBe null
      !filter.isInitialized
      val resources = new ResourcesType
      resources.setValue(
        """
          |/path/to/good  ALL       ANY
          |/path/to/bad   ALL       role with regular space
          | """.stripMargin.trim()
      )
      config.setResources(resources)

      When("the configuration is updated")
      val exception = intercept[UpdateFailedException] {
        filter.configurationUpdated(config)
      }

      Then("the filter's configuration should be modified")
      !filter.isInitialized
      exception.getLocalizedMessage should include("Malformed RBAC Resource")
      val events = listAppender.getEvents.toList.map(_.getMessage.getFormattedMessage)
      events.count(_.contains("Malformed RBAC Resource: /path/to/bad")) shouldBe 1
      events.count(_.contains("use a non-breaking space")) shouldBe 1
    }
    it("should load the file if the resources are external") {
      Given("an un-initialized filter and a configuration with the resources in a file")
      val ctx = LogManager.getContext(false).asInstanceOf[LoggerContext]
      val listAppender = ctx.getConfiguration.getAppender("List0").asInstanceOf[ListAppender].clear
      filter.configuration shouldBe null
      !filter.isInitialized
      val rbacFileName = "/rbac/test.rbac"
      val resources = new ResourcesType
      resources.setHref(rbacFileName)
      config.setResources(resources)
      val resourceResolver: ConfigurationResourceResolver = mock[ConfigurationResourceResolver]
      val configurationResource: ConfigurationResource = mock[ConfigurationResource]
      whenMock(configurationResource.newInputStream).thenReturn(this.getClass.getResourceAsStream(rbacFileName))
      whenMock(resourceResolver.resolve(rbacFileName)).thenReturn(configurationResource)
      whenMock(mockConfigService.getResourceResolver).thenReturn(resourceResolver)

      val exception = intercept[UpdateFailedException] {
        filter.configurationUpdated(config)
      }

      Then("the filter's configuration should be modified")
      !filter.isInitialized
      exception.getLocalizedMessage should include("Malformed RBAC Resource")
      val events = listAppender.getEvents.toList.map(_.getMessage.getFormattedMessage)
      events.count(_.contains("Malformed RBAC Resource: ")) shouldBe 1
    }
  }

  describe(s"when attempting to access a resource") {
    def shouldNot: Int => String = { int => if (int == SC_OK) "should" else "should not" }

    def withOut: Boolean => String = { boolean => if (boolean) "with" else "without" }

    def maskedNot: (Boolean, Int, Int) => Int = { (boolean, result, masked) => if (!boolean) result else masked }

    def statusRole: (Int, String) => String = { (status, role) => if (status == SC_OK) role else null }

    Seq("GET", "DELETE", "POST", "PUT", "PATCH", "HEAD", "OPTIONS", "CONNECT", "TRACE", "ANY", "ALL").foreach { method =>
      it(s"should support HTTP method $method") {
        Given(s"a request using HTTP method $method")
        val role = "role1"
        servletRequest.setRequestURI("/path/to/abc")
        servletRequest.setMethod(method)
        servletRequest.addHeader(ROLES, role)
        val resources = new ResourcesType
        resources.setValue(s"/path/[^/]+/.* some,$method,custom role1")
        config.setResources(resources)
        filter.configurationUpdated(config)

        When("the resource is requested")
        filter.doFilter(servletRequest, servletResponse, filterChain)

        Then("the request should be allowed access")
        servletResponse.getStatus shouldBe SC_OK
        filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(RELEVANT_ROLES) shouldBe role
      }
      Seq("/path/abc", "/TEST/TrailingSlash/", "/this/is/bad").foreach { path =>
        it(s"should not allow the request to $path since it is not in the access list. ($method)") {
          Given(s"a request using HTTP method $method")
          servletRequest.setRequestURI(path)
          servletRequest.setMethod(method)
          servletRequest.addHeader(ROLES, "role1")
          filter.configurationUpdated(config)

          When("the nonexistent resource is requested")
          filter.doFilter(servletRequest, servletResponse, filterChain)

          Then(s"the request should not be allowed access")
          servletResponse.getStatus shouldBe SC_NOT_FOUND
          filterChain.getRequest shouldBe null
        }
      }
      it(s"should not allow the request using a method not in the access list. ($method)") {
        Given(s"a request using HTTP method $method")
        servletRequest.setRequestURI("/path/to/good")
        servletRequest.setMethod(method)
        servletRequest.addHeader(ROLES, "role1")
        val resources = new ResourcesType
        resources.setValue("/path/[^/]+/.* Custom ANY")
        config.setResources(resources)
        filter.configurationUpdated(config)

        When("the nonexistent resource is requested")
        filter.doFilter(servletRequest, servletResponse, filterChain)

        Then(s"the request should not be allowed access")
        servletResponse.getStatus shouldBe SC_METHOD_NOT_ALLOWED
        filterChain.getRequest shouldBe null
      }
    }
    Seq(false, true).foreach { doMask =>
      Seq("/path/to/abc", "/path/to/xyz", "/path/to/abc/xyz", "/path/too/abc").foreach { path =>
        Seq(
          // @formatter:off
          //Method   Role     Result        Masked
          ("GET"   , "role1", SC_OK       , SC_OK),
          ("PUT"   , "role1", SC_OK       , SC_OK),
          ("POST"  , "role1", SC_OK       , SC_OK),
          ("DELETE", "role1", SC_OK       , SC_OK),
          ("GET"   , "role2", SC_OK       , SC_OK),
          ("PUT"   , "role2", SC_OK       , SC_OK),
          ("POST"  , "role2", SC_OK       , SC_OK),
          ("DELETE", "role2", SC_FORBIDDEN, SC_NOT_FOUND),
          ("GET"   , "role3", SC_OK       , SC_OK),
          ("PUT"   , "role3", SC_OK       , SC_OK),
          ("POST"  , "role3", SC_FORBIDDEN, SC_NOT_FOUND),
          ("DELETE", "role3", SC_FORBIDDEN, SC_NOT_FOUND),
          ("GET"   , "role4", SC_OK       , SC_OK),
          ("PUT"   , "role4", SC_FORBIDDEN, SC_NOT_FOUND),
          ("POST"  , "role4", SC_FORBIDDEN, SC_NOT_FOUND),
          ("DELETE", "role4", SC_FORBIDDEN, SC_NOT_FOUND),
          ("GET"   , "role5", SC_FORBIDDEN, SC_NOT_FOUND),
          ("PUT"   , "role5", SC_FORBIDDEN, SC_NOT_FOUND),
          ("POST"  , "role5", SC_FORBIDDEN, SC_NOT_FOUND),
          ("DELETE", "role5", SC_FORBIDDEN, SC_NOT_FOUND)
          // @formatter:on
        ).foreach { case (method, role, result, masked) =>
          it(s"${shouldNot(result)} allow the request to $path when using HTTP method $method and having role $role, ${withOut(doMask)} masking enabled.") {
            Given(s"a request using HTTP method $method")
            servletRequest.setRequestURI(path)
            servletRequest.setMethod(method)
            servletRequest.addHeader(ROLES, role)
            config.setMaskRaxRoles403(doMask)
            filter.configurationUpdated(config)

            When("the protected resource is requested")
            filter.doFilter(servletRequest, servletResponse, filterChain)

            Then(s"the request should not be allowed access")
            val status = maskedNot(doMask, result, masked)
            servletResponse.getStatus shouldBe status
            if (status == SC_OK) {
              assert(filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(RELEVANT_ROLES).equals(role))
            } else {
              assert(filterChain.getRequest == null)
            }
          }
        }
      }
    }
    it("should allow roles with spaces.") {
      Given(s"a request with roles with spaces")
      val role = "role 1"
      servletRequest.setRequestURI("/path/to/this")
      servletRequest.setMethod("GET")
      servletRequest.addHeader(ROLES, role)
      val resources = new ResourcesType
      resources.setValue("/path/[^/]+/.* GET role\u00A01")
      config.setResources(resources)
      filter.configurationUpdated(config)

      When("the protected resource is requested")
      filter.doFilter(servletRequest, servletResponse, filterChain)

      Then(s"the request should not be allowed access")
      servletResponse.getStatus shouldBe SC_OK
      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(RELEVANT_ROLES) shouldBe role
    }
    it("should delegate an unauthorized request when configured") {
      Given(s"a bad request")
      servletRequest.setRequestURI("/path/to/this")
      servletRequest.setMethod("GET")
      servletRequest.addHeader(ROLES, "bad")
      val resources = new ResourcesType
      resources.setValue("/path/[^/]+/.* get role1")
      config.setResources(resources)
      val delegatingType = new DelegatingType
      config.setDelegating(delegatingType)
      filter.configurationUpdated(config)

      When("the protected resource is requested")
      filter.doFilter(servletRequest, servletResponse, filterChain)

      Then(s"the request should not be allowed access")
      val delegationValue = filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(HttpDelegationHeaderNames.Delegated)
      delegationValue should not be null
      val delegationHeader = parseDelegationHeader(delegationValue)
      delegationHeader shouldBe a[Success[_]]
      delegationHeader.get.statusCode shouldBe SC_FORBIDDEN
      filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(RELEVANT_ROLES) shouldBe null
    }
    it("should allow configuration of the delegated unauthorized request") {
      Given(s"a bad request")
      servletRequest.setRequestURI("/path/to/this")
      servletRequest.setMethod("GET")
      servletRequest.addHeader(ROLES, "bad")
      val resources = new ResourcesType
      resources.setValue("/path/[^/]+/.* get role1")
      config.setResources(resources)
      val delegatingType = new DelegatingType
      val componentName = "November Lima"
      delegatingType.setComponentName(componentName)
      val quality = 0.1331D
      delegatingType.setQuality(quality)
      config.setDelegating(delegatingType)
      filter.configurationUpdated(config)

      When("the protected resource is requested")
      filter.doFilter(servletRequest, servletResponse, filterChain)

      Then(s"the request should not be allowed access")
      val delegationValue = filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(HttpDelegationHeaderNames.Delegated)
      delegationValue should not be null
      val delegationHeader = parseDelegationHeader(delegationValue)
      delegationHeader shouldBe a[Success[_]]
      delegationHeader.get.statusCode shouldBe SC_FORBIDDEN
      delegationHeader.get.component shouldBe componentName
      delegationHeader.get.quality shouldBe quality
    }
    Seq("/path/To/it", "/path/to/it").foreach { path =>
      Seq(
        // @formatter:off
        //Role          Result
        ("role1,role2", SC_OK),
        ("role1"      , SC_FORBIDDEN),
        ("role2"      , SC_FORBIDDEN)
        // @formatter:on
      ).foreach { case (roles, result) =>
        it(s"${shouldNot(result)} allow the request to $path with roles '$roles'.") {
          Given(s"a request to $path with roles '$roles'")
          servletRequest.setRequestURI(path)
          servletRequest.setMethod("GET")
          servletRequest.addHeader(ROLES, roles)
          val resources = new ResourcesType
          resources.setValue(
            """/path/[^/]+/.*     get role1
              |/path/[Tt][^/]+/.* GET role2
              |""".stripMargin.trim())
          config.setResources(resources)
          filter.configurationUpdated(config)

          When("the protected resource is requested")
          filter.doFilter(servletRequest, servletResponse, filterChain)

          Then(s"the request should not be allowed access")
          servletResponse.getStatus shouldBe result
          if (result == SC_OK) {
            assert(filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(RELEVANT_ROLES).contains("role1"))
            assert(filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader(RELEVANT_ROLES).contains("role2"))
          } else {
            assert(filterChain.getRequest == null)
          }
        }
      }
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
      verify(mockConfigService).unsubscribeFrom(eql("another-name.cfg.xml"), same(filter))
    }
  }
}
