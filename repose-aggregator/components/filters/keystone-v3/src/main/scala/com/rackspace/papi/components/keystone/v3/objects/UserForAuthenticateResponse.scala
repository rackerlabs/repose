package com.rackspace.papi.components.keystone.v3.objects

case class UserForAuthenticateResponse(domain : DomainsForAuthenticateResponse = None, links : LinksType = None, id : String = None, name : String = None,
                                       description : String = None, default_project_id : String = None)
