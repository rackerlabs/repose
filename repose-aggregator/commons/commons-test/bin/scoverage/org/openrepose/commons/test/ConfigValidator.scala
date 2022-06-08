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
package org.openrepose.commons.test

import java.io.{ByteArrayInputStream, InputStream}
import java.net.URL
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.{SchemaFactory, Validator}

class ConfigValidator(validator: Validator) {
  def validateConfigFile(fileName: String): Unit = {
    validateConfig(classOf[ConfigValidator].getResourceAsStream(fileName))
  }

  def validateConfigString(config: String): Unit = {
    validateConfig(new ByteArrayInputStream(config.getBytes))
  }

  def validateConfig(config: InputStream): Unit = {
    validator.validate(new StreamSource(config))
  }
}

object ConfigValidator {
  def apply(schemaFileNames: String*): ConfigValidator = {
    apply(schemaFileNames.map(classOf[ConfigValidator].getResource): _*)
  }

  // The dummy implicit here is not actually used. It is only included to provide
  // this function with a different signature than the other. The reason that this
  // is necessary is that Scala varags are packed into a generic collection which
  // is subject to type erasure. As a result, varags with different types are treated
  // as equivalent types.
  def apply(schemas: URL*)(implicit dummy: DummyImplicit): ConfigValidator = {
    val factory = SchemaFactory.newInstance(
      "http://www.w3.org/XML/XMLSchema/v1.1",
      classOf[org.apache.xerces.jaxp.validation.XMLSchema11Factory].getCanonicalName,
      classOf[ConfigValidator].getClassLoader)
    factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true)
    new ConfigValidator(factory
      .newSchema(schemas.map(u => new StreamSource(u.toString)).toArray[Source])
      .newValidator())
  }
}
