package com.rackspace.httpdelegation

import java.text.ParseException
import java.util

import scala.collection.JavaConverters._
import scala.util.{Failure, Success}

/** A Java interface into the [[HttpDelegationManager]].
  *
  * This object deals in Java objects to simplify interoperability with Java code which utilizes this library.
  */
object JavaDelegationManagerProxy {

  private object HttpDelegationManagerProxy extends HttpDelegationManager

  /** Generates the appropriate headers to add to a HTTP request to support delegation.
    *
    * @param statusCode the status code which would have been applied if not for delegation
    * @param message a description of why the status code would have been applied
    * @param quality a value, between 0 and 1, which is used to determine the order of importance for various
    *                delegations
    * @return a map of headers to be added to a HTTP request
    */
  def buildDelegationHeaders(statusCode: Int, component: String, message: String, quality: Double): util.Map[String, util.List[String]] = {
    val javaMap = new util.HashMap[String, util.List[String]]()

    HttpDelegationManagerProxy.buildDelegationHeaders(statusCode, component, message, quality).foreach { case (key, values) =>
      javaMap.put(key, values.asJava)
    }

    javaMap
  }

  /** Constructs a case class object which holds each component of the value of a delegation header.
    *
    * @param delegationHeaderValue the value of the delegation header to be parsed
    * @return a [[HttpDelegationHeader]] containing each parsed component
    */
  @throws(classOf[ParseException])
  def parseDelegationHeader(delegationHeaderValue: String): HttpDelegationHeader = {
    HttpDelegationManagerProxy.parseDelegationHeader(delegationHeaderValue) match {
      case Success(delegationHeaderBean) =>
        delegationHeaderBean
      case Failure(e) =>
        throw new ParseException(e.getMessage, -1)
    }
  }
}
