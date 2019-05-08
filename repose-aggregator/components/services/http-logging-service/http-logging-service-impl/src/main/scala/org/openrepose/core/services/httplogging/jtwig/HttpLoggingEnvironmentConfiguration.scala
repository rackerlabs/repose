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
package org.openrepose.core.services.httplogging.jtwig

import org.jtwig.environment.{EnvironmentConfiguration, EnvironmentConfigurationBuilder}
import org.openrepose.core.services.httplogging.config.Format

/**
  * Customizes some default settings of the [[EnvironmentConfigurationBuilder]]
  * for use by the [[org.openrepose.core.services.httplogging.HttpLoggingServiceImpl]].
  */
object HttpLoggingEnvironmentConfiguration {
  private final val StartCodeTag: String = "{;"
  private final val EndCodeTag: String = ";}"
  private final val JsonEscapeEngineName: String = "javascript"
  private final val NoopEscapeEngineName: String = "none"
  private final val EscapeEnginesByFormat: Map[Format, String] = Map(
    Format.JSON -> JsonEscapeEngineName
  )

  def apply(format: Format): EnvironmentConfiguration = {
    val initialEscapeEngine = EscapeEnginesByFormat.getOrElse(format, NoopEscapeEngineName)

    // @formatter:off
    EnvironmentConfigurationBuilder
      .configuration()
        .parser()
          .syntax()
            .withStartCode(StartCodeTag).withEndCode(EndCodeTag)
          .and()
        .and()
        .escape()
          .withInitialEngine(initialEscapeEngine)
        .and()
      .build()
    // @formatter:on
  }
}
