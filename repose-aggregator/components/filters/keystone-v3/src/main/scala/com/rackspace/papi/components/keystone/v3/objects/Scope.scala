package com.rackspace.papi.components.keystone.v3.objects

case class Scope(domain: Option[DomainScopeType] = None, project: Option[ProjectScope] = None, trust: Option[TrustScopeType] = None) extends Serializable
