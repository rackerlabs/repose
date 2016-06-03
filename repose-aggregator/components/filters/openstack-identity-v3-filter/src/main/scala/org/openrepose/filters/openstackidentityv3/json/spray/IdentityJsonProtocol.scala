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
package org.openrepose.filters.openstackidentityv3.json.spray

import org.openrepose.filters.openstackidentityv3.objects._
import spray.json.DefaultJsonProtocol

object IdentityJsonProtocol extends DefaultJsonProtocol {
  implicit val endpointFormat = jsonFormat6(Endpoint)
  implicit val serviceForAuthenticationResponse = jsonFormat3(ServiceForAuthenticationResponse)
  implicit val domainScopeFormat = jsonFormat2(DomainScope)
  implicit val domainsForAuthenticateResponse = jsonFormat3(DomainsForAuthenticateResponse)
  implicit val domainFormat = jsonFormat3(Domain)
  implicit val projectScopeFormat = jsonFormat3(ProjectScope)
  implicit val trustScopeFormat = jsonFormat1(TrustScope)
  implicit val scopeFormat = jsonFormat3(Scope)
  implicit val userNamePasswordRequestFormat = jsonFormat5(UserNamePasswordRequest)
  implicit val passwordCredentialsFormat = jsonFormat1(PasswordCredentials)
  implicit val projectForAuthenticateResponseFormat = jsonFormat4(ProjectForAuthenticateResponse)
  implicit val roleFormat = jsonFormat(Role, "id", "name", "project_id", "RAX-AUTH:project_id", "domain_id", "description")
  implicit val tokenCredentialsFormat = jsonFormat1(TokenCredentials)
  implicit val userForAuthenticateResponse = jsonFormat(UserForAuthenticateResponse, "domain", "id", "name", "description", "default_project_id", "RAX-AUTH:defaultRegion")
  implicit val impersonatorForAuthenticationResponse = jsonFormat2(ImpersonatorForAuthenticationResponse)
  implicit val authenticateResponseFormat = jsonFormat(AuthenticateResponse, "expires_at", "domain", "project", "catalog", "roles", "user", "RAX-AUTH:impersonator")
  implicit val authIdentityRequestFormat = jsonFormat3(AuthIdentityRequest)
  implicit val authRequestFormat = jsonFormat2(AuthRequest)
  implicit val authResponseFormat = jsonFormat1(AuthResponse)
  implicit val authRequestRootFormat = jsonFormat1(AuthRequestRoot)
  implicit val groupFormat = jsonFormat4(Group)
  implicit val groupsFormat = jsonFormat1(Groups)
}
