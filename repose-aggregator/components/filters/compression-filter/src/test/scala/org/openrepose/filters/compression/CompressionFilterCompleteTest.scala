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

package org.openrepose.filters.compression

import java.io.ByteArrayOutputStream
import java.util.zip.{Deflater, GZIPOutputStream}
import javax.servlet._

import org.apache.http.HttpHeaders
import org.junit.runner.RunWith
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.external.pjlcompression.CompressingFilter
import org.openrepose.filters.compression.config.{Compression, ContentCompressionConfig}
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class CompressionFilterCompleteTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {

  var filter: CompressionFilter = _
  var servletRequest: MockHttpServletRequest = _
  var servletResponse: MockHttpServletResponse = _
  var filterChain: FilterChain = _
  var compressingFilterFactory: CompressingFilterFactory = _
  var compressingFilter: CompressingFilter = _
  var filterConfig: FilterConfig = _

  override def beforeEach() = {
    servletRequest = new MockHttpServletRequest
    servletResponse = new MockHttpServletResponse
    filterChain = mock[FilterChain]
    compressingFilterFactory = new CompressingFilterFactory
    compressingFilter = new CompressingFilter
    filterConfig = mock[FilterConfig]

    when(filterConfig.getInitParameterNames).thenReturn(List.empty[String].toIterator.asJavaEnumeration)
    when(filterConfig.getServletContext).thenReturn(mock[ServletContext])

    filter = new CompressionFilter(mock[ConfigurationService], compressingFilterFactory)
    filter.init(filterConfig)
  }

  describe("compressing data") {
    val content = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi pretium non mi ac malesuada. Integer nec est turpis duis."
    val gzipCompressedContent = compressGzipContent(content)
    val deflateCompressedContent = compressDeflateContent(content)
    val contentBytes = content.getBytes()

    List(
      ("gzip", gzipCompressedContent, true),
      ("x-gzip", gzipCompressedContent, true),
      ("deflate", deflateCompressedContent, true),
      ("identity", contentBytes, false)
    ) foreach { case (encoding, zippedContent, isZipped) =>
      it(s"Check if GET request with Accept-Encoding header set to $encoding is honored on the output") {
        when(filterChain.doFilter(any(classOf[ServletRequest]), any(classOf[ServletResponse]))).thenAnswer(new Answer[Unit]() {
          override def answer(invocation: InvocationOnMock): Unit = {
            invocation.getArguments()(1).asInstanceOf[ServletResponse].getOutputStream.write(contentBytes)
          }
        })
        servletRequest.addHeader(HttpHeaders.ACCEPT_ENCODING, encoding)
        filter.configurationUpdated(defaultConfig)
        filter.doFilter(servletRequest, servletResponse, filterChain)

        if (isZipped) {
          val rxBody = servletResponse.getContentAsByteArray
          val expected = zippedContent.asInstanceOf[Array[Byte]]
          assert(rxBody sameElements expected)
        } else {
          assert(servletResponse.getContentAsString == content)
        }
      }
    }
  }

  def defaultConfig: ContentCompressionConfig = {
    val compression = new Compression
    compression.setStatsEnabled(false)
    compression.setDebug(true)
    compression.setCompressionThreshold(16)

    val contentCompressionConfig = new ContentCompressionConfig
    contentCompressionConfig.setCompression(compression)
    contentCompressionConfig
  }

  def compressGzipContent(content: String): Array[Byte] = {
    val out = new ByteArrayOutputStream(content.length())
    val gzipOut = new GZIPOutputStream(out)
    gzipOut.write(content.getBytes())
    gzipOut.close()
    val compressedContent = out.toByteArray
    out.close()
    compressedContent
  }

  def compressDeflateContent(content: String): Array[Byte] = {
    val deflater = new Deflater()
    deflater.setInput(content.getBytes())

    val outputStream = new ByteArrayOutputStream(content.getBytes().length)

    deflater.finish()
    val buffer = Array.ofDim[Byte](1024)
    while (!deflater.finished()) {
      val count = deflater.deflate(buffer) // returns the generated code... index
      outputStream.write(buffer, 0, count)
    }
    outputStream.close()
    outputStream.toByteArray
  }
}
