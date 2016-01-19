package org.openrepose.nodeservice.atomfeed.impl.auth

import org.openrepose.nodeservice.atomfeed.AuthenticationRequestContext

/**
  * A simple Scala bean-like implementation of the [[AuthenticationRequestContext]] interface.
  */
class AuthenticationRequestContextImpl(requestId: String, reposeVersion: String) extends AuthenticationRequestContext {

  override def getRequestId: String = requestId

  override def getReposeVersion: String = reposeVersion
}

object AuthenticationRequestContextImpl {
  def apply(requestId: String, reposeVersion: String) = new AuthenticationRequestContextImpl(requestId, reposeVersion)
}
