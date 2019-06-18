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
package org.openrepose.filters.openapivalidator

import com.atlassian.oai.validator.interaction.request.CustomRequestValidator
import com.atlassian.oai.validator.model.{ApiOperation, Request}
import com.atlassian.oai.validator.report.ValidationReport
import org.openrepose.commons.utils.http.OpenStackServiceHeader.ROLES
import org.openrepose.filters.openapivalidator.RaxRolesValidator.RoleValidationMessageKey

import scala.collection.JavaConverters._

class RaxRolesValidator extends CustomRequestValidator {
  override def validate(request: Request, apiOperation: ApiOperation): ValidationReport = {
    apiOperation.getOperation.getExtensions.asScala.get("x-rax-roles")
      .map(_.asInstanceOf[java.util.ArrayList[String]])
      .find(configuredRoles => !request.getHeaderValues(ROLES).asScala.exists(configuredRoles.contains(_)))
      .map(_ => ValidationReport.singleton(ValidationReport.Message.create(RoleValidationMessageKey, "None of the configured roles match the request.").build()))
      .getOrElse(ValidationReport.empty())
  }
}

object RaxRolesValidator {
  val RoleValidationMessageKey = "rax.roles"
}
