package com.rackspace.papi.components.keystone.v3.objects

case class AuthenticateResponse(expires_at: String, issued_at: String, methods: Option[List[String]] = None, domain: Option[DomainsForAuthenticateResponse] = None,
                                project: Option[ProjectForAuthenticateResponse] = None, catalog: Option[Catalog] = None, roles: Option[List[Role]] = None,
                                user: Option[UserForAuthenticateResponse] = None) extends Serializable
