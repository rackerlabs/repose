package com.rackspace.papi.components.openstackidentity.basicauth

object BasicAuthUtils {
  def extractCreds(decoded:Array[Byte]): (String, String) = {
    val decodedString = new String(decoded)
    val username = decodedString.split(":").head
    val password = decodedString.replace(s"$username:", "")
    (username, password)
  }

}
