package com.rackspace.httpdelegation.impl

import com.rackspace.httpdelegation.{HttpDelegationHeaders, HttpDelegationManager}

/** An implementation of the API defined in [[HttpDelegationManager]].
  *
  * This object provides functions to manipulate delegation headers.
  */
object HttpDelegationManagerImpl extends HttpDelegationManager {

  override def buildDelegationHeaders(statusCode: Int, component: String, message: String, quality: Double): Map[String, Set[String]] = {
    Map[String, Set[String]](
      HttpDelegationHeaders.Delegated -> Set(
        statusCode + ";component=" + component + ";msg=" + message + ";q=" + quality
      )
    )
  }
}
