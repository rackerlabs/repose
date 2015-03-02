package org.openrepose.filters.openstackidentityv3.utilities

class IdentityServiceException(message: String) extends Exception(message)

class IdentityServiceOverLimitException(message: String, statusCode: Int, retryAfter: String) extends IdentityServiceException(message) {
  def getStatusCode: Int = {
    statusCode
  }

  def getRetryAfter: String = {
    retryAfter
  }
}

class InvalidAdminCredentialsException(message: String) extends Exception(message)

class InvalidSubjectTokenException(message: String) extends Exception(message)

class InvalidUserForGroupsException(message: String) extends Exception(message)
