package com.rackspace.httpdelegation

import java.util

import com.rackspace.httpdelegation.impl.HttpDelegationManagerImpl

import scala.collection.JavaConverters._

/** A Java interface into the [[HttpDelegationManagerImpl]].
 *
 * This object deals in Java objects to simplify interoperability with Java code which utilizes this library.
 */
object JavaDelegationManagerProxy {

  def buildDelegationHeaders(statusCode: Int, component: String, message: String, quality: Double): util.Map[String, util.List[String]] = {
    val javaMap = new util.HashMap[String, util.List[String]]()

    HttpDelegationManagerImpl.buildDelegationHeaders(statusCode, component, message, quality).foreach { case (key, values) =>
      javaMap.put(key, values.asJava)
    }

    javaMap
  }
}