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

import java.io.{BufferedReader, InputStreamReader}
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util
import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED

import org.apache.commons.lang3.StringUtils
import org.apache.http.client.utils.DateUtils
import org.openrepose.commons.utils.io.RawInputStreamReader

import scala.collection.JavaConverters._
import scala.collection.immutable.{TreeMap, TreeSet}
import scala.collection.mutable

class HttpServletRequestWrapper(originalRequest: HttpServletRequest,
                                val inputStream: ServletInputStream)
  extends javax.servlet.http.HttpServletRequestWrapper(originalRequest)
    with HeaderInteractor {

  import HttpServletRequestWrapper._

  private var status = RequestBodyStatus.Available
  private var method: String = originalRequest.getMethod
  private var scheme: String = originalRequest.getScheme
  private var serverName: String = originalRequest.getServerName
  private var protocol: String = originalRequest.getProtocol
  private var serverPort: Int = originalRequest.getServerPort
  private var requestUri: String = originalRequest.getRequestURI
  private var queryString: String = originalRequest.getQueryString
  private var parameterMap: Option[Map[String, Array[String]]] = None
  private var formParameterMap: Option[Map[String, Array[String]]] = None
  private var headerMap: Map[String, List[String]] = new TreeMap[String, List[String]]()(caseInsensitiveOrdering)
  private var removedHeaders: Set[String] = new TreeSet[String]()(caseInsensitiveOrdering)

  def this(originalRequest: HttpServletRequest) = this(originalRequest, originalRequest.getInputStream)

  override def getInputStream: ServletInputStream = {
    if (status == RequestBodyStatus.Reader) throw new IllegalStateException else status = RequestBodyStatus.InputStream
    inputStream
  }

  override def getReader: BufferedReader = {
    if (status == RequestBodyStatus.InputStream) throw new IllegalStateException else status = RequestBodyStatus.Reader
    new BufferedReader(new InputStreamReader(inputStream))
  }

  override def getHeaderNames: util.Enumeration[String] = getHeaderNamesScala.toIterator.asJavaEnumeration

  def getHeaderNamesScala: Set[String] = headerMap.keySet ++ Option(super.getHeaderNames).getOrElse(util.Collections.emptyEnumeration).asScala.toSet.diff(removedHeaders)

  override def getHeaderNamesList: util.List[String] = getHeaderNamesScala.toList.asJava

  override def getIntHeader(headerName: String): Int = Option(getHeader(headerName)).getOrElse("-1").toInt

  override def getHeader(headerName: String): String = getHeadersScala(headerName).headOption.orNull

  override def getHeaders(headerName: String): util.Enumeration[String] = getHeadersScala(headerName).toIterator.asJavaEnumeration

  override def getDateHeader(headerName: String): Long = Option(getHeader(headerName)).map(DateUtils.parseDate(_).getTime).getOrElse(-1)

  override def getHeadersList(headerName: String): util.List[String] = getHeadersScala(headerName).asJava

  override def addHeader(headerName: String, headerValue: String, quality: Double): Unit = addHeader(headerName, headerValue + ";q=" + quality)

  override def appendHeader(headerName: String, headerValue: String, quality: Double): Unit = appendHeader(headerName, headerValue + ";q=" + quality)

  override def appendHeader(headerName: String, headerValue: String): Unit = {
    val existingHeaders: List[String] = getHeadersScala(headerName)
    existingHeaders.headOption match {
      case Some(value) =>
        val newHeadValue: String = value + "," + headerValue
        headerMap = headerMap + (headerName -> (newHeadValue +: existingHeaders.tail))
      case None => addHeader(headerName, headerValue)
    }
  }

  override def addHeader(headerName: String, headerValue: String): Unit = {
    val existingHeaders: List[String] = getHeadersScala(headerName) //this has to be done before we remove from the list,
    // because getting this list is partially based on the contents of the removed list
    removedHeaders = removedHeaders - headerName
    headerMap = headerMap + (headerName -> (existingHeaders :+ headerValue))
  }

  override def removeHeader(headerName: String): Unit = {
    removedHeaders = removedHeaders + headerName
    headerMap = headerMap - headerName
  }

  override def getPreferredHeaders(headerName: String): util.List[String] =
    getPreferredHeader(headerName, getHeadersScala).map(_.value).asJava

  override def getPreferredHeadersWithParameters(headerName: String): util.List[String] =
    getPreferredHeader(headerName, getHeadersScala).map(_.headerValue).asJava

  override def getPreferredSplittableHeaders(headerName: String): util.List[String] =
    getPreferredHeader(headerName, getSplittableHeaderScala).map(_.value).asJava

  override def getPreferredSplittableHeadersWithParameters(headerName: String): util.List[String] =
    getPreferredHeader(headerName, getSplittableHeaderScala).map(_.headerValue).asJava

  private def getPreferredHeader(headerName: String, getFun: String => List[String]): List[HeaderValue] = {
    getFun(headerName) match {
      case Nil => Nil
      case nonEmptyList =>
        nonEmptyList.map(HeaderValue) // parse the header value string
          .groupBy(_.quality) // group by quality
          .maxBy(_._1) // find the highest quality group
          ._2 // get the list of highest quality values
    }
  }

  def getSplittableHeaderScala(headerName: String): List[String] =
    getHeadersScala(headerName).foldLeft(List.empty[String])((list, s) => list ++ s.split(","))
      .map(_.trim)

  def getHeadersScala(headerName: String): List[String] = {
    if (removedHeaders.contains(headerName)) {
      List.empty[String]
    } else {
      headerMap.getOrElse(headerName, super.getHeaders(headerName).asScala.toList)
    }
  }

  override def replaceHeader(headerName: String, headerValue: String, quality: Double): Unit = replaceHeader(headerName, headerValue + ";q=" + quality)

  override def replaceHeader(headerName: String, headerValue: String): Unit = {
    headerMap = headerMap + (headerName -> List(headerValue))
    removedHeaders = removedHeaders - headerName
  }

  override def getSplittableHeaders(headerName: String): util.List[String] = getSplittableHeaderScala(headerName).asJava

  /**
    * @return a [[StringBuffer]] containing the reconstructed URL for this request (note that
    *         mutation of this [[StringBuffer]] will have no effect on the request URL)
    */
  override def getRequestURL: StringBuffer = {
    val url: StringBuffer = new StringBuffer(getScheme).append("://").append(getServerName)

    if (getServerPort > 0 && ((HTTP.equalsIgnoreCase(getScheme) && getServerPort != 80) || (HTTPS.equalsIgnoreCase(getScheme) && getServerPort != 443))) {
      url.append(':').append(getServerPort)
    }

    if (Option(getRequestURI).exists(_.nonEmpty)) {
      url.append(getRequestURI)
    }

    url
  }

  override def getRequestURI: String = requestUri

  def setRequestURI(uri: String): Unit = {
    if (Option(uri).isEmpty) throw new IllegalArgumentException("null is not a legal argument to setRequestURI")

    requestUri = uri
  }

  override def getProtocol: String = protocol

  def setProtocol(protocol: String): Unit = this.protocol = protocol

  override def getServerPort: Int = serverPort

  def setServerPort(port: Int): Unit = this.serverPort = port

  override def getMethod: String = method

  def setMethod(method: String): Unit = this.method = method

  override def getScheme: String = scheme

  def setScheme(scheme: String): Unit = this.scheme = scheme

  override def getServerName: String = serverName

  def setServerName(name: String): Unit = this.serverName = name

  /**
    * @param key a parameter key
    * @return the first parameter value associated with the provided key for this request, or null if no value exists
    */
  override def getParameter(key: String): String =
    Option(getParameterValues(key)).map(_.head).orNull

  /**
    * @param key a parameter key
    * @return all parameter values associated with the provided key for this request
    */
  override def getParameterValues(key: String): Array[String] =
    getParameterMap.get(key)

  /**
    * @return all parameter names for this request
    */
  override def getParameterNames: util.Enumeration[String] =
    util.Collections.enumeration(getParameterMap.keySet())

  /**
    * @return a string representation of the query parameters for this request
    */
  override def getQueryString: String = queryString

  /**
    * @param newQueryString the desired raw (i.e., already encoded where necessary) query string for this request
    */
  def setQueryString(newQueryString: String): Unit = {
    val updatedParameterMap = mutable.Map.empty[String, Array[String]]
    val curQueryMap = Option(getQueryString).map(parseParameterString).getOrElse(Map.empty[String, Array[String]])

    // Remove all current query parameters from the parameter map
    formParameterMap match {
      case Some(fpm) =>
        updatedParameterMap ++= fpm
      case None =>
        getParameterMap.asScala foreach { case (key, values) =>
          val formValues = mutable.ArrayBuffer(values: _*)
          curQueryMap.get(key).foreach(queryValues => queryValues.foreach(formValues.-=))

          if (formValues.nonEmpty) updatedParameterMap += (key -> formValues.toArray)
        }
        formParameterMap = Option(updatedParameterMap.toMap)
    }
    // Add the new query parameters to the parameter map with the query parameters preceding the form parameters
    insertParameters(Option(newQueryString).map(parseParameterString).getOrElse(Map.empty[String, Array[String]]), updatedParameterMap)

    parameterMap = Option(updatedParameterMap.toMap)
    queryString = newQueryString
  }

  /** Returns the parameter map containing all form and query parameters for this request. Note that form parameters
    * are only modifiable "manually" by manipulating the body of this request. Changes to form parameters in the body
    * of this request will not be reflected in this parameter map.
    *
    * Note that, for application/x-www-form-urlencoded requests, super.getParameterMap is not called. As a result,
    * if the wrapped request stores parameters as internal state (e.g., reading form encoded parameters into a map),
    * this wrapper will not be able to retrieve or return those parameters. This wrapper only retrieves parameters
    * from the query string and the request body. While unfortunate, there does not seem to be a way around this
    * behavior while remaining strictly servlet specification compliant. The reason is that getParameterMap and
    * getInputStream cannot both be called when parameters exist in the body -- once one is called, the other will
    * not be able to access the unread body stream, and so will not be able to return correct and complete data.
    *
    * @return the parameter map for this request
    */
  override def getParameterMap: util.Map[String, Array[String]] = {
    def retrieveParameterMap: util.Map[String, Array[String]] = {
      // As per Servlet Spec 3.1 section 3.1.1, form parameters are only parsed under certain conditions.
      if (getContentLength > 0 && APPLICATION_FORM_URLENCODED.equalsIgnoreCase(getContentType) &&
        ("POST".equalsIgnoreCase(getMethod) || "PUT".equalsIgnoreCase(getMethod))) {
        val updatedParameterMap = mutable.Map.empty[String, Array[String]]
        formParameterMap match {
          case Some(fpm) =>
            updatedParameterMap ++= fpm
          case None =>
            // As per Servlet Spec 3.1 section 3.1.1, form parameters are only available until the input stream is read.
            try {
              updatedParameterMap ++= parseParameterString(
                new String(RawInputStreamReader.instance.readFully(getInputStream, getContentLength), StandardCharsets.UTF_8))
            } catch {
              case _: IllegalStateException => // Just consume it since the stream has already been read.
            }
            formParameterMap = Option(updatedParameterMap.toMap)
        }
        // Add the query parameters to the parameter map preceding the form parameters already added.
        insertParameters(Option(getQueryString).map(parseParameterString).getOrElse(Map.empty[String, Array[String]]), updatedParameterMap)

        parameterMap = Option(updatedParameterMap.toMap)
        parameterMap.getOrElse(Map.empty[String, Array[String]]).asJava
      } else {
        super.getParameterMap
      }
    }

    parameterMap.map(_.asJava).getOrElse(retrieveParameterMap)
  }
}

