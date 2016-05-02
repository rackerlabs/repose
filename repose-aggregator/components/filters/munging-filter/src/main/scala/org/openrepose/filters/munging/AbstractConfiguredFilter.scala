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
package org.openrepose.filters.munging

import java.net.URL
import javax.servlet._

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService

import scala.reflect.ClassTag
import scala.reflect._

/**
  * Created by adrian on 4/29/16.
  */
abstract class AbstractConfiguredFilter[T: ClassTag](val configurationService: ConfigurationService)
  extends Filter with LazyLogging with UpdateListener[T] {

  val DEFAULT_CONFIG: String
  val SCHEMA_LOCATION: String

  private var configFile: String = _
  var configuration: T = _
  var initialized: Boolean = false

  override def init(filterConfig: FilterConfig): Unit = {
    logger.trace("{} initializing ...", this.getClass.getSimpleName)
    configFile = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG)

    logger.info("Initializing filter using config " + configFile)
    val xsdURL: URL = getClass.getResource(SCHEMA_LOCATION)
    configurationService.subscribeTo(
      filterConfig.getFilterName,
      configFile,
      xsdURL,
      this,
      classTag[T].runtimeClass.asInstanceOf[Class[T]]
    )

    logger.trace("{} initialized.", this.getClass.getSimpleName)
  }

  override def destroy(): Unit = {
    logger.trace("{} destroying ...")
    configurationService.unsubscribeFrom(configFile, this)
    logger.trace("{} destroyed.")
  }

  override def configurationUpdated(configurationObject: T): Unit = {
    logger.trace("{} received a configuration update", this.getClass.getSimpleName)
    configuration = configurationObject

    initialized = true
  }

  override def isInitialized: Boolean = initialized
}
