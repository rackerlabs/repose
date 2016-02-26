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

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import org.junit.runner.RunWith
import org.openrepose.filters.scripting.config.{ScriptData, ScriptingConfig}
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}
import org.springframework.mock.web.{MockFilterConfig, MockFilterChain, MockHttpServletRequest, MockHttpServletResponse}

@RunWith(classOf[JUnitRunner])
class ScriptingFilterTest extends FunSpec with Matchers {

  System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
    "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl")

  it("should return a 503 if the filter has not yet initialized") {
    val filter = new ScriptingFilter(null)
    val response = new MockHttpServletResponse()

    filter.doFilter(null, response, null)

    response.getStatus shouldBe HttpServletResponse.SC_SERVICE_UNAVAILABLE
  }

  it("should register to a configuration on init") {
    pending
  }

  it("should unregister from a configuration on destroy") {
    pending
  }

  it("can parse some javascript to add a header with static value") {
    val fakeConfigService = new FakeConfigService()
    val filter = new ScriptingFilter(fakeConfigService)
    val filterChain = new MockFilterChain()

    val scriptingConfig = new ScriptingConfig()

    val scriptData = new ScriptData()
    scriptData.setValue(
      """
        |request.addHeader("lol", "butts")
        |filterChain.doFilter(request, response)
      """.stripMargin)
    scriptData.setLanguage("javascript")
    scriptingConfig.setScript(scriptData)

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

    val scriptData = new ScriptData()
    scriptData.setValue(
      """
        |$request.addHeader("lol", "butts")
        |$filterChain.doFilter($request, $response)
      """.stripMargin)
    scriptData.setLanguage("jruby")
    scriptingConfig.setScript(scriptData)

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

    val scriptData = new ScriptData()
    scriptData.setValue(
      """
        |request.addHeader("lol", "butts")
        |filterChain.doFilter(request, response)
      """.stripMargin)
    scriptData.setLanguage("python")
    scriptingConfig.setScript(scriptData)

    filter.configurationUpdated(scriptingConfig)

    filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), filterChain)
    filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader("lol") should equal("butts")
  }

  it("can parse some lua to add a header with static value") {
    val fakeConfigService = new FakeConfigService()
    val filter = new ScriptingFilter(fakeConfigService)
    val filterChain = new MockFilterChain()

    val scriptingConfig = new ScriptingConfig()

    val scriptData = new ScriptData()
    scriptData.setValue(
      """
        |request:addHeader("lol", "butts")
        |filterChain:doFilter(request, response)
      """.stripMargin)
    scriptData.setLanguage("lua")
    scriptingConfig.setScript(scriptData)

    filter.configurationUpdated(scriptingConfig)

    filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), filterChain)
    filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader("lol") should equal("butts")
  }

  ignore("can parse some scala to add a header with static value") {
    val fakeConfigService = new FakeConfigService()
    val filter = new ScriptingFilter(fakeConfigService)
    val filterChain = new MockFilterChain()

    val scriptingConfig = new ScriptingConfig()

    val scriptData = new ScriptData()
    scriptData.setValue(
      """
        |request.addHeader("lol", "butts")
        |filterChain.doFilter(request, response)
      """.stripMargin)
    scriptData.setLanguage("scala")
    scriptingConfig.setScript(scriptData)

    filter.configurationUpdated(scriptingConfig)

    filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), filterChain)
    filterChain.getRequest.asInstanceOf[HttpServletRequest].getHeader("lol") should equal("butts")
  }
}
