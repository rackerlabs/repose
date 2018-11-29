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

import java.io.File
import java.net.{URL, URLClassLoader}

import com.typesafe.scalalogging.slf4j.StrictLogging
import javax.inject.{Inject, Named}
import javax.script._
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import kotlin.script.experimental.jvm.util.JvmClasspathUtilKt.KOTLIN_SCRIPT_CLASSPATH_PROPERTY
import org.openrepose.commons.config.manager.{UpdateFailedException, UpdateListener}
import org.openrepose.commons.utils.servlet.http._
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.scripting.ScriptingFilter._
import org.openrepose.filters.scripting.config.ScriptingConfig
import org.python.core.Options

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

@Named
class ScriptingFilter @Inject()(configurationService: ConfigurationService)
  extends Filter with UpdateListener[ScriptingConfig] with StrictLogging {

  // Necessary for Jython, doesn't work with JSR223 without, fails silently!
  Options.importSite = false

  // Necessary for Kotlin to load classes during compilation, including the classes of our bindings
  // Bindings must be cast to the appropriate type at runtime to be usable
  System.setProperty(
    KOTLIN_SCRIPT_CLASSPATH_PROPERTY,
    classpathFromClassLoader(getClass.getClassLoader).map(_.getFile).distinct.mkString(File.pathSeparator)
  )

  private final val DefaultConfig = "scripting.cfg.xml"

  private var initialized: Boolean = false
  private var configurationFile: String = _
  private var scriptRunner: ScriptRunner = _

  override def init(filterConfig: FilterConfig): Unit = {
    configurationFile = new FilterConfigHelper(filterConfig).getFilterConfig(DefaultConfig)
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
    if (!isInitialized) {
      logger.error("Filter has not yet initialized... Please check your configuration files and your artifacts directory.")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
    } else {
      val wrappedRequest = new HttpServletRequestWrapper(servletRequest.asInstanceOf[HttpServletRequest])
      val wrappedResponse = new HttpServletResponseWrapper(servletResponse.asInstanceOf[HttpServletResponse],
        ResponseMode.MUTABLE,
        ResponseMode.MUTABLE)

      val bindings = new SimpleBindings()
      bindings.put("request", wrappedRequest)
      bindings.put("response", wrappedResponse)
      bindings.put("filterChain", filterChain)

      logger.debug("Running script")
      Try(scriptRunner.run(bindings)) match {
        case Success(_) =>
          logger.debug("Script Successful")
          wrappedResponse.commitToResponse()
        case Failure(e) =>
          logger.error("Processing failure -- the script threw an Exception", e)
          servletResponse.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
      }
    }
  }

  override def isInitialized: Boolean = initialized

  override def configurationUpdated(configurationObject: ScriptingConfig): Unit = {
    val scriptEngineManager = new ScriptEngineManager(getClass.getClassLoader)

    scriptRunner = Option(scriptEngineManager.getEngineByName(configurationObject.getLanguage.value)) match {
      case Some(engine) =>
        ScriptRunner(configurationObject.getValue, engine)
      case None =>
        logger.error(configurationObject.getLanguage + " is not a supported language")
        throw new UpdateFailedException(configurationObject.getLanguage + " is not a supported language", null)
    }

    initialized = true
  }

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

object ScriptingFilter {
  // A helper method to construct a classpath which includes all paths accessible to a provided ClassLoader.
  //
  // This is a somewhat questionable approach since it flattens the classloading hierarchy, which may result
  // in collisions that would not normally occur.
  // To alleviate this concern in some small way, paths are ordered with paths from any ClassLoader always
  // coming before paths from any ancestors of that ClassLoader.
  //
  // However, this is necessary to support the Kotlin ScriptEngine, which constructs a new ClassLoader from a given
  // classpath.
  // It does this since it is not provided a ClassLoader by the ScriptEngineManager, but still needs to load certain
  // classes to perform compilation of scripts.
  // To enable the classpath used for script compilation to be customized, the Kotlin ScriptEngineFactory will use
  // the value of a named system property as the classpath if defined.
  //
  // An alternative approach would be to provide a ScriptEngineFactory for Kotlin that would determine the classpath
  // based on a provided ClassLoader (i.e., duplicate that KotlinJsr223JvmLocalScriptEngineFactory class, but pass
  // the ClassLoader for this class to the scriptCompilationClasspathFromContextOrStlib function call), but that
  // requires bridging Scala and Kotlin code.
  @tailrec
  private def classpathFromClassLoader(classLoader: ClassLoader, urls: Seq[URL] = Seq.empty): Seq[URL] = {
    Option(classLoader.getParent) match {
      case None if classLoader.isInstanceOf[URLClassLoader] =>
        urls ++ classLoader.asInstanceOf[URLClassLoader].getURLs
      case None =>
        urls
      case Some(parentLoader) if classLoader.isInstanceOf[URLClassLoader] =>
        classpathFromClassLoader(parentLoader, urls ++ classLoader.asInstanceOf[URLClassLoader].getURLs)
      case Some(parentLoader) =>
        classpathFromClassLoader(parentLoader, urls)
    }
  }
}
