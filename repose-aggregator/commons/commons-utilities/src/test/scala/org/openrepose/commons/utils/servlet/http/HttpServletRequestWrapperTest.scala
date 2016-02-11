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

import java.io.{BufferedReader, IOException}
import java.util
import javax.servlet.ServletInputStream

import com.mockrunner.mock.web.MockHttpServletRequest
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.io.Source

@RunWith(classOf[JUnitRunner])
class HttpServletRequestWrapperTest extends FunSpec with BeforeAndAfter with Matchers {
  val queryParamMap: Map[String, Array[String]] = Map(
    "foo" -> Array("bar", "baz"))
  val headerMap: Map[String, List[String]] = Map(
    "foo" -> List("bar", "baz"),
    "banana-phone" -> List("ring,ring,ring"),
    "cup" -> List("blue,orange;q=0.5"),
    "ornament" -> List("weird penguin;q=0.8", "santa;q=0.9", "droopy tree;q=0.3"),
    "thumbs" -> List("2"),
    "abc" -> List("1,2,3"),
    "awesomeTime" -> List("Fri, 29 May 2015 12:12:12 CST"))
  var wrappedRequest: HttpServletRequestWrapper = _

  before {
    val mockRequest = new MockHttpServletRequest
    queryParamMap foreach { case (parameterKey, parameterValues) =>
      mockRequest.setupAddParameter(parameterKey, parameterValues)
      mockRequest.setQueryString(Option(mockRequest.getQueryString).getOrElse("") + parameterValues.map(value => parameterKey + "=" + value).mkString("&"))
    }
    headerMap foreach { case (headerName, headerValues) =>
      headerValues foreach { headerValue =>
        mockRequest.addHeader(headerName, headerValue)
      }
    }
    mockRequest.setBodyContent("i like pie\nyummy yummy\n")
    wrappedRequest = new HttpServletRequestWrapper(mockRequest)
  }

  describe("the getInputStream method") {
    it("tests that the body matches") {
      val output: String = Source.fromInputStream(wrappedRequest.getInputStream).mkString
      output shouldBe "i like pie\nyummy yummy\n"
    }

    it("tests the body is consumed after one call to getInputStream") {
      val output: String = Source.fromInputStream(wrappedRequest.getInputStream).mkString
      output shouldBe "i like pie\nyummy yummy\n"
      val output2: String = Source.fromInputStream(wrappedRequest.getInputStream).mkString
      output2 shouldBe ""
    }

    it("tests IllegalStateException is thrown if getReader is already called") {
      val br: BufferedReader = wrappedRequest.getReader
      val output: String = Stream.continually(br.readLine()).takeWhile(_ != null).mkString
      output shouldBe "i like pieyummy yummy"
      an[IllegalStateException] should be thrownBy Source.fromInputStream(wrappedRequest.getInputStream).mkString
    }

    it("tests IOException is thrown") {
      val is: ServletInputStream = wrappedRequest.getInputStream
      is.close
      an[IOException] should be thrownBy is.reset
    }
  }

  describe("the getReader method") {
    it("tests that the body matches") {
      val br: BufferedReader = wrappedRequest.getReader
      val output: String = Stream.continually(br.readLine()).takeWhile(_ != null).mkString
      output shouldBe "i like pieyummy yummy"
    }

    it("tests the body is consumed after one call to getReader") {
      val br: BufferedReader = wrappedRequest.getReader
      val output: String = Stream.continually(br.readLine()).takeWhile(_ != null).mkString
      output shouldBe "i like pieyummy yummy"
      val br2: BufferedReader = wrappedRequest.getReader
      val output2: String = Stream.continually(br2.readLine()).takeWhile(_ != null).mkString
      output2 shouldBe ""
    }

    it("tests IllegalStateException is thrown if getInputStream is already called") {
      val output: String = Source.fromInputStream(wrappedRequest.getInputStream).mkString
      output shouldBe "i like pie\nyummy yummy\n"
      an[IllegalStateException] should be thrownBy wrappedRequest.getReader
    }

    it("tests IOException is thrown") {
      val br: BufferedReader = wrappedRequest.getReader
      br.close
      an[IOException] should be thrownBy br.reset
    }
  }

