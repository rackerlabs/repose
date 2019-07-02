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
package org.openrepose.powerfilter.intrafilterlogging

import java.io.{ByteArrayInputStream, IOException}
import javax.servlet.ServletInputStream

import org.junit.runner.RunWith
import org.openrepose.commons.utils.io.BufferedServletInputStream
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.core.systemmodel.config.Filter
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class RequestLogTest extends FunSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  import org.mockito.Mockito.when

  val requestBody = "pandas"

  var httpServletRequestWrapper: HttpServletRequestWrapper = _
  val dummyInputStream = new BufferedServletInputStream(new ByteArrayInputStream(requestBody.getBytes))

  override def beforeEach() = {
    httpServletRequestWrapper = mock[HttpServletRequestWrapper]

    // the code under test makes some static method calls, so we gotta do this mess
    when(httpServletRequestWrapper.getInputStream).thenReturn(dummyInputStream)
    when(httpServletRequestWrapper.getHeaderNames).thenReturn(Iterator[String]().asJavaEnumeration)
  }

  describe("a request log") {
    describe("filter description") {
      it("should include the filter name") {
        val filterName = "test-filter"

        // when we create a new RequestLog
        val requestLog = new RequestLog(httpServletRequestWrapper, filterName)

        // then the filter description includes just the filter name
        filterName shouldEqual requestLog.currentFilter
      }
    }

    describe("input stream") {
      it("should be readable after being used by this class") {
        new RequestLog(httpServletRequestWrapper, "test-filter")

        // try to read from the buffer again
        val buffer = new Array[Byte](requestBody.length)
        dummyInputStream.read(buffer, 0, requestBody.length)
        new String(buffer) shouldEqual requestBody
      }

      it("should not throw an exception if the provided input stream does not support mark/reset") {
        val unsupportedInputStream = mock[ServletInputStream]
        when(unsupportedInputStream.markSupported()).thenReturn(false)
        when(httpServletRequestWrapper.getInputStream).thenReturn(unsupportedInputStream)

        // no exception should be thrown
        new RequestLog(httpServletRequestWrapper, "test-filter")
      }

      it("should not throw an exception if the provided input stream is already closed") {
        val closedInputStream = mock[ServletInputStream]
        when(closedInputStream.markSupported()).thenReturn(true)
        when(closedInputStream.read()).thenThrow(new IOException("Stream closed"))
        when(httpServletRequestWrapper.getInputStream).thenReturn(closedInputStream)

        // no exception should be thrown
        new RequestLog(httpServletRequestWrapper, "test-filter")
      }
    }

    describe("header names") {
      it("should grab the headers when there is one") {
        when(httpServletRequestWrapper.getHeaderNames).thenReturn(Iterator[String]("header-name").asJavaEnumeration)
        when(httpServletRequestWrapper.getHeaders("header-name")).thenReturn(Iterator[String]("header-value").asJavaEnumeration)

        val requestLog = new RequestLog(httpServletRequestWrapper, "test-filter")

        requestLog.headers.asScala should contain ("header-name" -> "header-value")
      }

      it("should grab the headers when there are two") {
        when(httpServletRequestWrapper.getHeaderNames).thenReturn(Iterator[String]("header-name", "Accept").asJavaEnumeration)
        when(httpServletRequestWrapper.getHeaders("header-name")).thenReturn(Iterator[String]("header-value").asJavaEnumeration)
        when(httpServletRequestWrapper.getHeaders("Accept")).thenReturn(Iterator[String]("text/html").asJavaEnumeration)

        val requestLog = new RequestLog(httpServletRequestWrapper, "test-filter")

        requestLog.headers.asScala should contain ("header-name" -> "header-value")
        requestLog.headers.asScala should contain ("Accept" -> "text/html")
      }

      it("should grab the headers when a header has multiple values") {
        when(httpServletRequestWrapper.getHeaderNames).thenReturn(Iterator[String]("Accept").asJavaEnumeration)
        when(httpServletRequestWrapper.getHeaders("Accept")).thenReturn(Iterator[String]("text/html", "application/xml").asJavaEnumeration)

        val requestLog = new RequestLog(httpServletRequestWrapper, "test-filter")

        requestLog.headers.asScala should contain ("Accept" -> "text/html,application/xml")
      }
    }
  }

}