object HttpServletRequestWrapper {
  private final val HTTP = "http"
  private final val HTTPS = "https"
  private final val QueryPairDelimiter = "&"
  private final val QueryKeyValueDelimiter = "="

  private val caseInsensitiveOrdering = Ordering.by[String, String](_.toLowerCase)

  object RequestBodyStatus extends Enumeration {
    val Available, InputStream, Reader = Value
  }

  private def parseParameterString(parameterString: String): Map[String, Array[String]] = {
    val parsedParameterMap = mutable.Map.empty[String, Array[String]]
    val nullSafeParameterString = Option(parameterString).getOrElse("")

    if (nullSafeParameterString.nonEmpty) {
      nullSafeParameterString.split(QueryPairDelimiter) foreach { queryPair =>
        val keyValuePair = queryPair.split(QueryKeyValueDelimiter, 2)
        val key = URLDecoder.decode(keyValuePair(0), StandardCharsets.UTF_8.toString)
        if (keyValuePair.length == 2) {
          val value = URLDecoder.decode(keyValuePair(1), StandardCharsets.UTF_8.toString)
          parsedParameterMap += (key -> parsedParameterMap.getOrElse(key, Array.empty[String]).:+(value))
        } else {
          parsedParameterMap += (key -> parsedParameterMap.getOrElse(key, Array.empty[String]).:+(""))
        }
      }
    }

    parsedParameterMap.toMap
  }

  def parseQueryString(queryString: String): java.util.Map[String, Array[String]] = parseParameterString(queryString).asJava

  private def insertParameters(insertMap: Map[String, Array[String]], intoMap: mutable.Map[String, Array[String]]) = {
    insertMap foreach { case (key, values) =>
      intoMap += (key -> (values ++ intoMap.getOrElse(key, Array.empty[String])))
    }
  }
}
