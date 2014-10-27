package com.rackspace.httpdelegation

import javax.servlet.http.HttpServletRequest

/**
 * The API for the delegation headers library.
 */
trait DelegationHeaderManager {

  /**
   *
   */
  def isDelegated(request: HttpServletRequest): Boolean = ???

  /**
   *
   */
  def setDelegated(request: HttpServletRequest): HttpServletRequest = ???
}
