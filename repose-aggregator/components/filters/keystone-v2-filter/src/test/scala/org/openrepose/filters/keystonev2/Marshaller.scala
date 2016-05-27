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
package org.openrepose.filters.keystonev2

import java.net.URL
import java.nio.charset.StandardCharsets

import org.openrepose.commons.config.parser.jaxb.JaxbConfigurationParser
import org.openrepose.commons.config.resource.ConfigurationResource
import org.openrepose.commons.config.resource.impl.{ByteArrayConfigurationResource, BufferedURLConfigurationResource}
import org.openrepose.core.systemmodel.SystemModel
import org.openrepose.filters.keystonev2.config.KeystoneV2Config

import scala.reflect.ClassTag

object Marshaller {

  val systemModelXSD = getClass.getResource("/META-INF/schema/system-model/system-model.xsd")
  val keystoneV2XSD = getClass.getResource("/META-INF/schema/config/keystone-v2.xsd")


  def systemModel(resource: String): SystemModel = {
    configResource[SystemModel](new BufferedURLConfigurationResource(this.getClass.getResource(resource)), systemModelXSD)
  }

  def configResource[T: ClassTag](configResource: ConfigurationResource, xsdURL: URL): T = {
    import scala.reflect._
    val ct: ClassTag[T] = classTag[T]
    val parser = JaxbConfigurationParser.getXmlConfigurationParser(
      ct.runtimeClass.asInstanceOf[Class[T]],
      xsdURL,
      this.getClass.getClassLoader)

    parser.read(configResource)
  }

  def keystonev2Config(resource: String): KeystoneV2Config = {
    configResource[KeystoneV2Config](new BufferedURLConfigurationResource(this.getClass.getResource(resource)), keystoneV2XSD)
  }

  def keystoneV2ConfigFromString(content: String): KeystoneV2Config = {
    configResource[KeystoneV2Config](new ByteArrayConfigurationResource("keystoneV2Config", content.getBytes(StandardCharsets.UTF_8)), keystoneV2XSD)

  }

}
