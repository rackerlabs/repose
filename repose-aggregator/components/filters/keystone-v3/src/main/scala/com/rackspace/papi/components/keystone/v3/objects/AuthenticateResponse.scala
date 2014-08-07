package com.rackspace.papi.components.keystone.v3.objects

case class AuthenticateResponse(expires_at : String, issued_at : String, methods : List[String] = None, domain : DomainsForAuthenticateResponse = None,
                                project : ProjectForAuthenticateResponse = None, catalog : CatalogType = None, roles : List[Role] = None,
                                user : UserForAuthenticateResponse = None)
