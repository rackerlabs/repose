package com.rackspace.papi.components.keystone.v3.objects

// TODO: Use jsonFormat to map type => openstackType
case class ServiceForAuthenticationResponse(endpoints: Option[List[Endpoints]] = None, openstackType: String, id: Option[String] = None) extends Serializable
