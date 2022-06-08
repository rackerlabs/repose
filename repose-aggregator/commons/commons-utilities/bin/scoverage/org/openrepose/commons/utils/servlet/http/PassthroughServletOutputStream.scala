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

import java.io.InputStream

import com.typesafe.scalalogging.StrictLogging
import javax.servlet.ServletOutputStream

class PassthroughServletOutputStream(servletOutputStream: ServletOutputStream)
  extends ExtendedServletOutputStream
    with StrictLogging {

  override def write(b: Int): Unit = servletOutputStream.write(b)

  override def write(b: Array[Byte]): Unit = servletOutputStream.write(b)

  override def write(b: Array[Byte], off: Int, len: Int): Unit = servletOutputStream.write(b, off, len)

  override def getOutputStreamAsInputStream: InputStream = {
    logger.error("getOutputStreamAsInputStream not available on {}", classOf[PassthroughServletOutputStream].getSimpleName)
    throw new IllegalStateException("Method not available for PASSTHROUGH response mode")
  }

  override def setOutput(in: InputStream): Unit = {
    logger.error("setOutput not available on {}", classOf[PassthroughServletOutputStream].getSimpleName)
    throw new IllegalStateException("Method not available for PASSTHROUGH response mode")
  }

  override def commit(): Unit = {
    logger.error("commit not available on {}", classOf[PassthroughServletOutputStream].getSimpleName)
    throw new IllegalStateException("Method not available for PASSTHROUGH response mode")
  }

  // Since this OutputStream does not maintain a buffer, do nothing
  override def resetBuffer(): Unit = {}
}
