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

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads}

object KeystoneV2Common {

  final val TokenRequestAttributeName: String = "http://openrepose.org/filters/keystonev2/token"
  final val DomainRoleTenantKey: String = "repose/domain/roles"

  type TenantToRolesMap = Map[String, Set[String]]

  case class ValidToken(expirationDate: String,
                        userId: String,
                        roles: Seq[Role],
                        username: Option[String],
                        tenantName: Option[String],
                        defaultTenantId: Option[String],
                        tenantIds: Seq[String],
                        impersonatorId: Option[String],
                        impersonatorName: Option[String],
                        impersonatorRoles: Seq[String],
                        domainId: Option[String],
                        defaultRegion: Option[String],
                        contactId: Option[String],
                        authenticatedBy: Option[Seq[String]])

  case class Role(name: String, tenantId: Option[String] = None)

  case class Endpoint(region: Option[String], name: Option[String], endpointType: Option[String], publicURL: String) {
    /**
      * Determines whether or not this endpoint meets the requirements set forth by the values contained in
      * endpointRequirement for the purpose of authorization.
      *
      * @param endpointRequirement an endpoint containing fields with required values
      * @return true if this endpoint has field values matching those in the endpointRequirement, false otherwise
      */
    def meetsRequirement(endpointRequirement: Endpoint): Boolean = {
      def compare(available: Option[String], required: Option[String]) = (available, required) match {
        case (Some(x), Some(y)) => x == y
        case (None, Some(_)) => false
        case _ => true
      }

      this.publicURL.startsWith(endpointRequirement.publicURL) &&
        compare(this.region, endpointRequirement.region) &&
        compare(this.name, endpointRequirement.name) &&
        compare(this.endpointType, endpointRequirement.endpointType)
    }
  }

  case class EndpointsData(json: String, vector: Vector[Endpoint])

  object Endpoint {
    implicit val endpointReads: Reads[Endpoint] = (
      (JsPath \ "region").readNullable[String] and
        (JsPath \ "name").readNullable[String] and
        (JsPath \ "type").readNullable[String] and
        (JsPath \ "publicURL").read[String]
      ) (Endpoint.apply _)
  }

}
