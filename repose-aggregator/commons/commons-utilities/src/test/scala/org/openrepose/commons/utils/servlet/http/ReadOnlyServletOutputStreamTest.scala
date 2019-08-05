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
import javax.servlet.ServletOutputStream

import org.junit.runner.RunWith
import org.mockito.AdditionalMatchers._
import org.mockito.Matchers.{eq => mEq}
import org.mockito.Mockito._
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

import scala.io.Source

@RunWith(classOf[JUnitRunner])
class ReadOnlyServletOutputStreamTest extends FunSpec with Matchers with MockitoSugar {

  describe("write") {
    it("should write through to the wrapped output stream - Int") {
      val mockOutputStream = mock[ServletOutputStream]
      val readOnlyOutputStream = new ReadOnlyServletOutputStream(mockOutputStream)

      readOnlyOutputStream.write(10)

      verify(mockOutputStream).write(10)
    }

    it("should write through to the wrapped output stream - Array[Byte]") {
      val mockOutputStream = mock[ServletOutputStream]
      val readOnlyOutputStream = new ReadOnlyServletOutputStream(mockOutputStream)

      readOnlyOutputStream.write(Array[Byte](1, 2, 3))

      verify(mockOutputStream).write(aryEq(Array[Byte](1, 2, 3)))
    }

    it("should write through to the wrapped output stream - Array[Byte], offset, length") {
      val mockOutputStream = mock[ServletOutputStream]
      val readOnlyOutputStream = new ReadOnlyServletOutputStream(mockOutputStream)

      readOnlyOutputStream.write(Array[Byte](1, 2, 3), 0, 2)

      verify(mockOutputStream).write(aryEq(Array[Byte](1, 2, 3)), mEq(0), mEq(2))
    }
  }

  describe("getOutputStreamAsInputStream") {
    it("should return an InputStream with all of the contents written to the wrapper") {
      val mockOutputStream = mock[ServletOutputStream]
      val readOnlyOutputStream = new ReadOnlyServletOutputStream(mockOutputStream)

      readOnlyOutputStream.write(10)
      readOnlyOutputStream.write(20)

      val inputStream = readOnlyOutputStream.getOutputStreamAsInputStream
      val firstRead = inputStream.read()
      val secondRead = inputStream.read()
      val thirdRead = inputStream.read()

      firstRead shouldEqual 10
      secondRead shouldEqual 20
      thirdRead shouldEqual -1
    }
  }

  describe("setOutput") {
    it("should throw an IllegalStateException") {
      val mockOutputStream = mock[ServletOutputStream]
      val readOnlyOutputStream = new ReadOnlyServletOutputStream(mockOutputStream)

      an[IllegalStateException] should be thrownBy readOnlyOutputStream.setOutput(new ByteArrayInputStream(Array[Byte](1, 2, 3)))
    }
  }

  describe("commit") {
    it("should throw an IllegalStateException") {
      val mockOutputStream = mock[ServletOutputStream]
      val readOnlyOutputStream = new ReadOnlyServletOutputStream(mockOutputStream)

      an[IllegalStateException] should be thrownBy readOnlyOutputStream.commit()
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
