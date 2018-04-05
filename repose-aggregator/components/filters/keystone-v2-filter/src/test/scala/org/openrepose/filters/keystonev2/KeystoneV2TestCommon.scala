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

import org.openrepose.filters.keystonev2.KeystoneV2Common.{Role, ValidToken}

object KeystoneV2TestCommon {
  def createValidToken(expirationDate: String = "",
                       userId: String = "",
                       roles: Seq[Role] = Seq.empty[Role],
                       username: Option[String] = None,
                       tenantName: Option[String] = None,
                       defaultTenantId: Option[String] = None,
                       tenantIds: Seq[String] = Seq.empty[String],
                       impersonatorId: Option[String] = None,
                       impersonatorName: Option[String] = None,
                       impersonatorRoles: Seq[String] = Seq.empty[String],
                       domainId: Option[String] = None,
                       defaultRegion: Option[String] = None,
                       contactId: Option[String] = None,
                       authenticatedBy: Option[Seq[String]] = None): ValidToken = {
    ValidToken(expirationDate,
      userId,
      roles,
      username,
      tenantName,
      defaultTenantId,
      tenantIds,
      impersonatorId,
      impersonatorName,
      impersonatorRoles,
      domainId,
      defaultRegion,
      contactId,
      authenticatedBy)
  }
}
