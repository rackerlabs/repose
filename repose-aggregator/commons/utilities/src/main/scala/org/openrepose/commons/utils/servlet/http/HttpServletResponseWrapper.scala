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
import java.util
import javax.servlet.ServletOutputStream
import javax.servlet.http.HttpServletResponse

/**
 * This class wraps a HttpServletResponse applying further functionality. It allows for varying levels of read
 * and writeablity for both headers and body. Both headers and body offer three modes, although in the case of headers
 * two are functionally identical. If either option is set to ResponseMode.MUTABLE commitToResponse must be called
 * before completion of the doFilter method in the utilizing filter otherwise all values may not be written to the
 * underlying response.
 *
 * When working with headers a mode of ResponseMode.PASSTHROUGH or ResponseMode.READONLY behave the same as the basic
 * HttpServletResponse interface. In other words all read methods are supported, as are those methods that are strictly
 * adds. In either case the headers will be written directly to the underlying response as they are added. A mode of
 * ResponseMode.MUTABLE will enable the append, replace, and remove header methods. In mutable mode commitToResponse
 * must be called when all changes to the response are done otherwise none of the headers will written to the underlying
 * response.
 *
 * When working with the body a mode of ResponseMode.PASSTHROUGH will have all calls go directly to the provided output
 * stream, which in the case of one of the constructors is the original response's output stream. A mode of
 * ResponseMode.READONLY will see us wrapping the provided output stream in one that will record everything that goes
 * through but still passing the data along to the provided stream. This can be accessed by calling
 * getOutputStreamAsInputStream. It should be noted that this will keep a secondary copy of the response in memory,
 * which for large responses could be expensive. If you desire to see the output after it went through a provided output
 * stream you can use our wrapping stream xxxxxxx yourself by first wrapping the original response's output stream in
 * our wrapper and then wrapping our stream with your own, and passing that in and setting the mode to
 * ResponseMode.PASSTHROUGH. ResponseMode.MUTABLE behaves similiarly, but will not write to the underlying output stream
 * until commitToResponse is called. Additionally, you can call setOutput with an input stream which we will wrap in an
 * output stream which will be used to write to the underlying output stream when commitToResponse is called. This mode
 * will also break chunked encoding because the response can't be streamed directly through.
 *
 * @constructor the main constructor to be used for this class
 * @param originalResponse the response to be wrapped
 * @param headerMode the read/write-ability mode of headers
 * @param bodyMode the read/write-ability mode of the body
 * @param desiredOutputStream the underlying output stream to be presented callers of getOutputStream, maybe wrapped
 *                            depending on mode selection of the body
 */
class HttpServletResponseWrapper(originalResponse: HttpServletResponse, headerMode: ResponseMode, bodyMode: ResponseMode, desiredOutputStream: ServletOutputStream)
  extends javax.servlet.http.HttpServletResponseWrapper(originalResponse)
  with HeaderInteractor {

  /**
   * This constructor chains to the main constructor using the original responses output stream  as the last arguement.
   *
   * @constructor the constructor to use when you don't plan on using a custom stream for processing
   * @param originalResponse the response to be wrapped
   * @param headerMode the mode to use for header accessiblity
   * @param bodyMode the mode to use for body accessibility
   */
  def this(originalResponse: HttpServletResponse, headerMode: ResponseMode, bodyMode: ResponseMode) =
    this(originalResponse, headerMode, bodyMode, originalResponse.getOutputStream)

  override def getHeaderNamesList: util.List[String] = ???

  override def addHeader(headerName: String, headerValue: String, quality: Double): Unit = ???

  /**
   * @throws IllegalStateException when headerMode is anything other than ResponseMode.MUTABLE
   */
  override def appendHeader(headerName: String, headerValue: String): Unit = ???

  /**
   * @throws IllegalStateException when headerMode is anything other than ResponseMode.MUTABLE
   */
  override def appendHeader(headerName: String, headerValue: String, quality: Double): Unit = ???

  /**
   * @throws IllegalStateException when headerMode is anything other than ResponseMode.MUTABLE
   */
  override def removeHeader(headerName: String): Unit = ???

  /**
   * @throws IllegalStateException when headerMode is anything other than ResponseMode.MUTABLE
   */
  override def replaceHeader(headerName: String, headerValue: String): Unit = ???

  /**
   * @throws IllegalStateException when headerMode is anything other than ResponseMode.MUTABLE
   */
  override def replaceHeader(headerName: String, headerValue: String, quality: Double): Unit = ???

  override def getHeadersList(headerName: String): util.List[String] = ???

  override def getPreferredHeaders(headerName: String): util.List[String] = ???

  override def getPreferredSplittableHeaders(headerName: String): util.List[String] = ???

  override def getSplittableHeaders(headerName: String): util.List[String] = ???

  /**
   * @throws IllegalStateException when bodyMode is ResponseMode.PASSTHROUGH
   */
  def getOutputStreamAsInputStream(): InputStream = ???

  /**
   * @throws IllegalStateException when bodyMode is anything other than ResponseMode.MUTABLE
   */
  def setOutput(inputStream: InputStream): Unit = ???

  /**
   * @throws IllegalStateException when neither headerMode nor bodyMode are ResponseMode.MUTABLE
   */
  def commitToResponse(): Unit = ???


}
