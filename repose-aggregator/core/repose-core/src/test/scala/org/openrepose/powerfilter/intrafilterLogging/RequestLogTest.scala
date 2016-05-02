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
package org.openrepose.powerfilter.intrafilterLogging

import java.io.{ByteArrayInputStream, IOException}
import javax.servlet.ServletInputStream

import org.junit.runner.RunWith
import org.openrepose.commons.utils.io.BufferedServletInputStream
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.systemmodel.Filter
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}
import org.scalatest.junit.JUnitRunner

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class RequestLogTest extends FunSpec with Matchers with MockitoSugar with BeforeAndAfter {

  import org.mockito.Mockito.when

  val requestBody = "pandas"

  var httpServletRequestWrapper: HttpServletRequestWrapper = _
  val dummyInputStream = new BufferedServletInputStream(new ByteArrayInputStream(requestBody.getBytes))

  before {
    httpServletRequestWrapper = mock[HttpServletRequestWrapper]

    // the code under test makes some static method calls, so we gotta do this mess
    when(httpServletRequestWrapper.getInputStream).thenReturn(dummyInputStream)
    when(httpServletRequestWrapper.getHeaderNames).thenReturn(Iterator[String]().asJavaEnumeration)
  }

  describe("a request log") {
    describe("filter description") {

      it("should include the filter ID when it is present") {
        // given a filter ID and name
        val filterId = "filter1"
        val filterName = "test-filter"

        val filter = new Filter
        filter.setId(filterId)
        filter.setName(filterName)

        // when we create a new RequestLog
        val requestLog = new RequestLog(httpServletRequestWrapper, filter)

        // then the filter description includes both the ID and name
        s"$filterId-$filterName" shouldEqual requestLog.currentFilter
      }

      it("should not include the filter ID when it is null") {
        // given a null filter ID
        val filterId = null
        val filterName = "test-filter"

        val filter = new Filter
        filter.setId(filterId)
        filter.setName(filterName)

        // when we create a new RequestLog
        val requestLog = new RequestLog(httpServletRequestWrapper, filter)

        // then the filter description includes just the filter name
        filterName shouldEqual requestLog.currentFilter
      }

      it("should not include the filter ID when it is an empty string") {
        // given an empty string for the filter ID
        val filterId = ""
        val filterName = "test-filter"

        val filter = new Filter
        filter.setId(filterId)
        filter.setName(filterName)

        // when we create a new RequestLog
        val requestLog = new RequestLog(httpServletRequestWrapper, filter)

        // then the filter description includes just the filter name
        filterName shouldEqual requestLog.currentFilter
      }
    }

    describe("input stream") {
      it("should be readable after being used by this class") {
        val filter = new Filter
        filter.setName("test-filter")

        new RequestLog(httpServletRequestWrapper, filter)

        // try to read from the buffer again
        val buffer = new Array[Byte](requestBody.length)
        dummyInputStream.read(buffer, 0, requestBody.length)
        new String(buffer) shouldEqual requestBody
      }

      it("should not throw an exception if the provided input stream does not support mark/reset") {
        val filter = new Filter
        filter.setName("test-filter")

        val unsupportedInputStream = mock[ServletInputStream]
        when(unsupportedInputStream.markSupported()).thenReturn(false)
        when(httpServletRequestWrapper.getInputStream).thenReturn(unsupportedInputStream)

        // no exception should be thrown
        new RequestLog(httpServletRequestWrapper, filter)
      }

      it("should not throw an exception if the provided input stream is already closed") {
        val filter = new Filter
        filter.setName("test-filter")

        val closedInputStream = mock[ServletInputStream]
        when(closedInputStream.markSupported()).thenReturn(true)
        when(closedInputStream.read()).thenThrow(new IOException("Stream closed"))
        when(httpServletRequestWrapper.getInputStream).thenReturn(closedInputStream)

        // no exception should be thrown
        new RequestLog(httpServletRequestWrapper, filter)
      }
    }

    describe("header names") {
      it("should grab the headers when there is one") {
        val filter = new Filter
        filter.setName("test-filter")

        when(httpServletRequestWrapper.getHeaderNames).thenReturn(Iterator[String]("header-name").asJavaEnumeration)
        when(httpServletRequestWrapper.getHeaders("header-name")).thenReturn(Iterator[String]("header-value").asJavaEnumeration)

        val requestLog = new RequestLog(httpServletRequestWrapper, filter)

        requestLog.headers.asScala should contain ("header-name" -> "header-value")
      }

      it("should grab the headers when there are two") {
        val filter = new Filter
        filter.setName("test-filter")

        when(httpServletRequestWrapper.getHeaderNames).thenReturn(Iterator[String]("header-name", "Accept").asJavaEnumeration)
        when(httpServletRequestWrapper.getHeaders("header-name")).thenReturn(Iterator[String]("header-value").asJavaEnumeration)
        when(httpServletRequestWrapper.getHeaders("Accept")).thenReturn(Iterator[String]("text/html").asJavaEnumeration)

        val requestLog = new RequestLog(httpServletRequestWrapper, filter)

        requestLog.headers.asScala should contain ("header-name" -> "header-value")
        requestLog.headers.asScala should contain ("Accept" -> "text/html")
      }

      it("should grab the headers when a header has multiple values") {
        val filter = new Filter
        filter.setName("test-filter")

        when(httpServletRequestWrapper.getHeaderNames).thenReturn(Iterator[String]("Accept").asJavaEnumeration)
        when(httpServletRequestWrapper.getHeaders("Accept")).thenReturn(Iterator[String]("text/html", "application/xml").asJavaEnumeration)

        val requestLog = new RequestLog(httpServletRequestWrapper, filter)

        requestLog.headers.asScala should contain ("Accept" -> "text/html,application/xml")
      }
    }
  }

}
