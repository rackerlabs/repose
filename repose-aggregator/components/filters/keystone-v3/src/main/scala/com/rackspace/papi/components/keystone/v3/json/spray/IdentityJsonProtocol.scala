package com.rackspace.papi.components.keystone.v3.json.spray

import com.rackspace.papi.components.keystone.v3.objects._
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
  implicit val roleFormat = jsonFormat5(Role)
  implicit val tokenCredentialsFormat = jsonFormat1(TokenCredentials)
  implicit val userForAuthenticateResponse = jsonFormat5(UserForAuthenticateResponse)
  implicit val authenticateResponseFormat = jsonFormat8(AuthenticateResponse)
  implicit val authIdentityRequestFormat = jsonFormat3(AuthIdentityRequest)
  implicit val authRequestFormat = jsonFormat2(AuthRequest)
  implicit val authResponseFormat = jsonFormat1(AuthResponse)
  implicit val authRequestRootFormat = jsonFormat1(AuthRequestRoot)
  implicit val groupFormat = jsonFormat4(Group)
  implicit val groupsFormat = jsonFormat1(Groups)
}
