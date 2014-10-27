package com.rackspace.httpdelegation

/**
 * An object which enumerates all of the header names used in the delegation protocol.
 */
object DelegationHeaders {

  // Whether or not the request has been delegated. By convention, the header value corresponding to this header name
  // should be either 'true' or 'false'. If the value is anything other than false, however, the request will be treated
  // like a delegated request.
  final val Delegated = "X-Delegated"
}
