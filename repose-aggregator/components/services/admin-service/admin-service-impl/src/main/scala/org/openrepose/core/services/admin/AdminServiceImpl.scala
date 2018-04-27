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
package org.openrepose.core.services.admin

import com.typesafe.scalalogging.slf4j.LazyLogging
import javax.annotation.PostConstruct
import javax.inject.{Inject, Named}
import org.openrepose.adminservice.AdminServiceConfiguration
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.services.admin.config.AdminServiceConfigType
import org.openrepose.core.services.config.ConfigurationService
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.{ApplicationContext, ConfigurableApplicationContext}

/**
  * Created by adrian on 9/12/17.
  */
@Named
class AdminServiceImpl @Inject()(configurationService: ConfigurationService, applicationContext: ApplicationContext)
  extends AdminService
    with LazyLogging {

  var adminContext: Option[ConfigurableApplicationContext] = None

  @PostConstruct
  def init(): Unit = {
    logger.error("Initializing and registering configuration listeners")
    val xsdURL = getClass.getResource("/META-INF/schema/config/admin-service.xsd")

    configurationService.subscribeTo(
      AdminServiceImpl.ConfigName,
      xsdURL,
      AdminServiceConfigurationListener,
      classOf[AdminServiceConfigType]
    )
  }

  private object AdminServiceConfigurationListener extends UpdateListener[AdminServiceConfigType] {
    import scala.collection.JavaConverters._

    override def configurationUpdated(configuration: AdminServiceConfigType): Unit = {
      adminContext match {
        case Some(context) =>
          context.close()
          adminContext = Option(buildContext())
        case None =>
          adminContext = Option(buildContext())
      }

      def buildContext(): ConfigurableApplicationContext = {
        logger.error("trying to build the context")
        val properties = Map("server.port" -> configuration.getPort).asJava.asInstanceOf[java.util.Map[String, AnyRef]]
        val appBuilder = new SpringApplicationBuilder()
          .parent(applicationContext.asInstanceOf[ConfigurableApplicationContext])
          .sources(classOf[AdminServiceConfiguration])
          .properties(properties)
        appBuilder.run()
      }
    }

    override def isInitialized: Boolean = adminContext.isDefined
  }
}

object AdminServiceImpl {
  private final val ConfigName = "admin-service.cfg.xml"
}
