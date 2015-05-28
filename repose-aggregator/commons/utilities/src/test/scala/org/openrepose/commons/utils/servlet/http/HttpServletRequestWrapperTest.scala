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
package org.openrepose.commons.utils.servlet.http

import java.util

import com.mockrunner.mock.web.MockHttpServletRequest
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

import scala.collection.JavaConverters._

/**
 * Created with IntelliJ IDEA.
 * User: adrian
 * Date: 5/27/15
 * Time: 10:39 AM
 */
@RunWith(classOf[JUnitRunner])
class HttpServletRequestWrapperTest extends FunSpec with BeforeAndAfter with Matchers {
  var wrappedRequest :HttpServletRequestWrapper = _

  before {
    val mockRequest = new MockHttpServletRequest
    mockRequest.addHeader("foo", "bar")
    mockRequest.addHeader("foo", "baz")
    mockRequest.addHeader("banana-phone", "ring,ring,ring")
    mockRequest.addHeader("cup", "blue,orange?q=0.5")
    mockRequest.addHeader("ornament", "weird penguin?q=0.8")
    mockRequest.addHeader("ornament", "santa?q=0.9")
    mockRequest.addHeader("ornament", "droopy tree?q=0.3")
    mockRequest.addHeader("thumbs", "2")
    wrappedRequest = new HttpServletRequestWrapper(mockRequest)
  }

  describe("the getHeaderNamesList method") {
    it("should return a list of all the header names") {
      wrappedRequest.getHeaderNamesList.asScala should contain theSameElementsAs List("foo", "banana-phone", "cup", "ornament", "thumbs")
    }
  }

  describe("the getHeadersList method") {
    Map("foo" -> List("bar", "baz"),
      "banana-phone" -> List("ring,ring,ring"),
      "cup" -> List("blue,orange?q=0.5"),
      "ornament" -> List("weird penguin?q=0.8", "santa?q=0.9", "droopy tree?q=0.3"),
      "thumbs" -> List("2")).foreach { case (headerName, headerValues) =>
      it(s"should return the appropriate elements for header: $headerName") {

        val returnedValues: util.List[String] = wrappedRequest.getHeadersList(headerName)
        returnedValues.size shouldBe headerValues.size
        returnedValues.asScala should contain theSameElementsAs headerValues
      }
    }
  }

  describe("the addHeader method") {
    it("Should increase the size of the HttpServletRequestWrapper by 1") {
      val sizeOfHeaderList = wrappedRequest.getHeadersList("foo").size
      wrappedRequest.addHeader("foo", "foo")
      wrappedRequest.getHeadersList("foo").size shouldBe sizeOfHeaderList + 1
    }
  }

  describe("the addHeader method with quality") {
    it("Should increase the size of the HttpServletRequestWrapper by 1 with a quality value") {
      val sizeOfHeaderList = wrappedRequest.getHeadersList("foo").size
      wrappedRequest.addHeader("foo", "foo", 0.5)
      wrappedRequest.getHeadersList("foo").size shouldBe sizeOfHeaderList + 1
    }
  }

  describe("the removeHeader method") {
    List("foo", "banana-phone", "cup", "ornament", "thumbs").foreach { case (headerName) =>
      it(s"Should remove the header from the wrapper: $headerName") {
        wrappedRequest.removeHeader(headerName)
        wrappedRequest.getHeadersList(headerName).size shouldBe 0
      }
    }
  }

  describe("the getPreffereedHeader method") {
    it("Should return value with largest quality value for ornament") {
      val preferred = wrappedRequest.getPreferredHeader("ornament")
      preferred shouldBe "santa"
    }
    it("Should return value with largest quality value for cup") {
      val preferred = wrappedRequest.getPreferredHeader("cup")
      preferred shouldBe "blue"
    }
  }

}
