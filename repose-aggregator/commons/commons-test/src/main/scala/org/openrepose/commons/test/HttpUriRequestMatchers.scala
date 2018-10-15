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
package org.openrepose.commons.test

import java.net.URI

import org.apache.http.client.methods.HttpUriRequest
import org.hamcrest.{Description, Factory, Matcher, TypeSafeMatcher}

/**
  * A factory for [[HttpUriRequest]] [[org.hamcrest.Matcher]]s.
  */
object HttpUriRequestMatchers {
  @Factory
  def hasMethod(method: String): Matcher[HttpUriRequest] = new HasMethod(method)

  @Factory
  def hasUri(uri: URI): Matcher[HttpUriRequest] = new HasUri(uri)

  @Factory
  def hasHeader(name: String, valueMatcher: Matcher[_ >: String]): Matcher[HttpUriRequest] = new HasHeader(name, valueMatcher)

  private class HasMethod(method: String) extends TypeSafeMatcher[HttpUriRequest] {
    override def matchesSafely(actualRequest: HttpUriRequest): Boolean = {
      method.equals(actualRequest.getMethod)
    }

    override def describeTo(description: Description): Unit = {
      description
        .appendText("method should be ")
        .appendValue(method)
    }

    override def describeMismatchSafely(actualRequest: HttpUriRequest, mismatchDescription: Description): Unit = {
      mismatchDescription
        .appendText(" was ")
        .appendValue(actualRequest.getMethod)
    }
  }

  private class HasUri(uri: URI) extends TypeSafeMatcher[HttpUriRequest] {
    override def matchesSafely(actualRequest: HttpUriRequest): Boolean = {
      uri.equals(actualRequest.getURI)
    }

    override def describeTo(description: Description): Unit = {
      description
        .appendText("URI should be ")
        .appendValue(uri)
    }

    override def describeMismatchSafely(actualRequest: HttpUriRequest, mismatchDescription: Description): Unit = {
      mismatchDescription
        .appendText(" was ")
        .appendValue(actualRequest.getURI)
    }
  }

  private class HasHeader(name: String, valueMatcher: Matcher[_ >: String]) extends TypeSafeMatcher[HttpUriRequest] {
    override def matchesSafely(actualRequest: HttpUriRequest): Boolean = {
      actualRequest.getHeaders(name).map(_.getValue).exists(valueMatcher.matches)
    }

    override def describeTo(description: Description): Unit = {
      description
        .appendText("headers include [")
        .appendValue(name)
        .appendText(": ")
        .appendDescriptionOf(valueMatcher)
        .appendText("]")
    }

    override def describeMismatchSafely(actualRequest: HttpUriRequest, mismatchDescription: Description): Unit = {
      mismatchDescription
        .appendText("headers were ")
        .appendValueList("[", ", ", "]", actualRequest.getAllHeaders)
    }
  }

}
