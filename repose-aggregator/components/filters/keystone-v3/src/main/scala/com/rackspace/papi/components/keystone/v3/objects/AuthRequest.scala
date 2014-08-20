package com.rackspace.papi.components.keystone.v3.objects

case class AuthRequest(identity: AuthIdentityRequest, scope: Option[Scope] = None) extends Serializable
