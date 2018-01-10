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

import javax.inject.{Inject, Named}
import javax.servlet.ServletRequest

import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.keystonev2.AbstractKeystoneV2Filter.KeystoneV2Result
import org.openrepose.filters.keystonev2.KeystoneV2Authorization.doAuthorization
import org.openrepose.filters.keystonev2.KeystoneV2Common.{TokenRequestAttributeName, ValidToken}
import org.openrepose.filters.keystonev2.config.KeystoneV2Config

import scala.util.Try

@Named
class KeystoneV2AuthorizationFilter @Inject()(configurationService: ConfigurationService)
  extends AbstractKeystoneV2Filter[KeystoneV2Config](configurationService) {

  import KeystoneV2AuthorizationFilter._

  override val DEFAULT_CONFIG = "keystone-v2-authorization.cfg.xml"
  override val SCHEMA_LOCATION = "/META-INF/schema/config/keystone-v2-authorization.xsd"

  override val handleFailures: PartialFunction[Try[Unit.type], KeystoneV2Result] = KeystoneV2Authorization.handleFailures

  override def doAuth(request: HttpServletRequestWrapper): Try[Unit.type] = {
    ???
  }

  def getToken(request: ServletRequest): Try[ValidToken] = {
    Try {
      Option(request.getAttribute(TokenRequestAttributeName)).get.asInstanceOf[ValidToken]
    } recover {
      case nsee: NoSuchElementException => throw MissingTokenException("Token request attribute does not exist", nsee)
      case cce: ClassCastException => throw InvalidTokenException("Token request attribute is not a valid token", cce)
    }
  }
}

object KeystoneV2AuthorizationFilter {
  case class MissingTokenException(message: String, cause: Throwable = null) extends Exception(message, cause)
  case class InvalidTokenException(message: String, cause: Throwable = null) extends Exception(message, cause)
}
