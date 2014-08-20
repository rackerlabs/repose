package com.rackspace.papi.components.keystone.v3.objects

case class UserNamePasswordRequest(domain: Option[Domain] = None, id: Option[String] = None, name: Option[String] = None, password: String, scope: Option[Scope] = None) extends Serializable
