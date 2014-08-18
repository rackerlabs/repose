package com.rackspace.papi.components.keystone.v3.objects

case class ScopeType(domain: Option[DomainScopeType] = None, project: Option[ProjectScopeType] = None, trust: Option[TrustScopeType] = None) extends Serializable
