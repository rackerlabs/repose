package com.rackspace.papi.components.openstack.identity.v3.utilities

object OpenstackIdentityV3Endpoints {
  final val TOKEN = "/v3/auth/tokens"
  final val GROUPS = (userId: String) => s"/v3/users/$userId/groups"
}
