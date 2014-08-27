package com.rackspace.papi.components.keystone.v3.utilities.exceptions

class InvalidUserForGroupsException(message: String, cause: Throwable) extends Exception(message, cause) {
  def this(message: String) = {
    this(message, null)
  }
}
