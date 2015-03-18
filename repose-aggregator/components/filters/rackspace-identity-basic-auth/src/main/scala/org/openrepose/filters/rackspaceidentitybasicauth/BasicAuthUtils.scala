/*
 *  Copyright (c) 2015 Rackspace US, Inc.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.openrepose.filters.rackspaceidentitybasicauth

import org.apache.commons.codec.binary.Base64

trait BasicAuthUtils {
  /**
   * Returns a tuple of the (username, API Key) retrieved from an HTTP Basic authentication header (Authorization) that
   * has already been stripped of the "Basic " auth method identifier.
   * @param authValue the cleaned header value to be decoded and split
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
   * @param headers the Authentication header values to search
   * @param method the auth method to search for
   * @return an Iterator of the Authentication header values that match the desired auth method
   */
  def getBasicAuthHeaders(headers: java.util.Enumeration[String], method: String): Iterator[String] = {
      import scala.collection.JavaConverters._
      headers.asScala.filter(_.toUpperCase.startsWith(method.toUpperCase()))
  }
}
