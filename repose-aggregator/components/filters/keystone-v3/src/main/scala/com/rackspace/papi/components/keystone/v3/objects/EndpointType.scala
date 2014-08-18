package com.rackspace.papi.components.keystone.v3.objects

case class EndpointType(id: String, name: String, interface: Option[String] = None, region: Option[String] = None, url: String, service_id: Option[String] = None) extends Serializable
