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

import org.apache.http.client.utils.DateUtils

import scala.collection.JavaConverters._
import scala.collection.immutable.{TreeMap, TreeSet}
import scala.collection.mutable

class HttpServletRequestWrapper(originalRequest: HttpServletRequest, inputStream: ServletInputStream)
  extends javax.servlet.http.HttpServletRequestWrapper(originalRequest) with HeaderInteractor {

  import HttpServletRequestWrapper._

  private var status = RequestBodyStatus.Available
  private var scheme: String = originalRequest.getScheme
  private var serverName: String = originalRequest.getServerName
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

  def getHeaderNamesScala: Set[String] = headerMap.keySet ++ Option(super.getHeaderNames).getOrElse(util.Collections.emptyEnumeration).asScala.toSet.filterNot(removedHeaders.contains)

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
      List[String]()
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

  // todo: getProtocol?

  override def getServerPort: Int = serverPort

  def setServerPort(port: Int): Unit = this.serverPort = port

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
    parameterMap.map(_.get(key).orNull).getOrElse(super.getParameterValues(key))

  /**
    * @return all parameter names for this request
    */
  override def getParameterNames: util.Enumeration[String] =
    parameterMap.map(_.keysIterator.asJavaEnumeration).getOrElse(super.getParameterNames)

  /**
    * @return a string representation of the query parameters for this request
    */
  override def getQueryString: String = queryString

  /**
    * @param newQueryString the desired raw (i.e., already encoded where necessary) query string for this request
    */
  def setQueryString(newQueryString: String): Unit = {
    def parseQueryString(s: String): Map[String, Array[String]] = {
      val parameterMap = mutable.Map.empty[String, Array[String]]

      s.split(QueryPairDelimiter) foreach { queryPair =>
        val keyValuePair = queryPair.split(QueryKeyValueDelimiter, 2)

        /**
          * Note: Decoding using UTF-8 is consistent with the processing performed by [[HttpComponentRequestProcessor]]
          * on request parameters. However, if the JVM default encoding is not UTF-8, decoding may not work as expected.
          * Perhaps the default JVM encoding should be used instead?
          */
        val key = URLDecoder.decode(keyValuePair(0), StandardCharsets.UTF_8.toString)
        if (keyValuePair.length == 2) {
          val value = URLDecoder.decode(keyValuePair(1), StandardCharsets.UTF_8.toString)
          parameterMap += (key -> parameterMap.getOrElse(key, Array.empty[String]).:+(value))
        } else {
          parameterMap += (key -> parameterMap.getOrElse(key, Array.empty[String]).:+(""))
        }
      }

      parameterMap.toMap
    }

    val updatedParameterMap = mutable.Map.empty[String, Array[String]]
    val curQueryMap = Option(getQueryString).map(parseQueryString).getOrElse(Map.empty[String, Array[String]])
    val newQueryMap = Option(newQueryString).map(parseQueryString).getOrElse(Map.empty[String, Array[String]])

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

    // Add all new query parameters to the parameter map with query parameters preceding form parameters
    newQueryMap foreach { case (key, values) =>
      updatedParameterMap += (key -> (values ++ updatedParameterMap.getOrElse(key, Array.empty[String])))
    }

    parameterMap = Option(updatedParameterMap.toMap)
    queryString = newQueryString
  }

  /** Returns the parameter map containing all form and query parameters for this request. Note that form parameters
    * are only modifiable "manually" by manipulating the body of this request. Changes to form parameters in the body
    * of this request will no be reflected in this parameter map.
    *
    * @return the parameter map for this request
    */
  override def getParameterMap: util.Map[String, Array[String]] =
    parameterMap.map(_.asJava).getOrElse(super.getParameterMap)
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

}
