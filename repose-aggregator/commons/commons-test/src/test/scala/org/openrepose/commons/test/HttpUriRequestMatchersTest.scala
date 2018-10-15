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

import org.apache.http.HttpHeaders
import org.apache.http.client.methods.{HttpGet, HttpPost, HttpUriRequest, RequestBuilder}
import org.apache.http.entity.ContentType
import org.hamcrest.Matchers.{anything, containsString, equalTo}
import org.hamcrest.{Description, Matcher, SelfDescribing}
import org.junit.runner.RunWith
import org.mockito.Matchers.{any, anyObject, anyString}
import org.mockito.Mockito.{verify, when}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class HttpUriRequestMatchersTest extends FunSpec with Matchers with MockitoSugar {

  describe("hasMethod") {
    describe("matches") {
      Seq(
        (HttpGet.METHOD_NAME, RequestBuilder.get().build(), true),
        (HttpGet.METHOD_NAME, RequestBuilder.post().build(), false),
        (HttpPost.METHOD_NAME, RequestBuilder.get().build(), false),
        (HttpPost.METHOD_NAME, RequestBuilder.post().build(), true))
        .foreach { case (method: String, request: HttpUriRequest, matches: Boolean) =>
          it(s"should return $matches when expecting $method with actual ${request.getMethod}") {
            HttpUriRequestMatchers.hasMethod(method).matches(request) shouldBe matches
          }
        }
    }

    describe("describeTo") {
      it("should append to the description") {
        val description = mock[Description]

        when(description.appendText(anyString())).thenReturn(description)
        when(description.appendValue(anyObject())).thenReturn(description)

        HttpUriRequestMatchers.hasMethod(HttpGet.METHOD_NAME).describeTo(description)

        verify(description).appendText("method should be ")
        verify(description).appendValue(HttpGet.METHOD_NAME)
      }
    }

    describe("describeMismatch") {
      it("should append to the description on mismatch") {
        val actualRequest = RequestBuilder.post().build()
        val description = mock[Description]

        when(description.appendText(anyString())).thenReturn(description)
        when(description.appendValue(anyObject())).thenReturn(description)

        HttpUriRequestMatchers.hasMethod(HttpGet.METHOD_NAME).describeMismatch(actualRequest, description)

        verify(description).appendText(" was ")
        verify(description).appendValue(actualRequest.getMethod)
      }
    }
  }

  describe("hasUri") {
    describe("matches") {
      Seq(
        (URI.create("http://localhost/"), RequestBuilder.get("http://localhost/").build(), true),
        (URI.create("http://localhost/"), RequestBuilder.get("http://localhost/foo").build(), false),
        (URI.create("https://localhost/"), RequestBuilder.get("http://localhost/").build(), false),
        (URI.create("http://localhost/?a=b&c=d"), RequestBuilder.get("http://localhost/?a=b&c=d").build(), true),
        (URI.create("http://localhost/?c=d&a=b"), RequestBuilder.get("http://localhost/?a=b&c=d").build(), false))
        .foreach { case (uri: URI, request: HttpUriRequest, matches: Boolean) =>
          it(s"should return $matches when expecting $uri with actual ${request.getURI}") {
            HttpUriRequestMatchers.hasUri(uri).matches(request) shouldBe matches
          }
        }
    }

    describe("describeTo") {
      it("should append to the description") {
        val uri = URI.create("http://localhost/")
        val description = mock[Description]

        when(description.appendText(anyString())).thenReturn(description)
        when(description.appendValue(anyObject())).thenReturn(description)

        HttpUriRequestMatchers.hasUri(uri).describeTo(description)

        verify(description).appendText("URI should be ")
        verify(description).appendValue(uri)
      }
    }

    describe("describeMismatch") {
      it("should append to the description on mismatch") {
        val uri = URI.create("http://localhost/")
        val actualRequest = RequestBuilder.get(uri).build()
        val description = mock[Description]

        when(description.appendText(anyString())).thenReturn(description)
        when(description.appendValue(anyObject())).thenReturn(description)

        HttpUriRequestMatchers.hasUri(uri).describeMismatch(actualRequest, description)

        verify(description).appendText(" was ")
        verify(description).appendValue(actualRequest.getURI)
      }
    }
  }

  describe("hasHeader") {
    describe("matches") {
      Seq(
        (HttpHeaders.ACCEPT, equalTo(ContentType.APPLICATION_JSON.getMimeType), RequestBuilder.get().addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType).build(), true),
        (HttpHeaders.ACCEPT, containsString("json"), RequestBuilder.get().addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType).build(), true),
        (HttpHeaders.ACCEPT, equalTo(ContentType.APPLICATION_JSON.getMimeType), RequestBuilder.get().addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_XML.getMimeType).build(), false),
        (HttpHeaders.ACCEPT, containsString("json"), RequestBuilder.get().addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_XML.getMimeType).build(), false),
        (HttpHeaders.ACCEPT, equalTo(ContentType.APPLICATION_JSON.getMimeType), RequestBuilder.get().addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_XML.getMimeType).addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType).build(), true))
        .foreach { case (name: String, valueMatcher: Matcher[_], request: HttpUriRequest, matches: Boolean) =>
          it(s"should return $matches when expecting header [$name: $valueMatcher] with actual headers ${request.getAllHeaders}") {
            HttpUriRequestMatchers.hasHeader(name, valueMatcher).matches(request) shouldBe matches
          }
        }
    }

    describe("describeTo") {
      it("should append to the description") {
        val valueMatcher = equalTo(ContentType.APPLICATION_JSON.getMimeType)
        val description = mock[Description]

        when(description.appendText(anyString())).thenReturn(description)
        when(description.appendValue(anyObject())).thenReturn(description)
        when(description.appendDescriptionOf(any[SelfDescribing])).thenReturn(description)

        HttpUriRequestMatchers.hasHeader(HttpHeaders.ACCEPT, valueMatcher).describeTo(description)

        verify(description).appendText("headers include [")
        verify(description).appendValue(HttpHeaders.ACCEPT)
        verify(description).appendText(": ")
        verify(description).appendDescriptionOf(valueMatcher)
        verify(description).appendText("]")
      }
    }

    describe("describeMismatch") {
      it("should append to the description on mismatch") {
        val actualRequest = RequestBuilder.get().build()
        val description = mock[Description]

        when(description.appendText(anyString())).thenReturn(description)
        when(description.appendValueList(anyString(), anyString(), anyString(), any[Iterable[_]])).thenReturn(description)

        HttpUriRequestMatchers.hasHeader(HttpHeaders.ACCEPT, anything()).describeMismatch(actualRequest, description)

        verify(description).appendText("headers were ")
        verify(description).appendValueList("[", ", ", "]", actualRequest.getAllHeaders)
      }
    }
  }
}
