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
package org.openrepose.core.services.phonehome.impl

import javax.annotation.PostConstruct
import javax.inject.{Inject, Named}

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.spring.ReposeSpringProperties
import org.openrepose.core.systemmodel.{PhoneHomeService => PhoneHomeServiceConfig, SystemModel}
import org.springframework.beans.factory.annotation.Value

@Named
class PhoneHomeServiceImpl @Inject()(@Value(ReposeSpringProperties.CORE.REPOSE_VERSION) reposeVer: String,
                                     @Value(ReposeSpringProperties.CORE.CONFIG_ROOT) confDir: String,
                                     configurationService: ConfigurationService)
  extends PhoneHomeService with LazyLogging {

  private var configuration: Option[PhoneHomeServiceConfig] = None

  @PostConstruct
  def init(): Unit = {
    logger.debug("Registering system model listener")
    configurationService.subscribeTo(
      "system-model.cfg.xml",
      SystemModelConfigurationListener,
      classOf[SystemModel]
    )
  }

  override def isActive: Boolean = {
    logger.trace("isActive method called")

    ifInitialized {
      configuration match {
        case Some(_) => true
        case None => false
      }
    }
  }

  override def sendUpdate(): Unit = {
    logger.trace("sendUpdate method called")

    ifInitialized {
      configuration match {
        case Some(configurationReference) =>
          logger.debug("Sending usage data update to data collection point")
          ???
        case None =>
          logger.trace("Could not send an update; the phone home service is not active")
          throw new IllegalStateException("Could not send an update; the phone home service is not active")
      }
    }
  }

  private def ifInitialized[T](f: => T): T = {
    if (SystemModelConfigurationListener.isInitialized) {
      f
    } else {
      logger.error("The phone home service has not yet initialized")
      throw new IllegalStateException("The phone home service has not yet initialized")
    }
  }

  object SystemModelConfigurationListener extends UpdateListener[SystemModel] {
    private var initialized = false

    override def configurationUpdated(configurationObject: SystemModel): Unit = {
      configuration = Option(configurationObject.getPhoneHome)

      initialized = true
    }

    override def isInitialized: Boolean = initialized
  }

}