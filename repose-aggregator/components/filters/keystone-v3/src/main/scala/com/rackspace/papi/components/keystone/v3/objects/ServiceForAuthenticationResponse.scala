package com.rackspace.papi.components.keystone.v3.objects

// TODO: Use jsonFormat to map type => openstackType
case class ServiceForAuthenticationResponse(endpoints : List[EndpointsType] = None, openstackType : String, id : String = None)
