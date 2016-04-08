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

import java.io.ByteArrayInputStream
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.{SchemaFactory, Validator}

class ConfigValidator(validator: Validator) {
  def validateConfigFile(fileName: String): Unit = {
    validator.validate(new StreamSource(classOf[ConfigValidator].getResourceAsStream(fileName)))
  }

  def validateConfigString(config: String): Unit = {
    validator.validate(new StreamSource(new ByteArrayInputStream(config.getBytes)))
  }
}

object ConfigValidator {
  def apply(schemaFileName: String): ConfigValidator = {
    val factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1")
    factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true)
    new ConfigValidator(
      factory.newSchema(new StreamSource(classOf[ConfigValidator].getResourceAsStream(schemaFileName))).newValidator())
  }
}
