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
  implicit val roleFormat = jsonFormat(Role, "id", "name", "project_id", "RAX-AUTH:projectId", "domain_id", "description")
  implicit val tokenCredentialsFormat = jsonFormat1(TokenCredentials)
  implicit val userForAuthenticateResponse = jsonFormat(UserForAuthenticateResponse, "domain","id","name", "description", "default_project_id", "RAX-AUTH:defaultRegion")
  implicit val impersonatorForAuthenticationResponse = jsonFormat2(ImpersonatorForAuthenticationResponse)
  implicit val authenticateResponseFormat = jsonFormat(AuthenticateResponse, "expires_at", "issued_at", "methods", "domain", "project", "catalog", "roles", "user", "RAX-AUTH:impersonator")
  implicit val authIdentityRequestFormat = jsonFormat3(AuthIdentityRequest)
  implicit val authRequestFormat = jsonFormat2(AuthRequest)
  implicit val authResponseFormat = jsonFormat1(AuthResponse)
  implicit val authRequestRootFormat = jsonFormat1(AuthRequestRoot)
  implicit val groupFormat = jsonFormat4(Group)
  implicit val groupsFormat = jsonFormat1(Groups)
}
