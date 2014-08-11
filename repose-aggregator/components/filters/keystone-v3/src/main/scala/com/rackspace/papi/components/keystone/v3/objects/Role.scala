package com.rackspace.papi.components.keystone.v3.objects

case class Role(id: Option[String] = None, name: String, project_id: Option[String] = None, domain_id: Option[String] = None, description: String)
