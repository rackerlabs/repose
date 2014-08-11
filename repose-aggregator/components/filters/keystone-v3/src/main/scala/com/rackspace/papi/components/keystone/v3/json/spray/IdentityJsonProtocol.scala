package com.rackspace.papi.components.keystone.v3.json.spray

import com.rackspace.papi.components.keystone.v3.objects._
import spray.json.DefaultJsonProtocol

object IdentityJsonProtocol extends DefaultJsonProtocol {
    implicit val endpointTypeFormat = jsonFormat6(EndpointType)
    implicit val endpointsTypeFormat = jsonFormat1(EndpointsType)
    implicit val serviceForAuthenticationResponse = jsonFormat3(ServiceForAuthenticationResponse)
    implicit val catalogTypeFormat = jsonFormat1(CatalogType)
    implicit val domainScopeFormat = jsonFormat2(DomainScopeType)
    implicit val domainsForAuthenticateResponse = jsonFormat3(DomainsForAuthenticateResponse)
    implicit val domainTypeFormat = jsonFormat3(DomainType)
    implicit val userNamePasswordRequestFormat = jsonFormat4(UserNamePasswordRequest)
    implicit val passwordCredentialsFormat = jsonFormat1(PasswordCredentials)
    implicit val projectForAuthenticateResponseFormat = jsonFormat4(ProjectForAuthenticateResponse)
    implicit val projectScopeTypeFormat = jsonFormat3(ProjectScopeType)
    implicit val roleFormat = jsonFormat5(Role)
    implicit val trustScopeTypeFormat = jsonFormat1(TrustScopeType)
    implicit val scopeTypeFormat = jsonFormat3(ScopeType)
    implicit val tokenCredentialsFormat = jsonFormat1(TokenCredentials)
    implicit val userForAuthenticateResponse = jsonFormat5(UserForAuthenticateResponse)
    implicit val authenticateResponseFormat = jsonFormat8(AuthenticateResponse)
    implicit val authIdentityRequestFormat = jsonFormat3(AuthIdentityRequest)
    implicit val authRequestFormat = jsonFormat2(AuthRequest)
    implicit val authResponseFormat = jsonFormat1(AuthResponse)
}
