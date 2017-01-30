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
package org.openrepose.core.filter

import java.net.URL
import javax.servlet._
import javax.servlet.http.HttpServletResponse

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.services.config.ConfigurationService

import scala.reflect.{ClassTag, _}

/**
  * An abstract class for the easy construction of repose filters that take a configuration file.
  *
  * @param configurationService
  * @tparam T the config class
  */
abstract class AbstractConfiguredFilter[T: ClassTag](val configurationService: ConfigurationService)
  extends Filter with LazyLogging with UpdateListener[T] {

  private var configFile: String = _

  /**
    * The default configuration file name for the filter.
    */
  val DEFAULT_CONFIG: String

  /**
    * The location of the schema file describing the xml config.
    */
  val SCHEMA_LOCATION: String

  /**
    * The configuration most recently received by the configurationUpdated method.
    */
  var configuration: T = _
  var initialized: Boolean = false

  /**
    * Subscribes with the configuration service. If the filter config doesn't have a custom file name,
    * it uses the name provided by DEFAULT_CONFIG. It tries to load and use the schema provided by SCHEMA_LOCATION.
    *
    * @param filterConfig
    */
  override def init(filterConfig: FilterConfig): Unit = {
    logger.trace("{} initializing ...", this.getClass.getSimpleName)
    configFile = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)

    logger.info("Initializing filter using config {}", configFile)
    val xsdURL: URL = getClass.getResource(SCHEMA_LOCATION)
    configurationService.subscribeTo(
      filterConfig.getFilterName,
      configFile,
      xsdURL,
      this,
      classTag[T].runtimeClass.asInstanceOf[Class[T]]
    )

    doInit(filterConfig)

    logger.trace("{} initialized.", this.getClass.getSimpleName)
  }

  /**
    * Called immediately after the [[configFile]] is subscribed to.
    * Should be overridden when additional processing needs to be performed on initialization.
    */
  def doInit(filterConfig: FilterConfig): Unit = {
    logger.trace("{} default doInit ...", this.getClass.getSimpleName)
  }

  /**
    * Stores the configuration and marks the filter as initialized.
    *
    * @param configurationObject
    */
  override def configurationUpdated(configurationObject: T): Unit = {
    logger.trace("{} received a configuration update", this.getClass.getSimpleName)
    doConfigurationUpdated(configurationObject)
    configuration = configurationObject
    initialized = true
  }

  /**
    * Called before the configuration reference is updated.
    *
    * @param newConfiguration
    */
  def doConfigurationUpdated(newConfiguration: T): Unit = {
    logger.trace("{} default doConfigurationUpdated ...", this.getClass.getSimpleName)
  }


  /**
    * Returns true once configurationUpdated successfully completes.
    *
    * @return
    */
  override def isInitialized: Boolean = initialized

  /**
    * Does an initialization check. Will return 500 if not yet initialized, otherwise calls through to doWork.
    *
    * @param request
    * @param response
    * @param chain
    */
  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    if (!initialized) {
      logger.error("{} has not yet initialized...", this.getClass.getSimpleName)
      response.asInstanceOf[HttpServletResponse].sendError(500, "Filter not initialized")
    } else {
      logger.trace("{} processing request...", this.getClass.getSimpleName)
      doWork(request, response, chain)
      logger.trace("{} returning response...", this.getClass.getSimpleName)
    }
  }

  /**
    * Where the concrete class does it's work. This method is the equivalent doFilter in a normal filter.
    *
    * @param request
    * @param response
    * @param chain
    */
  def doWork(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit

  /**
    * Un-subscribes from the configuration service.
    */
  override def destroy(): Unit = {
    logger.trace("{} destroying ...", this.getClass.getSimpleName)
    configurationService.unsubscribeFrom(configFile, this)
    doDestroy()
    logger.trace("{} destroyed.", this.getClass.getSimpleName)
  }

  /**
    * Called immediately after the [[configFile]] is un-subscribed from.
    * Should be overridden when additional processing needs to be performed on destruction.
    */
  def doDestroy(): Unit = {
    logger.trace("{} default doDestroy ...", this.getClass.getSimpleName)
  }
}
