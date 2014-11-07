package com.rackspace.httpdelegation

/**
 * An object which enumerates all of the header names used in the delegation protocol.
 */
object HttpDelegationHeaders {

  /**
   * Whether or not the request has been delegated. The value of this header will be the response code that would have
   * been returned if not for delegation along with a message and quality. The form of the value is as follows:
   * 404;msg=not found;q=.8
   */
  final val Delegated = "X-Delegated"
}
