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

package org.openrepose.commons.utils.string

import java.util.regex.{Matcher, Pattern}

import scala.language.implicitConversions

/**
  * Allows the implicit conversion to RegexString to be mixed-in.
  */
trait RegexStringOperators {
  implicit def stringToRegexString(str: String): RegexString = new RegexString(str)
}

/**
  * Allows the implicit conversion to RegexString to be imported.
  */
object RegexString extends RegexStringOperators {}

class RegexString(val string: String) {

  val pattern: Pattern = string.r.pattern

  /**
    * Attempts to match this, as a regular expression, against another string.
    *
    * @param other the string to attempt to match against
    * @return true if other fully matches, false otherwise
    */
  def =~(other: String): Boolean = ==~(other).matches

  /**
    * Attempts to create a matcher with this, as a regular expression, against another string.
    *
    * @param other the string to create a matcher for
    * @return a matcher for this against other
    */
  def ==~(other: String): Matcher = pattern.matcher(other)
}
