package com.rackspace.papi.components.keystone.v3.objects

case class UserNamePasswordRequest(domain: Option[DomainType] = None, id: Option[String] = None, password: String, name: Option[String] = None)
