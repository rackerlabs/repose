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

import java.io.{InputStream, OutputStreamWriter, PrintWriter, UnsupportedEncodingException}
import java.nio.charset.{Charset, StandardCharsets}
import java.util
import java.util.Date
import javax.servlet.http.HttpServletResponse
import javax.servlet.{ServletOutputStream, ServletResponse}

import org.apache.http.client.utils.DateUtils
import org.openrepose.commons.utils.http.CommonHttpHeader

import scala.collection.JavaConversions._
import scala.collection.immutable.TreeMap
import scala.io.Source

/**
  * This class wraps a HttpServletResponse applying further functionality. It allows for varying levels of read
  * and writeability for both headers and body. Both headers and body offer three modes, although in the case of headers
  * two are functionally identical. If either option is set to ResponseMode.MUTABLE, commitToResponse must be called
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
  * ResponseMode.PASSTHROUGH. ResponseMode.MUTABLE behaves similarly, but will not write to the underlying output stream
  * until commitToResponse is called. Additionally, you can call setOutput with an input stream which we will wrap in an
  * output stream which will be used to write to the underlying output stream when commitToResponse is called. This mode
  * will also break chunked encoding because the response can't be streamed directly through.
  *
  * Note that mutation to the [[HttpServletResponse]] will be treated by this wrapper as permanent. Therefore, mutation
  * should not be performed prior to downstream processing. In practice, mutation should not be performed on the response
  * prior to calling the doFilter() method. The reason is that writing to a [[ServletOutputStream]] or
  * [[PrintWriter]] can not be undone, so for the sake of consistency, headers are handled in the same manner. In
  * fact, headers written prior to wrapping a response will not appear to exist within the wrapped response.
  *
  * @constructor the main constructor to be used for this class
  * @param originalResponse    the response to be wrapped
  * @param headerMode          the read/write-ability mode of headers
  * @param bodyMode            the read/write-ability mode of the body
  * @param desiredOutputStream the underlying output stream to be presented to callers of getOutputStream, maybe wrapped
  *                            depending on mode selection of the body
  */
