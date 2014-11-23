package com.rackspace.httpdelegation

import java.text.ParseException

import scala.util.{Failure, Try}

/** The API for the HTTP delegation library. */
trait HttpDelegationManager {

  /** Generates the appropriate headers to add to a HTTP request to support delegation.
    *
    * @param statusCode the status code which would have been applied if not for delegation
    * @param message a description of why the status code would have been applied
    * @param quality a value, between 0 and 1, which is used to determine the order of importance for various
    *                delegations
    * @return a map of headers to be added to a HTTP request
    */
  def buildDelegationHeaders(statusCode: Int, component: String, message: String, quality: Double): Map[String, List[String]] = {
    assume(component != null, "Component cannot be null")
    assume(message != null, "Message cannot be null")

    Map[String, List[String]](
      HttpDelegationHeaders.Delegated -> List(
        "status_code=" + statusCode + "`component=" + component + "`message=" + message + ";q=" + quality
      )
    )
  }

  /** Constructs a case class object which holds each component of the value of a delegation header.
    *
    * @param delegationHeaderValue the value of the delegation header to be parsed
    * @return a [[HttpDelegationHeaderBean]] containing each parsed component
    */
  def parseDelegationHeader(delegationHeaderValue: String): Try[HttpDelegationHeaderBean] = {
    val parsingRegex = """status_code=(\d\d\d)`component=(.*)`message=((?:(?!;q=).)*)(?:;q=((?:\d+(?:\.\d*)?)|(?:\.\d+)))?""".r("statusCode", "component", "message", "quality")

    parsingRegex.findFirstMatchIn(delegationHeaderValue) match {
      case Some(regexMatch) =>
        Try(
          new HttpDelegationHeaderBean(
            regexMatch.group("statusCode").toInt,
            regexMatch.group("component"),
            regexMatch.group("message"),
            Option(regexMatch.group("quality")).map(_.toDouble).getOrElse(1.0)
          )
        )
      case None =>
        Failure(new ParseException("The delegation header value could not be parsed", -1))
    }
  }
}
