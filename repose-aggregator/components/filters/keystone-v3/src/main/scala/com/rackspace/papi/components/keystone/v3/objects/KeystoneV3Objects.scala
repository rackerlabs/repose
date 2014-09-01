package com.rackspace.papi.components.keystone.v3.objects

case class DomainsForAuthenticateResponse(id: Option[String] = None,
                                          name: Option[String] = None,
                                          enabled: Option[Boolean] = None
                                           ) extends Serializable

case class ProjectForAuthenticateResponse(domain: DomainsForAuthenticateResponse,
                                          id: Option[String] = None,
                                          name: Option[String] = None,
                                          enabled: Option[Boolean] = None
                                           ) extends Serializable

case class Endpoint(id: String,
                    name: Option[String] = None,
                    interface: Option[String] = None,
                    region: Option[String] = None,
                    url: String,
                    service_id: Option[String] = None
                     ) extends Serializable {
  /**
   * Determine if this endpoint matches the specified parameters
   * For any of them, except for URL (which is not optional), if there is Some(x) it must be set,
   * otherwise, it matches (so it ignores requirements that are None)
   * @param requiredUrl
   * @param requiredRegion
   * @param requiredName
   * @param requiredInterface
   * @return
   */
  def matches(requiredUrl:String,
              requiredRegion:Option[String],
              requiredName:Option[String],
              requiredInterface:Option[String]):Boolean = {
    def compare(required:Option[String], available:Option[String]):Boolean = (required, available) match {
      case (Some(x), Some(y)) => x == y
      case (Some(_), None) => false
      case _ => true
    }

    requiredUrl == url &&
      compare(requiredRegion, region) &&
      compare(requiredName, name) &&
      compare(requiredInterface, interface)
  }
}

case class ServiceForAuthenticationResponse(endpoints: List[Endpoint],
                                            // openstackType: String, // TODO: this probably won't work since the actual name of the key is "type"
                                            id: Option[String] = None,
                                            name: Option[String] = None
                                             ) extends Serializable

case class Catalog(service: List[ServiceForAuthenticationResponse]
                    ) extends Serializable

case class Role(id: Option[String] = None,
                name: String,
                project_id: Option[String] = None,
                domain_id: Option[String] = None,
                description: String
                 ) extends Serializable

case class UserForAuthenticateResponse(domain: DomainsForAuthenticateResponse,
                                       id: Option[String] = None,
                                       name: Option[String] = None,
                                       description: Option[String] = None,
                                       default_project_id: Option[String] = None
                                        ) extends Serializable

case class AuthenticateResponse(expires_at: String,
                                issued_at: String,
                                methods: List[String],
                                domain: Option[DomainsForAuthenticateResponse],
                                project: Option[ProjectForAuthenticateResponse],
                                catalog: Option[Catalog],
                                roles: Option[List[Role]],
                                user: UserForAuthenticateResponse
                                 ) extends Serializable

case class UserNamePasswordRequest(domain: Option[Domain] = None,
                                   id: Option[String] = None,
                                   name: Option[String] = None,
                                   password: String,
                                   scope: Option[Scope] = None
                                    ) extends Serializable

case class PasswordCredentials(user: UserNamePasswordRequest
                                ) extends Serializable

case class TokenCredentials(id: String
                             ) extends Serializable

case class AuthIdentityRequest(methods: List[String],
                               password: Option[PasswordCredentials] = None,
                               token: Option[TokenCredentials] = None
                                ) extends Serializable

case class AuthRequest(identity: AuthIdentityRequest,
                       scope: Option[Scope] = None
                        ) extends Serializable

case class AuthRequestRoot(auth: AuthRequest
                            ) extends Serializable

case class AuthResponse(token: AuthenticateResponse
                         ) extends Serializable

case class Domain(id: Option[String] = None,
                  name: String,
                  enabled: Option[Boolean] = None
                   ) extends Serializable

case class DomainScope(id: String,
                       name: String
                        ) extends Serializable


case class ProjectScope(domain: Option[DomainScope] = None,
                        id: String,
                        name: Option[String] = None
                         ) extends Serializable

case class TrustScope(id: String
                       ) extends Serializable

case class Scope(domain: Option[DomainScope] = None,
                 project: Option[ProjectScope] = None,
                 trust: Option[TrustScope] = None
                  ) extends Serializable

case class Group(id: String,
                 name: String,
                 description: String,
                 domain_id: Option[String] = None) extends Serializable

case class Groups(groups: List[Group]) extends Serializable
