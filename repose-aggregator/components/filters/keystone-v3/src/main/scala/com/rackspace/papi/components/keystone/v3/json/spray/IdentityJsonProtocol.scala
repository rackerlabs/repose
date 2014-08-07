package com.rackspace.papi.components.keystone.v3.json.spray

import com.rackspace.papi.components.keystone.v3.objects.{AuthRequest, AuthResponse}
import spray.json.DefaultJsonProtocol

object IdentityJsonProtocol extends DefaultJsonProtocol {
  implicit val authResponseFormat = jsonFormat1(AuthResponse)
  implicit val authRequestFormat = jsonFormat2(AuthRequest)
}
