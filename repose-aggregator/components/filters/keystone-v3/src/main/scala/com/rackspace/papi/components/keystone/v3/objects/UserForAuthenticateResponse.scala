package com.rackspace.papi.components.keystone.v3.objects

case class UserForAuthenticateResponse(domain: Option[DomainsForAuthenticateResponse] = None, id: Option[String] = None, name: Option[String] = None,
                                       description: Option[String] = None, default_project_id: Option[String] = None) extends Serializable
