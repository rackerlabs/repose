package com.rackspace.papi.components.keystone.v3.objects

case class ProjectScopeType(domain: Option[DomainScopeType] = None, id: String, name: Option[String] = None) extends Serializable
