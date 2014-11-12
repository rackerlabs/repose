package com.rackspace.httpdelegation

/** The API for the HTTP delegation library. */
trait HttpDelegationManager {

  /** Generates the appropriate headers to add to a HTTP request to support delegation.
    *
    * @param statusCode the status code which would have been applied if not for delegation
    * @param message a description of why the status code would have been applied
    * @param quality a value, between 0 and 1, which is used to determine the order of importance for various
    *                delegations
    * @return a map of headers to be added to a HTTP request
    */
  def buildDelegationHeaders(statusCode: Int, component: String, message: String, quality: Double): Map[String, Set[String]] = {
    assume(component != null, "Component cannot be null")
    assume(message != null, "Message cannot be null")

    Map[String, Set[String]](
      HttpDelegationHeaders.Delegated -> Set(
        "status_code=" + statusCode + "`component=" + component + "`message=" + message + ";q=" + quality
      )
    )
  }
}
