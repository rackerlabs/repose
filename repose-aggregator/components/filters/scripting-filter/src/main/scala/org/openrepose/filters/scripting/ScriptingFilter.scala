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
import javax.inject.{Inject, Named}
import javax.script._
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.rackspace.httpdelegation.HttpDelegationManager
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.{UpdateFailedException, UpdateListener}
import org.openrepose.commons.utils.servlet.http._
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.scripting.config.ScriptingConfig
import org.python.core.Options

import scala.collection.JavaConversions._

@Named
class ScriptingFilter @Inject()(configurationService: ConfigurationService)
  extends Filter with UpdateListener[ScriptingConfig] with HttpDelegationManager with LazyLogging {

  // Necessary for Jython, doesn't work with JSR223 without, fails silently!
  Options.importSite = false

  private final val DEFAULT_CONFIG = "scripting.cfg.xml"

  private var configurationFile: String = DEFAULT_CONFIG
  private var initialized: Boolean = false
  private var requestScriptRunners: Iterable[ScriptRunner] = Iterable.empty
  private var responseScriptRunners: Iterable[ScriptRunner] = Iterable.empty

  override def init(filterConfig: FilterConfig): Unit = {
    configurationFile = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)
    logger.info("Initializing filter using config " + configurationFile)
    val xsdURL: URL = getClass.getResource("/META-INF/schema/config/scripting.xsd")
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
    logger.debug("Wrapping servlet request and response")
    val wrappedRequest = new HttpServletRequestWrapper(servletRequest.asInstanceOf[HttpServletRequest])
    val wrappedResponse = new HttpServletResponseWrapper(servletResponse.asInstanceOf[HttpServletResponse], ResponseMode.MUTABLE, ResponseMode.MUTABLE)
    val bindings = new SimpleBindings(Map(
      "request" -> wrappedRequest,
      "response" -> wrappedResponse
    ))

    logger.debug("Running request scripts")
    requestScriptRunners foreach { runner =>
      logger.debug("Running script")
      runner.run(bindings)
    }

    logger.debug("Calling next filter")
    filterChain.doFilter(wrappedRequest, wrappedResponse)

    logger.debug("Running response scripts")
    responseScriptRunners foreach { runner =>
      logger.debug("Running script")
      runner.run(bindings)
    }
  }

  override def configurationUpdated(configurationObject: ScriptingConfig): Unit = {
    val scriptEngineManager = new ScriptEngineManager()

    requestScriptRunners = configurationObject.getRequestScript map { requestScript =>
      Option(scriptEngineManager.getEngineByName(requestScript.getLanguage)) match {
        case Some(engine) =>
          ScriptRunner(requestScript.getValue, engine)
        case None =>
          logger.error(requestScript.getLanguage + " is not a supported language")
          throw new UpdateFailedException(requestScript.getLanguage + " is not a supported language", null)
      }
    }

    responseScriptRunners = configurationObject.getResponseScript map { responseScript =>
      Option(scriptEngineManager.getEngineByName(responseScript.getLanguage)) match {
        case Some(engine) =>
          ScriptRunner(responseScript.getValue, engine)
        case None =>
          logger.error(responseScript.getLanguage + " is not a supported language")
          throw new UpdateFailedException(responseScript.getLanguage + " is not a supported language", null)
      }
    }

    initialized = true
  }

  override def isInitialized: Boolean = initialized

  private sealed trait ScriptRunner {
    def run(bindings: Bindings): AnyRef
  }

  private class CompiledScriptRunner(compiledScript: CompiledScript) extends ScriptRunner {
    override def run(bindings: Bindings): AnyRef = {
      compiledScript.eval(bindings)
    }
  }

  private class StringScriptRunner(script: String, scriptEngine: ScriptEngine) extends ScriptRunner {
    override def run(bindings: Bindings): AnyRef = {
      scriptEngine.eval(script, bindings)
    }
  }

  private object ScriptRunner {
    def apply(script: String, engine: ScriptEngine): ScriptRunner = {
      engine match {
        case c: Compilable =>
          logger.debug("Creating compiled script runner")
          new CompiledScriptRunner(c.compile(script))
        case e =>
          logger.debug("Creating string script runner")
          new StringScriptRunner(script, e)
      }
    }
  }

}
