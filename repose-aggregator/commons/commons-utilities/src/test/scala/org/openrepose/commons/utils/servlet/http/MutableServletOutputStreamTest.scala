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

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import javax.servlet.ServletOutputStream

import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

import scala.io.Source

@RunWith(classOf[JUnitRunner])
class MutableServletOutputStreamTest extends FunSpec with Matchers with MockitoSugar {

  describe("write") {
    it("should not yet write through to the wrapped output stream - Int") {
      val mockOutputStream = mock[ServletOutputStream]
      val mutableOutputStream = new MutableServletOutputStream(mockOutputStream)

      mutableOutputStream.write(10)

      verifyZeroInteractions(mockOutputStream)
    }

    it("should not yet write through to the wrapped output stream - Array[Byte]") {
      val mockOutputStream = mock[ServletOutputStream]
      val mutableOutputStream = new MutableServletOutputStream(mockOutputStream)

      mutableOutputStream.write(Array[Byte](1, 2, 3))

      verifyZeroInteractions(mockOutputStream)
    }

    it("should not yet write through to the wrapped output stream - Array[Byte], offset, length") {
      val mockOutputStream = mock[ServletOutputStream]
      val mutableOutputStream = new MutableServletOutputStream(mockOutputStream)

      mutableOutputStream.write(Array[Byte](1, 2, 3), 0, 2)

      verifyZeroInteractions(mockOutputStream)
    }
  }

  describe("getOutputStreamAsInputStream") {
    it("should return an InputStream with all of the contents written to the wrapper") {
      val mockOutputStream = mock[ServletOutputStream]
      val mutableOutputStream = new MutableServletOutputStream(mockOutputStream)

      mutableOutputStream.write(10)
      mutableOutputStream.write(20)

      val inputStream = mutableOutputStream.getOutputStreamAsInputStream
      val firstRead = inputStream.read()
      val secondRead = inputStream.read()
      val thirdRead = inputStream.read()

      firstRead shouldEqual 10
      secondRead shouldEqual 20
      thirdRead shouldEqual -1
    }
  }

  describe("setOutput") {
    val testBody = """{ "test": "nbsp\u00A0" }"""

    it("should set the contents of this wrapper to the contents of the provided InputStream") {
      val outputStream = new ByteArrayServletOutputStream()
      val mutableOutputStream = new MutableServletOutputStream(outputStream)

      mutableOutputStream.setOutput(new ByteArrayInputStream(testBody.getBytes))
      mutableOutputStream.commit()

      outputStream.toString shouldEqual testBody
    }

    Set(
      StandardCharsets.ISO_8859_1,
      StandardCharsets.UTF_8,
      StandardCharsets.UTF_16
    ) foreach { charset =>
      it(s"should be charset agnostic $charset") {
        val outputStream = new ByteArrayServletOutputStream()
        val mutableOutputStream = new MutableServletOutputStream(outputStream)

        mutableOutputStream.setOutput(new ByteArrayInputStream(testBody.getBytes(charset)))
        mutableOutputStream.commit()

        new String(outputStream.toByteArray, charset) shouldEqual testBody
      }
    }

    // The US_ASCII Charset replaces the non-printable NBSP character with the literal Question Mark ('?').
    it(s"should be charset agnostic ${StandardCharsets.US_ASCII}") {
      val outputStream = new ByteArrayServletOutputStream()
      val mutableOutputStream = new MutableServletOutputStream(outputStream)

      mutableOutputStream.setOutput(new ByteArrayInputStream(testBody.getBytes(StandardCharsets.US_ASCII)))
      mutableOutputStream.commit()

      new String(outputStream.toByteArray, StandardCharsets.US_ASCII) shouldEqual """{ "test": "nbsp?" }"""
    }
  }

  describe("commit") {
    it("should commit all writes to the underlying OutputStream") {
      val outputStream = new ByteArrayServletOutputStream()
      val mutableOutputStream = new MutableServletOutputStream(outputStream)

      mutableOutputStream.write("foo".getBytes)
      mutableOutputStream.commit()

      outputStream.toString shouldEqual "foo"
    }
  }

  describe("resetBuffer") {
    it("should clear the internal buffer") {
      val mockOutputStream = mock[ServletOutputStream]
      val readOnlyOutputStream = new ReadOnlyServletOutputStream(mockOutputStream)

      readOnlyOutputStream.write("foo".getBytes)
      readOnlyOutputStream.resetBuffer()

      val bufferString = Source.fromInputStream(readOnlyOutputStream.getOutputStreamAsInputStream).mkString
      bufferString shouldEqual ""
    }
  }
}
