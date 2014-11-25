package com.rackspace.httpdelegation

/**
 * An object which enumerates all of the header names used in the delegation protocol.
 */
object HttpDelegationHeaderNames {

  /**
   * Whether or not the request has been delegated. The value of this header will be the response code that would have
   * been returned if not for delegation along with supplementary data.
   */
  final val Delegated = "X-Delegated"
}