  describe("the getHeadersNamesSet method") {
    it("should return all the header names from the original request") {
      wrappedRequest.getHeaderNamesScala should contain theSameElementsAs headerMap.keys
    }

    it("should return all the header names including the ones that were added") {
      wrappedRequest.addHeader("butts", "butts")
      wrappedRequest.getHeaderNamesScala should contain theSameElementsAs headerMap.keys ++ List("butts")
    }

    it("should return a list that is missing any deleted headers") {
      wrappedRequest.removeHeader("foo")
      wrappedRequest.getHeaderNamesScala should contain theSameElementsAs headerMap.keys.filterNot(_ == "foo")
    }

    it("should return the same list when Foo is added") {
      wrappedRequest.addHeader("Foo", "foo")
      wrappedRequest.getHeaderNamesScala should contain theSameElementsAs headerMap.keys
    }
  }

  describe("the getHeaderNames method") {
    it("should return all the header names from the original request") {
      wrappedRequest.getHeaderNames.asScala.toList should contain theSameElementsAs headerMap.keys
    }

    it("should return all the header names including the ones that were added") {
      wrappedRequest.addHeader("butts", "butts")
      wrappedRequest.getHeaderNames.asScala.toList should contain theSameElementsAs headerMap.keys ++ List("butts")
    }

    it("should return a list that is missing any deleted headers") {
      wrappedRequest.removeHeader("foo")
      wrappedRequest.getHeaderNames.asScala.toList should contain theSameElementsAs headerMap.keys.filterNot(_ == "foo")
    }

    it("should return the same list when Foo is added") {
      wrappedRequest.addHeader("Foo", "foo")
      wrappedRequest.getHeaderNames.asScala.toList should contain theSameElementsAs headerMap.keys
    }

    it("should return empty enumeration") {
      val mockRequest = new MockHttpServletRequest
      wrappedRequest = new HttpServletRequestWrapper(mockRequest)
      wrappedRequest.getHeaderNames.hasMoreElements shouldBe false
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
      a[NumberFormatException] should be thrownBy wrappedRequest.getIntHeader("cup")
    }

    it("should provide a value for an added header") {
      wrappedRequest.addHeader("butts", "42")
      wrappedRequest.getIntHeader("butts") shouldBe 42
    }

    it("should not return the value for a deleted header") {
      wrappedRequest.removeHeader("thumbs")
      wrappedRequest.getIntHeader("thumbs") shouldBe -1
    }

    it("should return a value for a replaced header") {
      wrappedRequest.replaceHeader("foo", "42")
      wrappedRequest.getIntHeader("foo") shouldBe 42
    }

    it("should return a value for an appended header with no other values") {
      wrappedRequest.appendHeader("new-header", "42")
      wrappedRequest.getIntHeader("new-header") shouldBe 42
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

    it("should return an empty list for unknown header") {
      wrappedRequest.getHeaders("notAHeader").asScala.toList shouldBe empty
    }

    it("should return all values for a header including added ones") {
      val headerList = wrappedRequest.getHeadersList("foo")
      wrappedRequest.addHeader("foo", "foo")
      val returnedValues: List[String] = wrappedRequest.getHeaders("foo").asScala.toList
      returnedValues.size shouldBe headerList.size + 1
      returnedValues should contain theSameElementsAs headerList.asScala ++ List("foo")
    }

    it("should return value for a brand new header") {
      wrappedRequest.addHeader("butts", "butts")
      wrappedRequest.getHeaders("butts").asScala.toList should contain theSameElementsAs List("butts")
    }

    it("should return an empty list for a deleted header") {
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
      an[IllegalArgumentException] should be thrownBy wrappedRequest.getIntHeader("cup")
    }

    it("should provide a value for an added header in RFC1123") {
      wrappedRequest.addHeader("newAwesomeTime", "Thu, 1 Jan 1970 00:00:00 GMT")
      wrappedRequest.getDateHeader("newAwesomeTime") shouldBe 0L
    }

    it("should provide a value for an added header in RFC1036") {
      wrappedRequest.addHeader("newAwesomeTime", "Friday, 29-May-2015 12:12:12 CST")
      wrappedRequest.getDateHeader("newAwesomeTime") shouldBe 1432923132000L
    }

    it("should provide a value for an added header in asctime") {
      wrappedRequest.addHeader("newAwesomeTime", "Thu Jan 1 00:00:00 1970")
      wrappedRequest.getDateHeader("newAwesomeTime") shouldBe 0L
    }

    it("should return -1 for a removed header") {
      wrappedRequest.removeHeader("awesomeTime")
      wrappedRequest.getDateHeader("awesomeTime") shouldBe -1
    }

    it("should return 0L when header replaced") {
      wrappedRequest.replaceHeader("awesomeTime", "Thu, 1 Jan 1970 00:00:00 GMT")
      wrappedRequest.getDateHeader("awesomeTime") shouldBe 0L
    }

    it("should provide a value for a new appended header in RFC1123") {
      wrappedRequest.appendHeader("newAwesomeTime", "Thu, 1 Jan 1970 00:00:00 GMT")
      wrappedRequest.getDateHeader("newAwesomeTime") shouldBe 0L
    }
  }

  describe("the getHeader method") {
    it("should return the first value for a given header") {
      wrappedRequest.getHeader("foo") shouldBe "bar"
    }

    it("should return null for an unknown header") {
      wrappedRequest.getHeader("butts") shouldBe null
    }

    it("should continue to return the first original value even if a value has been added") {
      wrappedRequest.addHeader("foo", "foo")
      wrappedRequest.getHeader("foo") shouldBe "bar"
    }

    it("should return the first value for entirely new headers") {
      wrappedRequest.addHeader("butts", "butts")
      wrappedRequest.addHeader("butts", "shazbot")
      wrappedRequest.getHeader("butts") shouldBe "butts"
    }

    it("should return null for removed headers") {
      wrappedRequest.removeHeader("foo")
      wrappedRequest.getHeader("foo") shouldBe null
    }
  }

  describe("the getHeaderNamesList method") {
    it("should return a list of all the header names") {
      wrappedRequest.getHeaderNamesList.asScala should contain theSameElementsAs headerMap.keys
    }

    it("should not contain a header name that was not added") {
      wrappedRequest.getHeaderNamesList.asScala shouldNot contain theSameElementsAs List("notAHeader")
    }

    it("should include a newly added header name") {
      wrappedRequest.addHeader("butts", "butts")
      wrappedRequest.getHeaderNamesList.asScala should contain theSameElementsAs headerMap.keys ++ List("butts")
    }

    it("should not included removed headers") {
      wrappedRequest.removeHeader("foo")
      wrappedRequest.getHeaderNamesList.asScala should contain theSameElementsAs headerMap.keys.filterNot(_ == "foo")
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
      wrappedRequest.getHeadersList("notAHeader") shouldBe empty
    }

    it("should include a newly added header") {
      wrappedRequest.addHeader("bar", "foo")
      val returnedValues: List[String] = wrappedRequest.getHeadersList("bar").asScala.toList
      returnedValues.size shouldBe 1
      returnedValues should contain theSameElementsAs List("foo")
    }

    it("should include a newly added header with foo") {
      val originalValues: List[String] = wrappedRequest.getHeadersList("foo").asScala.toList
      wrappedRequest.addHeader("foo", "foo")
      val returnedValues: List[String] = wrappedRequest.getHeadersList("foo").asScala.toList
      returnedValues.size shouldBe originalValues.size + 1
      returnedValues should contain theSameElementsAs originalValues ++ List("foo")
    }

    it("should return an empty list when header is removed") {
      wrappedRequest.removeHeader("foo")
      wrappedRequest.getHeadersList("foo") shouldBe empty
    }
  }

  describe("the addHeader method") {
    it("Should add an additional value to an existing header") {
      val headerList: List[String] = wrappedRequest.getHeadersList("foo").asScala.toList
      wrappedRequest.addHeader("foo", "foo")
      val returnedValues: List[String] = wrappedRequest.getHeadersList("foo").asScala.toList
      returnedValues.size shouldBe headerList.size + 1
      returnedValues should contain theSameElementsAs headerList ++ List("foo")
    }

    it("Should add a brand new header if it didn't exist before") {
      wrappedRequest.addHeader("butts", "butts")
      val returnedValues: mutable.Buffer[String] = wrappedRequest.getHeadersList("butts").asScala
      returnedValues.size shouldBe 1
      returnedValues should contain theSameElementsAs List("butts")
    }

    it("should add a header even if it's already been deleted") {
      wrappedRequest.removeHeader("foo")
      wrappedRequest.addHeader("foo", "foo")
      val returnedValues: List[String] = wrappedRequest.getHeadersList("foo").asScala.toList
      returnedValues.size shouldBe 1
      returnedValues should contain theSameElementsAs List("foo")
    }

    it("should add a header even if one was added then deleted") {
      wrappedRequest.addHeader("foo", "foo")
      wrappedRequest.removeHeader("foo")
      wrappedRequest.addHeader("foo", "butts")
      val returnedValues: List[String] = wrappedRequest.getHeadersList("foo").asScala.toList
      returnedValues.size shouldBe 1
      returnedValues should contain theSameElementsAs List("butts")
    }
  }

  describe("the addHeader method with quality") {
    it("Should add an additional value to an existing header") {
      val headerList = wrappedRequest.getHeadersList("foo").asScala.toList
      wrappedRequest.addHeader("foo", "foo", 0.5)
      val result = wrappedRequest.getHeadersList("foo")
      result.size shouldBe headerList.size + 1
      result should contain theSameElementsAs headerList ++ List("foo;q=0.5")
    }

    it("Should add a brand new header if it didn't exist before") {
      wrappedRequest.addHeader("butts", "butts", 0.5)
      val returnedValues: mutable.Buffer[String] = wrappedRequest.getHeadersList("butts").asScala
      returnedValues.size shouldBe 1
      returnedValues should contain theSameElementsAs List("butts;q=0.5")
    }

    it("should add a header even if it's already been deleted") {
      wrappedRequest.removeHeader("foo")
      wrappedRequest.addHeader("foo", "foo", 0.5)
      val returnedValues: List[String] = wrappedRequest.getHeadersList("foo").asScala.toList
      returnedValues.size shouldBe 1
      returnedValues should contain theSameElementsAs List("foo;q=0.5")
    }

    it("should add a header even if one was added then deleted") {
      wrappedRequest.addHeader("foo", "foo")
      wrappedRequest.removeHeader("foo")
      wrappedRequest.addHeader("foo", "butts", 0.5)
      val returnedValues: List[String] = wrappedRequest.getHeadersList("foo").asScala.toList
      returnedValues.size shouldBe 1
      returnedValues should contain theSameElementsAs List("butts;q=0.5")
    }
  }

  describe("the appendHeader method") {
    it("should append a value on an existing header") {
      wrappedRequest.appendHeader("abc", "4")
      wrappedRequest.getHeadersList("abc").asScala should contain theSameElementsAs List("1,2,3,4")
    }

    it("should create a new header if the name does not yet exist") {
      wrappedRequest.appendHeader("butts", "butts")
      wrappedRequest.getHeadersList("butts").asScala should contain theSameElementsAs List("butts")
    }

    it("should append a header even if the original has been deleted") {
      wrappedRequest.removeHeader("abc")
      wrappedRequest.appendHeader("abc", "4")
      wrappedRequest.getHeadersList("abc").asScala should contain theSameElementsAs List("4")
    }

    it("should append a header even if one was created then deleted") {
      wrappedRequest.appendHeader("butts", "foo")
      wrappedRequest.removeHeader("butts")
      wrappedRequest.appendHeader("butts", "butts")
      val returnedValues: List[String] = wrappedRequest.getHeadersList("butts").asScala.toList
      returnedValues.size shouldBe 1
      returnedValues should contain theSameElementsAs List("butts")
    }
  }

  describe("the appendHeader method with quality") {
    it("should append a value on an existing header") {
      wrappedRequest.appendHeader("abc", "4", 0.1)
      wrappedRequest.getHeadersList("abc").asScala should contain theSameElementsAs List("1,2,3,4;q=0.1")
    }

    it("should create a new header if the name does not yet exist") {
      wrappedRequest.appendHeader("butts", "butts", 0.1)
      wrappedRequest.getHeadersList("butts").asScala should contain theSameElementsAs List("butts;q=0.1")
    }

    it("should append a header even if the original has been deleted") {
      wrappedRequest.removeHeader("abc")
      wrappedRequest.appendHeader("abc", "4", 0.5)
      wrappedRequest.getHeadersList("abc").asScala should contain theSameElementsAs List("4;q=0.5")
    }

    it("should append a header even if one was created then deleted") {
      wrappedRequest.appendHeader("butts", "foo")
      wrappedRequest.removeHeader("butts")
      wrappedRequest.appendHeader("butts", "butts", 0.5)
      val returnedValues: List[String] = wrappedRequest.getHeadersList("butts").asScala.toList
      returnedValues.size shouldBe 1
      returnedValues should contain theSameElementsAs List("butts;q=0.5")
    }
  }

  describe("the getPreferredSplittableHeaders method") {
    it("Should return value with largest quality value") {
      wrappedRequest.getPreferredSplittableHeaders("cup") should contain theSameElementsAs List("blue")
    }

    it("Should return the first entity if the quantities are the same") {
      wrappedRequest.getPreferredSplittableHeaders("abc") should contain theSameElementsInOrderAs List("1", "2", "3")
    }

    it("should give the highest quality even with the added value being highest") {
      wrappedRequest.addHeader("ornament", "star", 0.95)
      wrappedRequest.getPreferredSplittableHeaders("ornament") should contain theSameElementsInOrderAs List("star")
    }

    it("should give the highest quality even with an added value being in the middle") {
      wrappedRequest.addHeader("ornament", "star", 0.85)
      wrappedRequest.getPreferredSplittableHeaders("ornament") should contain theSameElementsInOrderAs List("santa")
    }

    it("should give the highest value even if the originals were removed and then a new added") {
      wrappedRequest.removeHeader("cup")
      wrappedRequest.addHeader("cup", "red", 0.1)
      wrappedRequest.getPreferredSplittableHeaders("cup") should contain theSameElementsInOrderAs List("red")
    }

    it("should give the highest single value wehn a value had been added, all removed, then another added") {
      wrappedRequest.addHeader("cup", "purple", 0.7)
      wrappedRequest.removeHeader("cup")
      wrappedRequest.addHeader("cup", "red", 0.1)
      wrappedRequest.getPreferredSplittableHeaders("cup") should contain theSameElementsInOrderAs List("red")
    }

    it("should give the highest quality even with an appended value being highest") {
      wrappedRequest.appendHeader("ornament", "star", 0.95)
      wrappedRequest.getPreferredSplittableHeaders("ornament") should contain theSameElementsInOrderAs List("star")
    }

    it("should give the highest quality even with a replaced value being highest") {
      wrappedRequest.replaceHeader("ornament", "star", 0.95)
      wrappedRequest.getPreferredSplittableHeaders("ornament") should contain theSameElementsInOrderAs List("star")
    }

    it("should work with headers that are already split") {
      wrappedRequest.getPreferredSplittableHeaders("ornament") should contain theSameElementsInOrderAs List("santa")
    }

    it("should give multiple ordered values when multiple values have the same quality") {
      wrappedRequest.getPreferredSplittableHeaders("foo") should contain theSameElementsInOrderAs List("bar", "baz")
    }
  }

  describe("the removeHeader method") {
    headerMap.keys.foreach { headerName =>
      it(s"Should remove the header from the wrapper: $headerName") {
        wrappedRequest.removeHeader(headerName)
        wrappedRequest.getHeadersList(headerName) shouldBe empty
      }
    }

    it("should remove a header that was added") {
      wrappedRequest.addHeader("butts", "butts")
      wrappedRequest.removeHeader("butts")
      wrappedRequest.getHeadersList("butts") shouldBe empty
    }

    it("should remove a header that was added to an existing header") {
      wrappedRequest.addHeader("foo", "butts")
      wrappedRequest.removeHeader("foo")
      wrappedRequest.getHeadersList("foo") shouldBe empty
    }

    it("should remove a header that was appended") {
      wrappedRequest.appendHeader("butts", "butts")
      wrappedRequest.removeHeader("butts")
      wrappedRequest.getHeadersList("butts") shouldBe empty
    }

    it("should remove a header that was replaced") {
      wrappedRequest.replaceHeader("butts", "butts")
      wrappedRequest.removeHeader("butts")
      wrappedRequest.getHeadersList("butts") shouldBe empty
    }

    it("should try to remove a header that does not exist") {
      wrappedRequest.getHeadersList("butts") shouldBe empty
      wrappedRequest.removeHeader("butts")
      wrappedRequest.getHeadersList("butts") shouldBe empty
    }

    it("should only have the new recently added value after a delete not any values prior to deletion") {
      wrappedRequest.addHeader("butts", "butts")
      wrappedRequest.removeHeader("butts")
      wrappedRequest.addHeader("butts", "foo")
      val returnedValues: util.List[String] = wrappedRequest.getHeadersList("butts")
      returnedValues.size shouldBe 1
      returnedValues should contain theSameElementsAs List("foo")
    }
  }

  describe("the getPreferredHeaders method") {
    it("Should return value with largest quality value for ornament") {
      wrappedRequest.getPreferredHeaders("ornament") should contain theSameElementsInOrderAs List("santa")
    }

    it("should return added value if quality is larger than original") {
      wrappedRequest.addHeader("ornament", "reindeer", 0.95)
      wrappedRequest.getPreferredHeaders("ornament") should contain theSameElementsInOrderAs List("reindeer")
    }

    it("should not return added value if quality is smaller than original") {
      wrappedRequest.addHeader("ornament", "reindeer", 0.85)
      wrappedRequest.getPreferredHeaders("ornament") should contain theSameElementsInOrderAs List("santa")
    }

    it("should throw an exception when quality becomes unreadable in a single line") {
      wrappedRequest.appendHeader("ornament", "star", 0.95)
      a[QualityFormatException] should be thrownBy wrappedRequest.getPreferredHeaders("ornament")
    }

    it("should return an added value when it's the only value") {
      wrappedRequest.addHeader("butts", "butts", 0.5)
      wrappedRequest.getPreferredHeaders("butts") should contain theSameElementsInOrderAs List("butts")
    }

    it("No quality specified should set quality to 1") {
      wrappedRequest.addHeader("ornament", "reindeer")
      wrappedRequest.getPreferredHeaders("ornament") should contain theSameElementsInOrderAs List("reindeer")
    }

    it("should give multiple ordered values when multiple values have the same quality") {
      wrappedRequest.getPreferredHeaders("foo") should contain theSameElementsInOrderAs List("bar", "baz")
    }
  }

  describe("the replaceHeader method") {
    it("should replace a header that already exists") {
      wrappedRequest.replaceHeader("foo", "foo")
      wrappedRequest.getHeadersList("foo") should contain theSameElementsAs List("foo")
    }

    it("should add a new header when by itself") {
      wrappedRequest.replaceHeader("butts", "butts")
      wrappedRequest.getHeadersList("butts") should contain theSameElementsAs List("butts")
    }

    it("should add a header even when existing ones have been removed") {
      wrappedRequest.removeHeader("foo")
      wrappedRequest.replaceHeader("foo", "foo")
      wrappedRequest.getHeadersList("foo") should contain theSameElementsAs List("foo")
    }

    it("should add a header when some have been added, removed then re added") {
      wrappedRequest.addHeader("foo", "butts")
      wrappedRequest.removeHeader("foo")
      wrappedRequest.replaceHeader("foo", "foo")
      wrappedRequest.getHeadersList("foo") should contain theSameElementsAs List("foo")
    }
  }

  describe("the replaceHeader method with quality") {
    it("should replace a header that already exists") {
      wrappedRequest.replaceHeader("foo", "foo", 0.5)
      wrappedRequest.getHeadersList("foo") should contain theSameElementsAs List("foo;q=0.5")
    }

    it("should add a new header when by itself") {
      wrappedRequest.replaceHeader("butts", "butts", 0.5)
      wrappedRequest.getHeadersList("butts") should contain theSameElementsAs List("butts;q=0.5")
    }

    it("should add a header even when existing ones have been removed") {
      wrappedRequest.removeHeader("foo")
      wrappedRequest.replaceHeader("foo", "foo", 0.5)
      wrappedRequest.getHeadersList("foo") should contain theSameElementsAs List("foo;q=0.5")
    }

    it("should add a header when some have been added, removed then re added") {
      wrappedRequest.addHeader("foo", "butts")
      wrappedRequest.removeHeader("foo")
      wrappedRequest.replaceHeader("foo", "foo", 0.5)
      wrappedRequest.getHeadersList("foo") should contain theSameElementsAs List("foo;q=0.5")
    }
  }

  describe("the getSplittableHeaders method") {
    it("Should return the values in a header if splittable as a list") {
      wrappedRequest.getSplittableHeaders("abc").asScala.toList should contain theSameElementsAs List("1", "2", "3")
    }

    it("should return a split list when a value is added") {
      wrappedRequest.addHeader("abc", "4")
      wrappedRequest.getSplittableHeaders("abc").asScala.toList should contain theSameElementsInOrderAs List("1", "2", "3", "4")
    }

    it("Should return a splittable list when added") {
      wrappedRequest.appendHeader("abc", "4")
      wrappedRequest.getSplittableHeaders("abc").asScala.toList should contain theSameElementsAs List("1", "2", "3", "4")
    }

    it("should split correctly when a replace is done") {
      wrappedRequest.replaceHeader("abc", "5,6,7,8")
      wrappedRequest.getSplittableHeaders("abc").asScala.toList should contain theSameElementsAs List("5", "6", "7", "8")
    }

    it("should return list for header that is already split") {
      wrappedRequest.getSplittableHeaders("foo").asScala.toList should contain theSameElementsAs List("bar", "baz")
    }

    it("should return empty list if header does not exist") {
      wrappedRequest.getSplittableHeaders("notAHeader").asScala.toList shouldBe empty
    }

    it("should return empty list if header is removed") {
      wrappedRequest.removeHeader("abc")
      wrappedRequest.getSplittableHeaders("abc").asScala.toList shouldBe empty
    }

    it("should throw an exception when quality is garbage") {
      wrappedRequest.addHeader("cup", "butts;q=butts")
      a[QualityFormatException] should be thrownBy wrappedRequest.getPreferredSplittableHeaders("cup")
    }
  }

  describe("getQueryString") {
    it("should return null if there are no parameters") {
      wrappedRequest.setParameterMap(Map.empty[String, Array[String]].asJava)

      wrappedRequest.getQueryString shouldBe null
    }

    it("should return a string representation of the query string") {
      wrappedRequest.getQueryString shouldBe "foo=bar&foo=baz"
    }

    it("should return a string representation of the query string after mutation") {
      wrappedRequest.setParameterMap(Map("bar" -> Array("rab"), "baz" -> Array("zab")).asJava)

      wrappedRequest.getQueryString shouldBe "bar=rab&baz=zab"
    }
  }

  describe("getParameter") {
    it("should return null if there is no value associated with a key") {
      wrappedRequest.getParameter("mia") shouldBe null
    }

    it("should return the first parameter value associated with a key") {
      wrappedRequest.getParameter("foo") shouldBe "bar"
    }

    it("should return the first parameter value associated with a key after mutation") {
      wrappedRequest.setParameterMap(Map("bar" -> Array("rab")).asJava)

      wrappedRequest.getParameter("bar") shouldBe "rab"
    }
  }

  describe("getParameterNames") {
    it("should return an empty enumeration if there are no parameters") {
      wrappedRequest.setParameterMap(Map.empty[String, Array[String]].asJava)

      wrappedRequest.getParameterNames.asScala shouldBe empty
    }

    it("should return all parameter names") {
      wrappedRequest.getParameterNames.asScala.toSeq should contain only "foo"
    }

    it("should return all parameter names after mutation") {
      wrappedRequest.setParameterMap(Map("bar" -> Array("rab"), "baz" -> Array("zab")).asJava)

      wrappedRequest.getParameterNames.asScala.toSeq should contain only("bar", "baz")
    }
  }

  describe("getParameterValues") {
    it("should return an null if there is no key parameter") {
      wrappedRequest.setParameterMap(Map.empty[String, Array[String]].asJava)

      wrappedRequest.getParameterValues("mia") shouldBe null
    }

    it("should return all parameter values associated with a key") {
      wrappedRequest.getParameterValues("foo") should contain only("bar", "baz")
    }

    it("should return all parameter values associated with a key after mutation") {
      wrappedRequest.setParameterMap(Map("bar" -> Array("rab")).asJava)

      wrappedRequest.getParameterValues("bar") should contain only "rab"
    }
  }

  describe("getParameterMap") {
    it("should return an empty map if there are no parameters") {
      wrappedRequest.setParameterMap(Map.empty[String, Array[String]].asJava)

      wrappedRequest.getParameterMap shouldBe empty
    }

    it("should return the original request query parameters if no mutation has occurred") {
      wrappedRequest.getParameterMap.keySet().asScala should contain only "foo"
      wrappedRequest.getParameterMap.get("foo") should contain only("bar", "baz")
    }

    it("should return the new request query parameters if mutation has occurred") {
      wrappedRequest.setParameterMap(Map("1" -> Array("2")).asJava)

      wrappedRequest.getParameterMap.keySet().asScala should contain only "1"
      wrappedRequest.getParameterMap.get("1") should contain only "2"
    }

    it("should return an immutable map") {
      an[UnsupportedOperationException] should be thrownBy wrappedRequest.getParameterMap.put("foo", Array("oof"))
      wrappedRequest.getParameterMap.get("foo") should contain only("bar", "baz")
    }
  }

  describe("setParameterMap") {
    it("should throw an IllegalArgumentException if passed null") {
      an[IllegalArgumentException] should be thrownBy wrappedRequest.setParameterMap(null)
    }

    it("should replace the current parameter map") {
      wrappedRequest.setParameterMap(Map("bar" -> Array("rab")).asJava)

      wrappedRequest.getParameterMap.keySet().asScala should contain only "bar"
      wrappedRequest.getParameterMap.get("bar") should contain only "rab"
    }

    it("should set the parameter map with the same iteration order as the provided map") {
      val orderedMap = mutable.LinkedHashMap(
        "2" -> Array("2"),
        "4" -> Array("4"),
        "3" -> Array("3"),
        "1" -> Array("1"),
        "5" -> Array("5"))

      wrappedRequest.setParameterMap(orderedMap.asJava)

      wrappedRequest.getParameterMap.keySet().asScala.toSeq should contain inOrderOnly("2", "4", "3", "1", "5")
    }

    it("should not reflect changes made to the provided map after this method has been called") {
      val orderedMap = mutable.LinkedHashMap(
        "2" -> Array("2"),
        "4" -> Array("4"),
        "3" -> Array("3"),
        "1" -> Array("1"),
        "5" -> Array("5"))

      wrappedRequest.setParameterMap(orderedMap.asJava)
      orderedMap += ("6" -> Array("6"))

      wrappedRequest.getParameterMap.keySet().asScala.toSeq should contain inOrderOnly("2", "4", "3", "1", "5")
    }
  }
}
