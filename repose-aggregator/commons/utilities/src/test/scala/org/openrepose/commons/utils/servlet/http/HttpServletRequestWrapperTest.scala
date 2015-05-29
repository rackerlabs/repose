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

import com.mockrunner.mock.web.MockHttpServletRequest
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
 * Created with IntelliJ IDEA.
 * User: adrian
 * Date: 5/27/15
 * Time: 10:39 AM
 */
@RunWith(classOf[JUnitRunner])
class HttpServletRequestWrapperTest extends FunSpec with BeforeAndAfter with Matchers {
  var wrappedRequest :HttpServletRequestWrapper = _
  val headerMap :Map[String, List[String]] = Map(
    "foo" -> List("bar", "baz"),
    "banana-phone" -> List("ring,ring,ring"),
    "cup" -> List("blue,orange?q=0.5"),
    "ornament" -> List("weird penguin?q=0.8", "santa?q=0.9", "droopy tree?q=0.3"),
    "thumbs" -> List("2"),
    "awesomeTime" -> List("Fri, 29 May 2015 12:12:12 CST"))

  before {
    val mockRequest = new MockHttpServletRequest
    headerMap.foreach { case (headerName, headerValues) =>
      headerValues.foreach { headerValue =>
        mockRequest.addHeader(headerName, headerValue)
      }
    }
    wrappedRequest = new HttpServletRequestWrapper(mockRequest)
  }

  describe("the getHeaderNames method") {
    it("should return all the header names from the original request") {
      wrappedRequest.getHeaderNames.asScala.toList should contain theSameElementsAs headerMap.keys
    }

    it("should return all the header names including the ones that were added") {
      pending
      wrappedRequest.addHeader("butts", "butts")
      wrappedRequest.getHeaderNames.asScala.toList should contain theSameElementsAs headerMap.keys ++ "butts"
    }

    it("should return a list that is missing any deleted headers") {
      pending
      wrappedRequest.removeHeader("foo")
      wrappedRequest.getHeaderNames.asScala.toList should contain theSameElementsAs headerMap.keys.filterNot( _ == "foo")
    }
  }

  describe("the getIntHeader method") {
    it("should return an int value when one is available") {
      wrappedRequest.getIntHeader("thumbs") shouldBe 2
    }

    it("should return -1 when the header doesn't exist") {
      wrappedRequest.getIntHeader("butts") shouldBe -1
    }

    it("should throw an exception when the header isn't an int") {
      a [NumberFormatException] should be thrownBy wrappedRequest.getIntHeader("cup")
    }

    it("should provide a value for an added header") {
      pending
      wrappedRequest.addHeader("butts", "42")
      wrappedRequest.getIntHeader("butts") shouldBe 42
    }

    it("should not return the value for a deleted header") {
      pending
      wrappedRequest.removeHeader("thumbs")
      wrappedRequest.getIntHeader("thumbs") shouldBe -1
    }
  }

  describe("the getHeaders method") {
    headerMap.foreach { case (headerName, headerValues) =>
      it(s"should return the appropriate elements for header: $headerName") {
        val returnedValues: List[String] = wrappedRequest.getHeaders(headerName).asScala.toList
        returnedValues.size shouldBe headerValues.size
        returnedValues should contain theSameElementsAs headerValues
      }
    }

    it("should increase returnValue list by 1 when another header is added") {
      pending
      val returnedValuesSize = wrappedRequest.getHeaders("foo").asScala.toList.size
      wrappedRequest.addHeader("foo", "foo")
      wrappedRequest.getHeaders("foo").asScala.toList.size shouldBe returnedValuesSize + 1
    }

    it("should return an empty list for unknown header") {
      wrappedRequest.getHeaders("notAHeader").asScala.toList shouldBe empty
    }

    it("should return all values for a header including added ones") {
      pending
      val sizeOfHeaderList = wrappedRequest.getHeadersList("foo").size
      wrappedRequest.addHeader("foo", "foo")
      val returnedValues: List[String] = wrappedRequest.getHeaders("foo").asScala.toList
      returnedValues.size shouldBe sizeOfHeaderList + 1
      returnedValues should contain ("foo")
    }

    it("should return value for a brand new header") {
      pending
      wrappedRequest.addHeader("butts", "butts")
      wrappedRequest.getHeaders("butts").asScala.toList should contain ("butts")
    }

    it("should return an empty list for a deleted header") {
      pending
      wrappedRequest.removeHeader("foo")
      wrappedRequest.getHeaders("foo").asScala.toList shouldBe empty
    }
  }

