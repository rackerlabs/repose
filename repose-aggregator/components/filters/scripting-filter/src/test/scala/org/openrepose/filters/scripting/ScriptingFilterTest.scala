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
package org.openrepose.filters.scripting

import java.net.URL
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.junit.runner.RunWith
import org.mockito.Matchers.{any, anyString, same}
import org.mockito.Mockito.verify
import org.openrepose.commons.config.manager.UpdateFailedException
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.scripting.config.ScriptingConfig
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import org.springframework.mock.web.{MockFilterChain, MockFilterConfig, MockHttpServletRequest, MockHttpServletResponse}

@RunWith(classOf[JUnitRunner])
class ScriptingFilterTest extends FunSpec with Matchers with MockitoSugar {

  System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
    "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl")

  it("should return a 503 if the filter has not yet initialized") {
    val filter = new ScriptingFilter(null)
    val response = new MockHttpServletResponse()

    filter.doFilter(null, response, null)

    response.getStatus shouldBe HttpServletResponse.SC_SERVICE_UNAVAILABLE
  }

  it("should register to a configuration on init") {
    val configurationService = mock[ConfigurationService]
    val filter = new ScriptingFilter(configurationService)

    filter.init(new MockFilterConfig())

    verify(configurationService).subscribeTo(anyString(), anyString(), any[URL], same(filter), any[Class[ScriptingConfig]])
  }

  it("should unregister from a configuration on destroy") {
    val configurationService = mock[ConfigurationService]
    val filter = new ScriptingFilter(configurationService)

    filter.destroy()

    verify(configurationService).unsubscribeFrom(anyString(), same(filter))
  }

  it("should throw an exception if the configuration is updated with an unsupported language") {
    val filter = new ScriptingFilter(null)

    val scriptingConfig = new ScriptingConfig()
    scriptingConfig.setValue(
      """
        |request.addHeader("lol", "butts")
        |filterChain.doFilter(request, response)
      """.stripMargin)
    scriptingConfig.setLanguage("foo")

    an[UpdateFailedException] should be thrownBy filter.configurationUpdated(scriptingConfig)
  }

  it("can parse some javascript to add a header with static value") {
    val fakeConfigService = new FakeConfigService()
    val filter = new ScriptingFilter(fakeConfigService)
    val filterChain = new MockFilterChain()

    val scriptingConfig = new ScriptingConfig()
    scriptingConfig.setValue(
      """
        |request.addHeader("lol", "butts")
        |filterChain.doFilter(request, response)
      """.stripMargin)
    scriptingConfig.setLanguage("javascript")

    filter.configurationUpdated(scriptingConfig)

    val request = new MockHttpServletRequest()

    filter.doFilter(request, new MockHttpServletResponse(), filterChain)
    filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader("lol") should equal("butts")
  }

  it("can parse some jruby to add a header with static value") {
    val fakeConfigService = new FakeConfigService()
    val filter = new ScriptingFilter(fakeConfigService)
    val filterChain = new MockFilterChain()

    val scriptingConfig = new ScriptingConfig()
    scriptingConfig.setValue(
      """
        |$request.addHeader("lol", "butts")
        |$filterChain.doFilter($request, $response)
      """.stripMargin)
    scriptingConfig.setLanguage("jruby")

    filter.configurationUpdated(scriptingConfig)

    val request = new MockHttpServletRequest()

    filter.doFilter(request, new MockHttpServletResponse(), filterChain)
    filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader("lol") should equal("butts")
  }

  it("can parse some jython to add a header with static value") {
    val fakeConfigService = new FakeConfigService()
    val filter = new ScriptingFilter(fakeConfigService)
    val filterChain = new MockFilterChain()

    val scriptingConfig = new ScriptingConfig()
    scriptingConfig.setValue(
      """
        |request.addHeader("lol", "butts")
        |filterChain.doFilter(request, response)
      """.stripMargin)
    scriptingConfig.setLanguage("python")

    filter.configurationUpdated(scriptingConfig)

    filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), filterChain)
    filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader("lol") should equal("butts")
  }

  it("can parse some lua to add a header with static value") {
    val fakeConfigService = new FakeConfigService()
    val filter = new ScriptingFilter(fakeConfigService)
    val filterChain = new MockFilterChain()

    val scriptingConfig = new ScriptingConfig()
    scriptingConfig.setValue(
      """
        |request:addHeader("lol", "butts")
        |filterChain:doFilter(request, response)
      """.stripMargin)
    scriptingConfig.setLanguage("lua")

    filter.configurationUpdated(scriptingConfig)

    filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), filterChain)
    filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader("lol") should equal("butts")
  }

  ignore("can parse some scala to add a header with static value") {
    val fakeConfigService = new FakeConfigService()
    val filter = new ScriptingFilter(fakeConfigService)
    val filterChain = new MockFilterChain()

    val scriptingConfig = new ScriptingConfig()
    scriptingConfig.setValue(
      """
        |request.addHeader("lol", "butts")
        |filterChain.doFilter(request, response)
      """.stripMargin)
    scriptingConfig.setLanguage("scala")

    filter.configurationUpdated(scriptingConfig)

    filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), filterChain)
    filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader("lol") should equal("butts")
  }

  it("should return a 500 if the script throws an exception") {
    val filter = new ScriptingFilter(null)
    val filterChain = new MockFilterChain()

    val scriptingConfig = new ScriptingConfig()
    scriptingConfig.setValue(
      """
        |throw "EXCEPTION"
      """.stripMargin)
    scriptingConfig.setLanguage("javascript")

    filter.configurationUpdated(scriptingConfig)

    val request = new MockHttpServletRequest()
    val response = new MockHttpServletResponse()

    filter.doFilter(request, response, filterChain)

    filterChain.getRequest shouldBe null
    response.getStatus shouldBe HttpServletResponse.SC_INTERNAL_SERVER_ERROR
  }
}
