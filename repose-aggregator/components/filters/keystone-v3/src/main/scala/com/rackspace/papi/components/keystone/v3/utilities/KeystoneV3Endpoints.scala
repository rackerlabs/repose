package com.rackspace.papi.components.keystone.v3.utilities

object KeystoneV3Endpoints {
  final val TOKEN = "/v3/auth/tokens"
  final val GROUPS = (userId: String) => s"/v3/users/$userId/groups"
}