  describe("the getDateHeader method") {
    it("should return the date as a long") {
      wrappedRequest.getDateHeader("awesomeTime") shouldBe 1432923132000L
    }

    it("should return -1 because of no date header") {
      wrappedRequest.getDateHeader("notADate") shouldBe -1
    }

    it("should throw IllegalArgumentException if value is not a Date") {
      an [IllegalArgumentException] should be thrownBy wrappedRequest.getIntHeader("cup")
    }
  }

  describe("the getHeader method") {
    it("should return the first value for a given header") {
      wrappedRequest.getHeader("foo") shouldBe "bar"
    }

    it("should return null for an unknown header") {
      wrappedRequest.getHeader("butts") shouldBe null
    }
  }

  describe("the getHeaderNamesList method") {
    it("should return a list of all the header names") {
      wrappedRequest.getHeaderNamesList.asScala should contain theSameElementsAs headerMap.keys
    }

    it("should not contain a header name that was not added") {
      wrappedRequest.getHeaderNamesList.asScala shouldNot contain theSameElementsAs List("notAHeader")
    }
  }

  describe("the getHeadersList method") {
    headerMap.foreach { case (headerName, headerValues) =>
      it(s"should return the appropriate elements for header: $headerName") {
        val returnedValues: List[String] = wrappedRequest.getHeadersList(headerName).asScala.toList
        returnedValues.size shouldBe headerValues.size
        returnedValues should contain theSameElementsAs headerValues
      }
    }

    it("should return an empty list") {
      wrappedRequest.getHeadersList("notAHeader").size shouldBe 0
    }
  }

  describe("the addHeader method") {
    it("Should add an additional value to an existing header") {
      pending
      val sizeOfHeaderList = wrappedRequest.getHeadersList("foo").size
      wrappedRequest.addHeader("foo", "foo")
      val returnedValues: List[String] = wrappedRequest.getHeadersList("foo").asScala.toList
      returnedValues.size shouldBe sizeOfHeaderList + 1
      returnedValues should contain ("foo")
    }

    it("Should add a brand new header if it didn't exist before") {
      pending
      wrappedRequest.addHeader("butts", "butts")
      val returnedValues: mutable.Buffer[String] = wrappedRequest.getHeadersList("butts").asScala
      returnedValues.size shouldBe 1
      returnedValues should contain ("butts")
    }
  }

  describe("the addHeader method with quality") {
    it("Should add an additional value to an existing header") {
      pending
      val sizeOfHeaderList = wrappedRequest.getHeadersList("foo").size
      wrappedRequest.addHeader("foo", "foo", 0.5)
      wrappedRequest.getHeadersList("foo").size shouldBe sizeOfHeaderList + 1
    }

    it("Should add a brand new header if it didn't exist before") {
      pending
      wrappedRequest.addHeader("butts", "butts", 0.5)
      val returnedValues: mutable.Buffer[String] = wrappedRequest.getHeadersList("butts").asScala
      returnedValues.size shouldBe 1
      returnedValues should contain ("butts")
    }
  }

  describe("the getPreferredSplittableHeader method") {
    it("Should return value with largest quality value for cup") {
      pending
      val preferred = wrappedRequest.getPreferredSplittableHeader("cup")
      preferred shouldBe "blue"
    }
  }

  describe("the removeHeader method") {
    headerMap.keys.foreach { headerName =>
      it(s"Should remove the header from the wrapper: $headerName") {
        pending
        wrappedRequest.removeHeader(headerName)
        wrappedRequest.getHeadersList(headerName).size shouldBe 0
      }
    }

    it("should try to remove a header that does not exist") {
      pending
    }
  }

  describe("the getPreferredHeader method") {
    it("Should return value with largest quality value for ornament") {
      pending
      val preferred = wrappedRequest.getPreferredHeader("ornament")
      preferred shouldBe "santa"
    }
  }

}
