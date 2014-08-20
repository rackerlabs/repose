package com.rackspace.papi.components.keystone.v3.objects

case class ProjectScope(domain: Option[DomainScope] = None, id: String, name: Option[String] = None) extends Serializable
