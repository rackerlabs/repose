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
package org.openrepose.filters.openstackidentityv3.objects

case class ProjectForAuthenticateResponse(id: Option[String] = None,
                                          name: Option[String] = None
                                           ) extends Serializable

case class Endpoint(id: String,
                    name: Option[String] = None,
                    interface: Option[String] = None,
                    region: Option[String] = None,
                    url: String,
                    service_id: Option[String] = None
                     ) extends Serializable {

  /**
   * Determines whether or not this endpoint meets the requirements set forth by the values contained in
   * endpointRequirement for the purpose of authorization.
   *
   * @param endpointRequirement an endpoint containing fields with required values
   * @return true if this endpoint has field values matching those in the endpointRequirement, false otherwise
   */
  def meetsRequirement(endpointRequirement: Endpoint) = {
    def compare(available: Option[String], required: Option[String]) = (available, required) match {
      case (Some(x), Some(y)) => x == y
      case (None, Some(_)) => false
      case _ => true
    }

    this.url.startsWith(endpointRequirement.url) &&
      compare(this.region, endpointRequirement.region) &&
      compare(this.name, endpointRequirement.name) &&
      compare(this.interface, endpointRequirement.interface)
  }
}

case class ServiceForAuthenticationResponse(endpoints: List[Endpoint],
                                            id: Option[String] = None,
                                            name: Option[String] = None
                                             ) extends Serializable

case class Role(name: String,
                project_id: Option[String] = None,
                rax_project_id: Option[String] = None
                 ) extends Serializable

case class UserForAuthenticateResponse(id: Option[String] = None,
                                       name: Option[String] = None,
                                       rax_default_region: Option[String] = None
                                        ) extends Serializable

case class ImpersonatorForAuthenticationResponse(id: Option[String] = None,
                                                 name: Option[String] = None)

case class AuthenticateResponse(expires_at: String,
                                project: Option[ProjectForAuthenticateResponse],
                                catalog: Option[List[ServiceForAuthenticationResponse]],
                                roles: Option[List[Role]],
                                user: UserForAuthenticateResponse,
                                rax_impersonator: Option[ImpersonatorForAuthenticationResponse] = None
                                 ) extends Serializable

case class Group(id: String,
                 name: String,
                 description: Option[String] = None,
                 domain_id: Option[String] = None) extends Serializable
