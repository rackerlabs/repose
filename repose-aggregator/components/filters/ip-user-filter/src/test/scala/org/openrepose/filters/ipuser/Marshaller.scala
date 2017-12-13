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
package org.openrepose.filters.ipuser

import java.net.URL
import java.nio.charset.StandardCharsets

import org.openrepose.commons.config.parser.jaxb.JaxbConfigurationParser
import org.openrepose.commons.config.resource.impl.{BufferedURLConfigurationResource, ByteArrayConfigurationResource}
import org.openrepose.filters.ipuser.config.IpUserConfig

object Marshaller {

  val IpUserConfig = getClass.getResource("/META-INF/schema/config/ip-user.xsd")

  def configFromResource(resource: String, xsdURL: URL = IpUserConfig): IpUserConfig = {
    val parser = new JaxbConfigurationParser(
      classOf[IpUserConfig],
      xsdURL,
      this.getClass.getClassLoader
    )
    val configResource = new BufferedURLConfigurationResource(this.getClass.getResource(resource))
    parser.read(configResource)
  }

  def configFromString(content: String, xsdURL: URL = IpUserConfig): IpUserConfig = {
    val parser = new JaxbConfigurationParser(
      classOf[IpUserConfig],
      xsdURL,
      this.getClass.getClassLoader
    )
    val configResource = new ByteArrayConfigurationResource("", content.getBytes(StandardCharsets.UTF_8))
    parser.read(configResource)
  }
}
