/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package com.rackspace.httpdelegation

import java.text.ParseException

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
      HttpDelegationHeaderNames.Delegated -> List(
        "status_code=" + statusCode + "`component=" + component + "`message=" + message + ";q=" + quality
      )
    )
  }

  /** Constructs a case class object which holds each component of the value of a delegation header.
    *
    * @param delegationHeaderValue the value of the delegation header to be parsed
    * @return a [[HttpDelegationHeader]] containing each parsed component
    */
  def parseDelegationHeader(delegationHeaderValue: String): Try[HttpDelegationHeader] = {
    // TODO: Performance concern due to negative lookahead in the message
    val parsingRegex = """^\s*status_code=(\d\d\d)`component=(.*)`message=((?:(?!;q=).)*)(?:;q=((?:\d+(?:\.\d*)?)|(?:\.\d+)))?\s*$""".r("statusCode", "component", "message", "quality")

    parsingRegex.findFirstMatchIn(delegationHeaderValue) match {
      case Some(regexMatch) =>
        Try(
          new HttpDelegationHeader(
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
