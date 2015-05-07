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
package org.openrepose.filters.valkyrieauthorization

import com.mockrunner.mock.web.{MockHttpServletResponse, MockFilterChain, MockHttpServletRequest}
import com.rackspace.httpdelegation.HttpDelegationManager
import org.junit.runner.RunWith
import org.openrepose.filters.scripting.config.{ScriptData, ScriptingConfig}
import org.openrepose.lols.ScriptingFilter
import org.scalatest.{BeforeAndAfterAll, Matchers, FunSpec}
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ScriptingFilterTest extends FunSpec with HttpDelegationManager with Matchers with BeforeAndAfterAll {

  override def beforeAll() {
    System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
      "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl")
  }


  it("can parse some jruby to add a header with static value") {
    val fakeConfigService = new FakeConfigService()
    val filter = new ScriptingFilter(fakeConfigService)

    val scriptingConfig = new ScriptingConfig()

    val scriptData = new ScriptData()
    scriptData.setValue(
      """
        |puts("lol")
        |$request.addHeader("lol", "butts")
      """.stripMargin)
    scriptData.setLanguage("jruby")
    scriptingConfig.getRequestScript.add(scriptData)

    filter.configurationUpdated(scriptingConfig)

    val request = new MockHttpServletRequest()

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain())
    request.getHeader("lol") should equal("butts")
  }

  it("can parse some jython to add a header with static value") {
    val fakeConfigService = new FakeConfigService()
    val filter = new ScriptingFilter(fakeConfigService)

    val scriptingConfig = new ScriptingConfig()

    val scriptData = new ScriptData()
    scriptData.setValue(
      """
        |print "jython fails silently in ways that make me sad"
        |request.addHeader("lol", "butts")
      """.stripMargin)
    scriptData.setLanguage("python")
    scriptingConfig.getRequestScript.add(scriptData)

    filter.configurationUpdated(scriptingConfig)

    filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain())
  }

}
