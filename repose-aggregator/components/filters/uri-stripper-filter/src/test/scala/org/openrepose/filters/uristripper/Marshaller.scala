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
package org.openrepose.filters.uristripper

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.StandardCharsets
import javax.xml.bind.{JAXBContext, JAXBElement, Unmarshaller}

import org.openrepose.filters.uristripper.config.UriStripperConfig

import scala.reflect._

object Marshaller {
  def uriStripperConfigFromString(content: String): UriStripperConfig = {
    val jaxbContext: JAXBContext = JAXBContext.newInstance(classTag[UriStripperConfig].runtimeClass.asInstanceOf[Class[UriStripperConfig]].getPackage.getName, this.getClass.getClassLoader)
    jaxbContext.createUnmarshaller().unmarshal(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))).asInstanceOf[JAXBElement[_]].getValue.asInstanceOf[UriStripperConfig]
  }
}
