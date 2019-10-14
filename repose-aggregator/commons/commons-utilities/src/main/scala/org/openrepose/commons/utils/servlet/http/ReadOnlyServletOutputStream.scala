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

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}

import com.typesafe.scalalogging.StrictLogging
import javax.servlet.ServletOutputStream

class ReadOnlyServletOutputStream(servletOutputStream: ServletOutputStream)
  extends ExtendedServletOutputStream
    with StrictLogging {

  private val byteArrayOutputStream = new ByteArrayOutputStream()

  override def write(b: Int): Unit = {
    byteArrayOutputStream.write(b)
    servletOutputStream.write(b)
  }

  override def write(b: Array[Byte]): Unit = {
    byteArrayOutputStream.write(b)
    servletOutputStream.write(b)
  }

  override def write(b: Array[Byte], off: Int, len: Int): Unit = {
    byteArrayOutputStream.write(b, off, len)
    servletOutputStream.write(b, off, len)
  }

  override def getOutputStreamAsInputStream: InputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray)

  override def setOutput(in: InputStream): Unit = {
    logger.error("setOutput not available on {}", classOf[ReadOnlyServletOutputStream].getSimpleName)
    throw new IllegalStateException("Method not available for READONLY response mode")
  }

  override def commit(): Unit = {
    logger.error("commit not available on {}", classOf[ReadOnlyServletOutputStream].getSimpleName)
    throw new IllegalStateException("Method not available for READONLY response mode")
  }

  override def resetBuffer(): Unit = {
    logger.debug("Discarding accumulated buffered output")
    byteArrayOutputStream.reset()
  }

  // Close the underlying ByteArrayOutputStream, which is a no-op.
  override def close(): Unit = byteArrayOutputStream.close()

  override def toString: String = byteArrayOutputStream.toString
}
