package com.rackspace.papi.components.rackspace.identity.basicauth

import org.apache.commons.codec.binary.Base64

import scala.collection.JavaConverters

object BasicAuthUtils {
  /**
   * Returns a tuple of the (username, API Key) retrieved from an HTTP Basic authentication header (Authorization) that
   * has already been stripped of the "Basic " auth method identifier.
   * @param decoded the cleaned header value to be decoded and split
   * @return a tuple of the (username, API Key)
   */
  def extractCredentials(authValue: String): (String, String) = {
    val decodedString = new String(Base64.decodeBase64(authValue))
    val username = decodedString.split(":").head
    val password = decodedString.replace(s"$username:", "")
    (username, password)
  }

  /**
   * Returns an Iterator of the Authentication header values that match the desired auth method.
   * @param optionHeaders the Authentication header values to search
   * @param method the auth method to search for
   * @return an Iterator of the Authentication header values that match the desired auth method
   */
  def getBasicAuthHeaders(headers: java.util.Enumeration[String], method: String): Iterator[String] = {
      import scala.collection.JavaConverters._
      headers.asScala.filter(_.toUpperCase.startsWith(method.toUpperCase()))
  }
}
