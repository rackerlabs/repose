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

import org.apache.http.entity.ContentType
import org.apache.http.protocol.{BasicHttpContext, HttpContext}
import org.hamcrest.Matchers.{anything, arrayContaining, equalTo}
import org.hamcrest.{Description, Matcher, SelfDescribing}
import org.junit.runner.RunWith
import org.mockito.Matchers.{any, anyObject, anyString}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

import scala.Function.tupled

@RunWith(classOf[JUnitRunner])
class HttpContextMatchersTest extends FunSpec with Matchers with MockitoSugar {

  describe("hasAttribute") {
    describe("matches") {
      Seq(
        ("testKey", anything(), makeContext(Map("testKey" -> "testValue")), true),
        ("testKey", anything(), makeContext(Map("badKey" -> "testValue")), false),
        ("testKey", equalTo("testValue"), makeContext(Map("testKey" -> "badValue")), false),
        ("testKey", arrayContaining("testValue"), makeContext(Map("testKey" -> Array.empty)), false))
        .foreach { case (key: String, valueMatcher: Matcher[_], context: HttpContext, matches: Boolean) =>
          it(s"should return $matches when expecting attribute [$key: $valueMatcher] on $context") {
            HttpContextMatchers.hasAttribute(key, valueMatcher).matches(context) shouldBe matches
          }
        }
    }

    describe("describeTo") {
      it("should append to the description") {
        val key = "testKey"
        val valueMatcher = equalTo(ContentType.APPLICATION_JSON.getMimeType)
        val description = mock[Description]

        when(description.appendText(anyString())).thenReturn(description)
        when(description.appendValue(anyObject())).thenReturn(description)
        when(description.appendDescriptionOf(any[SelfDescribing])).thenReturn(description)

        HttpContextMatchers.hasAttribute(key, valueMatcher).describeTo(description)

        verify(description).appendText("attributes include [")
        verify(description).appendValue(key)
        verify(description).appendText(": ")
        verify(description).appendDescriptionOf(valueMatcher)
        verify(description).appendText("]")
      }
    }

    describe("describeMismatch") {
      it("should append to the description on mismatch") {
        val key = "testKey"
        val actualContext = makeContext(Map.empty)
        val description = mock[Description]

        when(description.appendText(anyString())).thenReturn(description)
        when(description.appendValue(any[Any])).thenReturn(description)

        HttpContextMatchers.hasAttribute(key, anything()).describeMismatch(actualContext, description)

        verify(description).appendText("attribute with key ")
        verify(description).appendValue(key)
        verify(description).appendText(" was ")
        verify(description).appendValue(actualContext.getAttribute(key))
      }
    }
  }

  def makeContext(map: Map[String, Any]): HttpContext = {
    val context = new BasicHttpContext()
    map.foreach(tupled((key, value) => context.setAttribute(key, value)))
    context
  }
}
