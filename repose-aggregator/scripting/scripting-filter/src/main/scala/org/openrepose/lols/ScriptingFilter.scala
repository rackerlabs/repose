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
package org.openrepose.lols

import java.net.URL
import javax.inject.{Inject, Named}
import javax.script.ScriptEngineManager
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.rackspace.httpdelegation.HttpDelegationManager
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.servlet.http.{MutableHttpServletRequest, MutableHttpServletResponse}
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.scripting.config.{ScriptData, ScriptingConfig}
import org.python.core.Options

import scala.collection.JavaConversions._

@Named
class ScriptingFilter @Inject()(configurationService: ConfigurationService)
  extends Filter with UpdateListener[ScriptingConfig] with HttpDelegationManager with LazyLogging {

  // Necessary for Jython, doesn't work with JSR223 without, fails silently!
  Options.importSite = false

  private final val DEFAULT_CONFIG = "scripting.cfg.xml"

  var configurationFile: String = DEFAULT_CONFIG
  var configuration: ScriptingConfig = _
  var initialized: Boolean = false

  override def init(filterConfig: FilterConfig): Unit = {
    configurationFile = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    logger.info("Initializing filter using config " + configurationFile)
    val xsdURL: URL = getClass.getResource("/META-INF/schema/config/scripting-filter.xsd")
    configurationService.subscribeTo(
      filterConfig.getFilterName,
      configurationFile,
      xsdURL,
      this,
      classOf[ScriptingConfig])
  }

  override def destroy(): Unit = {
    configurationService.unsubscribeFrom(configurationFile, this)
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    logger.debug("Before wrapping thingers")
    val mutableHttpRequest = MutableHttpServletRequest.wrap(servletRequest.asInstanceOf[HttpServletRequest])
    val mutableHttpResponse = MutableHttpServletResponse.wrap(mutableHttpRequest, servletResponse.asInstanceOf[HttpServletResponse])

    logger.debug("Creating new ScriptEngineManager")
    val manager = new ScriptEngineManager()

    logger.debug("Running scripts")
    configuration.getRequestScript.foreach { script: ScriptData =>
      logger.debug(s"Getting engine for ${script.getLanguage}")
      val engine = manager.getEngineByName(script.getLanguage)

      logger.debug(s"ENGINE IS $engine")
      logger.debug("Setting request in global context")
      engine.put("request", mutableHttpRequest)

      logger.debug("Evaluating script!")
      engine.eval(script.getValue)
    }

    filterChain.doFilter(mutableHttpRequest, mutableHttpResponse)
  }

  override def configurationUpdated(configurationObject: ScriptingConfig): Unit = {
    configuration = configurationObject
    initialized = true
  }

  override def isInitialized: Boolean = initialized
}