class HttpServletResponseWrapper(originalResponse: HttpServletResponse, headerMode: ResponseMode, bodyMode: ResponseMode, desiredOutputStream: ServletOutputStream)
  extends javax.servlet.http.HttpServletResponseWrapper(originalResponse) with HeaderInteractor {

  private val caseInsensitiveOrdering = Ordering.by[String, String](_.toLowerCase)
  private val bodyOutputStream = bodyMode match {
    case ResponseMode.PASSTHROUGH => new PassthroughServletOutputStream(desiredOutputStream)
    case ResponseMode.READONLY => new ReadOnlyServletOutputStream(desiredOutputStream)
    case ResponseMode.MUTABLE => new MutableServletOutputStream(desiredOutputStream)
  }

  private var reason: Option[String] = None
  private var committed: Boolean = false
  private var flushedBuffer: Boolean = false
  private var sentError: Boolean = false
  private var responseBodyType: ResponseBodyType.Value = ResponseBodyType.Available
  private var bodyPrintWriter: PrintWriter = _
  private var characterEncoding: String = StandardCharsets.ISO_8859_1.toString
  private var headerMap: Map[String, Seq[String]] = new TreeMap[String, Seq[String]]()(caseInsensitiveOrdering)

  /**
    * This constructor chains to the main constructor using the original responses output stream  as the last argument.
    *
    * @constructor the constructor to use when you don't plan on using a custom stream for processing
    * @param originalResponse the response to be wrapped
    * @param headerMode       the mode to use for header accessibility
    * @param bodyMode         the mode to use for body accessibility
    */
  def this(originalResponse: HttpServletResponse, headerMode: ResponseMode, bodyMode: ResponseMode) =
    this(originalResponse, headerMode, bodyMode, originalResponse.getOutputStream)

  override def setStatus(i: Int): Unit = {
    if (isCommitted) {
      throw new IllegalStateException("Cannot call sendError or setStatus after the response has been committed")
    } else {
      super.setStatus(i)
      reason = None
    }
  }

  override def setStatus(i: Int, s: String): Unit = {
    if (isCommitted) {
      throw new IllegalStateException("Cannot call setStatus after the response has been committed")
    } else {
      super.setStatus(i, s)
      reason = Option(s)
    }
  }

  /** Sets a status code on the first line of the HTTP response. Also sets the error message to be retrieved and used
    * later.
    *
    * Note that this method will not call through to the [[sendError(i)]] method of the wrapped response.
    * As a result, the container will not have an opportunity to perform additional processing.
    *
    * @param i the status code to set
    */
  override def sendError(i: Int): Unit = {
    if (isCommitted) {
      throw new IllegalStateException("Cannot call sendError after the response has been committed")
    }

    // Set the status.
    setStatus(i)

    // Set the reason phrase.
    reason = None

    // Reset the buffered output.
    resetBuffer()

    // Track that the user intended to send an error.
    sentError = true
    committed = true

    // If we are not in a mutable mode, immediately send error to the wrapped response.
    if (headerMode != ResponseMode.MUTABLE && bodyMode != ResponseMode.MUTABLE) {
      originalResponse.sendError(i)
    }
  }

  /** See [[sendError(i)]].
    *
    * @param i the status code to set
    * @param s the string used to populate the response body
    */
  override def sendError(i: Int, s: String): Unit = {
    if (isCommitted) {
      throw new IllegalStateException("Cannot call sendError after the response has been committed")
    }

    // Set the status.
    setStatus(i)

    // Set the reason phrase.
    reason = Option(s)

    // Call resetBuffer() so that we can write directly to a clean output stream, even if the client has previously
    // written to the output stream.
    resetBuffer()

    // Track that the user intended to send an error.
    sentError = true
    committed = true

    // If we are not in a mutable mode, immediately send error to the wrapped response.
    if (headerMode != ResponseMode.MUTABLE && bodyMode != ResponseMode.MUTABLE) {
      originalResponse.sendError(i, s)
    }
  }

  def isError: Boolean = sentError

  def getReason: String = reason.orNull

  override def isCommitted: Boolean = super.isCommitted || committed

  override def getResponse: ServletResponse = throw new UnsupportedOperationException("getResponse is not supported")

  override def setResponse(servletResponse: ServletResponse): Unit =
    throw new UnsupportedOperationException("setResponse is not supported")

  override def getHeaderNamesList: util.List[String] = getHeaderNames.toList

  override def getHeaderNames: util.Collection[String] = headerMap.keys

  override def containsHeader(name: String): Boolean = headerMap.contains(name)

  override def getHeaders(name: String): util.Collection[String] = getHeaderValues(name)

  override def getPreferredHeaders(name: String): util.List[String] =
    getPreferredHeaderValues(getHeaderValues(name)).map(_.value)

  override def getPreferredHeadersWithParameters(name: String): util.List[String] =
    getPreferredHeaderValues(getHeaderValues(name)).map(_.headerValue)

  override def getPreferredSplittableHeaders(name: String): util.List[String] =
    getPreferredHeaderValues(getSplittableHeaders(name)).map(_.value)

  override def getPreferredSplittableHeadersWithParameters(name: String): util.List[String] =
    getPreferredHeaderValues(getSplittableHeaders(name)).map(_.headerValue)

  private def getPreferredHeaderValues(values: Seq[String]): Seq[HeaderValue] = {
    values match {
      case Nil => Nil
      case nonEmptyList =>
        nonEmptyList.map(HeaderValue) // parse the header value string
          .groupBy(_.quality) // group by quality
          .maxBy(_._1) // find the highest quality group
          ._2 // get the list of highest quality values
    }
  }

  override def getSplittableHeaders(name: String): util.List[String] =
    getHeaderValues(name).foldLeft(List.empty[String])((list, value) => list ++ value.split(","))
      .map(_.trim)

  private def getHeaderValues(name: String): Seq[String] = headerMap.getOrElse(name, Seq.empty[String])

  override def addHeader(name: String, value: String, quality: Double): Unit =
    addHeader(name, s"$value;q=$quality")

  override def addIntHeader(name: String, value: Int): Unit = addHeader(name, value.toString)

  /**
    * Formats the input time (as milliseconds since the epoch) to a format defined in RFC2616.
    */
  override def addDateHeader(name: String, timeSinceEpoch: Long): Unit =
    addHeader(name, DateUtils.formatDate(new Date(timeSinceEpoch)))

  override def addHeader(name: String, value: String): Unit = {
    headerMap = headerMap + (name -> (headerMap.getOrElse(name, Seq.empty[String]) :+ value))

    // Write through to the wrapped response immediately
    if (headerMode != ResponseMode.MUTABLE) {
      super.addHeader(name, value)
    }
  }

  /**
    * @throws IllegalStateException when headerMode is anything other than ResponseMode.MUTABLE
    */
  override def appendHeader(name: String, value: String, quality: Double): Unit =
    appendHeader(name, s"$value;q=$quality")

  /**
    * @throws IllegalStateException when headerMode is anything other than ResponseMode.MUTABLE
    */
  override def appendHeader(name: String, value: String): Unit = {
    ifMutable(headerMode) {
      val existingValues = getHeadersList(name)
      existingValues.headOption match {
        case Some(currentHeadValue) =>
          val newHeadValue = currentHeadValue + "," + value
          headerMap = headerMap + (name -> (newHeadValue +: existingValues.tail))
        case None => addHeader(name, value)
      }
    }
  }

  override def getHeadersList(name: String): util.List[String] = getHeaderValues(name)

  private def ifMutable[T](mode: ResponseMode)(processMutable: => T): T = {
    mode match {
      case ResponseMode.MUTABLE =>
        processMutable
      case _ =>
        throw new IllegalStateException("method should not be called if the ResponseMode is not set to MUTABLE")
    }
  }

  override def setDateHeader(name: String, timeSinceEpoch: Long): Unit =
    setHeader(name, DateUtils.formatDate(new Date(timeSinceEpoch)))

  /**
    * @throws IllegalStateException when headerMode is anything other than ResponseMode.MUTABLE
    */
  override def removeHeader(name: String): Unit = {
    ifMutable(headerMode) {
      headerMap = headerMap - name
    }
  }

  override def replaceHeader(name: String, value: String): Unit = setHeader(name, value)

  override def replaceHeader(name: String, value: String, quality: Double): Unit = setHeader(name, s"$value;q=$quality")

  override def setHeader(name: String, value: String): Unit = {
    headerMap = headerMap + (name -> Seq(value))

    if (headerMode != ResponseMode.MUTABLE) {
      // Write through to the wrapped response immediately
      super.setHeader(name, value)
    }
  }

  override def setContentLength(contentLength: Int): Unit = {
    if (!isCommitted) {
      setIntHeader(CommonHttpHeader.CONTENT_LENGTH, contentLength)
    }
  }

  override def setIntHeader(name: String, value: Int): Unit = setHeader(name, value.toString)

  override def getContentType: String = getHeader(CommonHttpHeader.CONTENT_TYPE)

  override def setContentType(contentType: String): Unit = {
    if (!isCommitted) {
      val charEncRegex = """;\s*charset\s*=\s*([^;]+)""".r
      val charEnc = charEncRegex.findFirstMatchIn(contentType).map(_.group(1))
      val modifiedContentType = contentType.replaceAll(""";\s*charset\s*=\s*[^;]+""", "")

      setHeader(CommonHttpHeader.CONTENT_TYPE, modifiedContentType)
      charEnc.foreach(setCharacterEncoding)
    }
  }

  override def setCharacterEncoding(charEncoding: String): Unit = {
    if (!isCommitted && responseBodyType != ResponseBodyType.PrintWriter) {
      // Verify that the provided character encoding is valid
      val charset = Charset.forName(charEncoding)

      if (!Charset.isSupported(charEncoding) || !charset.isRegistered) {
        // Verify that the charset is supported and registered with IANA
        throw new UnsupportedEncodingException("setCharacterEncoding: " + charEncoding + " is not a supported encoding")
      } else {
        characterEncoding = charEncoding
        Option(getHeader(CommonHttpHeader.CONTENT_TYPE))
          .map(_.replaceAll(""";\s*charset\s*=\s*[^;]+""", "")) // Strip out the current character encoding
          .foreach(contentType => setHeader(CommonHttpHeader.CONTENT_TYPE, contentType + ";charset=" + charEncoding))
      }
    }
  }

  /**
    * @throws IllegalStateException when bodyMode is ResponseMode.PASSTHROUGH
    */
  def getOutputStreamAsInputStream: InputStream = bodyOutputStream.getOutputStreamAsInputStream

  /**
    * @throws IllegalStateException when bodyMode is ResponseMode.PASSTHROUGH
    */
  def getOutputStreamAsString: String =
    Source.fromInputStream(getOutputStreamAsInputStream, getCharacterEncoding).mkString

  /**
    * @throws IllegalStateException when bodyMode is anything other than ResponseMode.MUTABLE
    */
  def setOutput(inputStream: InputStream): Unit = {
    responseBodyType = ResponseBodyType.Available
    bodyOutputStream.setOutput(inputStream)
  }

  override def getWriter: PrintWriter = {
    responseBodyType match {
      case ResponseBodyType.OutputStream =>
        throw new IllegalStateException("Cannot call getWriter after calling getOutputStream")
      case ResponseBodyType.Available =>
        responseBodyType = ResponseBodyType.PrintWriter
        /** TODO: Should we wrap this with our own writer? The data a user writes to a PrintWriter may be buffered.
          * Buffered content must be flushed from the PrintWriter to make it to the output stream, however,
          * flushing the PrintWriter poses two potential problems:
          * 1. It commits the response. Must the user commit the response if they want to write to the body?
          * 2. How does this wrapper know if the PrintWriter has been flushed? How can this wrapper report
          * being committed if the PrintWriter is flushed?
          */
        bodyPrintWriter = new PrintWriter(new OutputStreamWriter(bodyOutputStream, getCharacterEncoding))
        bodyPrintWriter
      case ResponseBodyType.PrintWriter =>
        bodyPrintWriter
    }
  }

  override def getCharacterEncoding: String = {
    Option(getHeader(CommonHttpHeader.CONTENT_TYPE)) flatMap { contentType =>
      val charEncRegex = """;\s*charset\s*=\s*([^;]+)""".r
      charEncRegex.findFirstMatchIn(contentType).map(_.group(1))
    } getOrElse characterEncoding
  }

  override def getHeader(name: String): String = getHeaderValues(name).headOption.orNull

  override def getOutputStream: ServletOutputStream = {
    responseBodyType match {
      case ResponseBodyType.PrintWriter =>
        throw new IllegalStateException("Cannot call getOutputStream after calling getWriter")
      case _ =>
        responseBodyType = ResponseBodyType.OutputStream
        bodyOutputStream
    }
  }

  override def flushBuffer(): Unit = {
    // Flush any buffered output from the writer which will, in turn, flush the underlying OutputStream.
    // Flush the raw OutputStream for consistency.
    // Note: Flushing the raw OutputStream should be a no-op.
    responseBodyType match {
      case ResponseBodyType.PrintWriter => bodyPrintWriter.flush()
      case _ => bodyOutputStream.flush()
    }

    // If we are not in a mutable mode, immediately flush the wrapped response.
    if (headerMode != ResponseMode.MUTABLE && bodyMode != ResponseMode.MUTABLE) {
      originalResponse.flushBuffer()
    }

    // Track that the user intended to commit the response.
    flushedBuffer = true
    committed = true
  }

  /**
    * @throws IllegalStateException when the wrapped response has already been committed
    */
  def uncommit(): Unit = {
    if (originalResponse.isCommitted) {
      throw new IllegalStateException("the wrapped response has already been committed")
    }

    committed = false
  }

  def resetError(): Unit = {
    if (isCommitted) {
      throw new IllegalStateException("Cannot call resetError after the response has been committed")
    }

    reason = None
    sentError = false
  }

  override def reset(): Unit = {
    if (isCommitted) {
      throw new IllegalStateException("Cannot call reset after the response has been committed")
    } else {
      super.reset()
      headerMap = new TreeMap[String, Seq[String]]()(caseInsensitiveOrdering)
      resetBuffer()
    }
  }

  override def resetBuffer(): Unit = {
    if (isCommitted) {
      throw new IllegalStateException("Cannot call resetBuffer after the response has been committed")
    } else {
      super.resetBuffer()
      responseBodyType = ResponseBodyType.Available
      bodyOutputStream.resetBuffer()
    }
  }

  /**
    * @throws IllegalStateException when neither headerMode nor bodyMode is ResponseMode.MUTABLE
    */
  def commitToResponse(): Unit = {
    def writeHeaders(): Unit = {
      headerMap foreach { case (name, values) =>
        values foreach { value =>
          super.addHeader(name, value)
        }
      }
    }

    def writeBody(): Unit = {
      // Since headers may have already been written, we set the content length on the wrapped response directly.
      super.setContentLength(bodyOutputStream.getOutputStreamAsInputStream.available())
      bodyOutputStream.commit()
    }

    (headerMode, bodyMode) match {
      case (ResponseMode.MUTABLE, ResponseMode.MUTABLE) =>
        // The headers are being written first so that they are available for processing by upstream
        // output streams. The Compressing filter output stream, for example, depends on the content-type header
        // being set before the output stream is written to.
        writeHeaders()
        writeBody()
      case (ResponseMode.MUTABLE, _) =>
        writeHeaders()
      case (_, ResponseMode.MUTABLE) =>
        writeBody()
      case (_, _) =>
        throw new IllegalStateException("method should not be called if the ResponseMode is not set to MUTABLE")
    }

    if (sentError) {
      reason match {
        case Some(msg) => originalResponse.sendError(getStatus, msg)
        case None => originalResponse.sendError(getStatus)
      }
    } else if (flushedBuffer) {
      originalResponse.flushBuffer()
    }

    committed = true
  }

  private case class HeaderValue(headerValue: String) {
    val value = headerValue.split(";").head
    val quality = {
      try {
        val headerParameters: Array[String] = headerValue.split(";").tail
        val qualityParameter: Option[String] = headerParameters.find(param => "q".equalsIgnoreCase(param.split("=").head.trim))
        qualityParameter.map(_.split("=", 2)(1).toDouble).getOrElse(1.0)
      } catch {
        case e: NumberFormatException => throw new QualityFormatException("Quality was an unparseable value", e)
      }
    }
  }

  object ResponseBodyType extends Enumeration {
    val Available, OutputStream, PrintWriter = Value
  }

}
