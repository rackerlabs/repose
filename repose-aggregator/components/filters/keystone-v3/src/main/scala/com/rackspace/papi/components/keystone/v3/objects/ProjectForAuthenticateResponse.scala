package com.rackspace.papi.components.keystone.v3.objects

case class ProjectForAuthenticateResponse(domain: Option[DomainsForAuthenticateResponse] = None, id: Option[String] = None,
                                          name: Option[String] = None, enabled: Option[Boolean] = None) extends Serializable
