package com.rackspace.papi.components.keystone.v3.objects

case class Scope(domain: Option[DomainScope] = None, project: Option[ProjectScope] = None, trust: Option[TrustScope] = None) extends Serializable
