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
import java.util
import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest

import org.apache.http.client.utils.DateUtils

import scala.collection.JavaConverters._
import scala.collection.immutable.{ListMap, TreeMap, TreeSet}
import scala.collection.mutable

class HttpServletRequestWrapper(originalRequest: HttpServletRequest, inputStream: ServletInputStream)
  extends javax.servlet.http.HttpServletRequestWrapper(originalRequest)
  with HeaderInteractor {

  object RequestBodyStatus extends Enumeration {
    val Available, InputStream, Reader = Value
  }

  private var status = RequestBodyStatus.Available

  def this(originalRequest: HttpServletRequest) = this(originalRequest, originalRequest.getInputStream)

  val caseInsensitiveOrdering = Ordering.by[String, String](_.toLowerCase)

  private var requestUri: String = originalRequest.getRequestURI
  private var requestUrl: StringBuffer = originalRequest.getRequestURL
  private var queryParameterMap: Option[ListMap[String, Array[String]]] = None
  private var headerMap: Map[String, List[String]] = new TreeMap[String, List[String]]()(caseInsensitiveOrdering)
  private var removedHeaders: Set[String] = new TreeSet[String]()(caseInsensitiveOrdering)

  override def getInputStream: ServletInputStream = {
    if (status == RequestBodyStatus.Reader) throw new IllegalStateException else status = RequestBodyStatus.InputStream
    inputStream
  }

  override def getReader: BufferedReader = {
    if (status == RequestBodyStatus.InputStream) throw new IllegalStateException else status = RequestBodyStatus.Reader
    new BufferedReader(new InputStreamReader(inputStream))
  }

  def getHeaderNamesScala: Set[String] = headerMap.keySet ++ Option(super.getHeaderNames).getOrElse(util.Collections.emptyEnumeration).asScala.toSet.filterNot(removedHeaders.contains)

  override def getHeaderNames: util.Enumeration[String] = getHeaderNamesScala.toIterator.asJavaEnumeration

  override def getHeaderNamesList: util.List[String] = getHeaderNamesScala.toList.asJava

  override def getIntHeader(headerName: String): Int = Option(getHeader(headerName)).getOrElse("-1").toInt

  def getHeadersScala(headerName: String): List[String] = {
    if (removedHeaders.contains(headerName)) {
      List[String]()
    } else {
      headerMap.getOrElse(headerName, super.getHeaders(headerName).asScala.toList)
    }
  }

  override def getHeaders(headerName: String): util.Enumeration[String] = getHeadersScala(headerName).toIterator.asJavaEnumeration

  override def getDateHeader(headerName: String): Long = Option(getHeader(headerName)).map(DateUtils.parseDate(_).getTime).getOrElse(-1)

  override def getHeader(headerName: String): String = getHeadersScala(headerName).headOption.orNull

  override def getHeadersList(headerName: String): util.List[String] = getHeadersScala(headerName).asJava

  override def addHeader(headerName: String, headerValue: String): Unit = {
    val existingHeaders: List[String] = getHeadersScala(headerName) //this has to be done before we remove from the list,
                                                                    // because getting this list is partially based on the contents of the removed list
    removedHeaders = removedHeaders - headerName
    headerMap = headerMap + (headerName -> (existingHeaders :+ headerValue))
  }

  override def addHeader(headerName: String, headerValue: String, quality: Double): Unit = addHeader(headerName, headerValue + ";q=" + quality)

  override def appendHeader(headerName: String, headerValue: String): Unit = {
    val existingHeaders: List[String] = getHeadersScala(headerName)
    existingHeaders.headOption match {
      case Some(value) =>
        val newHeadValue: String = value + "," + headerValue
        headerMap = headerMap + (headerName -> (newHeadValue +: existingHeaders.tail))
      case None => addHeader(headerName, headerValue)
    }
  }

  override def appendHeader(headerName: String, headerValue: String, quality: Double): Unit = appendHeader(headerName, headerValue + ";q=" + quality)

  override def removeHeader(headerName: String): Unit = {
    removedHeaders = removedHeaders + headerName
    headerMap = headerMap - headerName
  }

  private def getPreferredHeader(headerName: String, getFun: String => List[String]): List[String] = {
    case class HeaderValue(headerValue: String) {
      val value = headerValue.split(";").head
      val quality = {
        try {
          val headerParameters: Array[String] = headerValue.split(";").tail
          val qualityParameters: Option[String] = headerParameters.find(param => "q".equalsIgnoreCase(param.split("=").head.trim))
          qualityParameters.map(_.split("=", 2)(1).toDouble).getOrElse(1.0)
        } catch {
          case e: NumberFormatException => throw new QualityFormatException("Quality was an unparseable value", e)
        }
      }
    }

    getFun(headerName) match {
      case Nil => Nil
      case nonEmptyList =>
        nonEmptyList.map(HeaderValue) // parse the header value string
          .groupBy(_.quality) // group by quality
          .maxBy(_._1) // find the highest quality group
          ._2 // get the list of highest quality values
          .map(_.value) // return a list of just the values
    }
  }

  override def getPreferredHeaders(headerName: String): util.List[String] = getPreferredHeader(headerName, getHeadersScala).asJava

  override def getPreferredSplittableHeaders(headerName: String): util.List[String] = getPreferredHeader(headerName, getSplittableHeaderScala).asJava

  override def replaceHeader(headerName: String, headerValue: String): Unit = {
    headerMap = headerMap + (headerName -> List(headerValue))
    removedHeaders = removedHeaders - headerName
  }

  override def replaceHeader(headerName: String, headerValue: String, quality: Double): Unit = replaceHeader(headerName, headerValue + ";q=" + quality)

  def getSplittableHeaderScala(headerName: String): List[String] = getHeadersScala(headerName).foldLeft(List.empty[String])((list, s) => list ++ s.split(","))

  override def getSplittableHeaders(headerName: String): util.List[String] = getSplittableHeaderScala(headerName).asJava

  override def getRequestURL: StringBuffer = requestUrl

  def setRequestURL(url: StringBuffer): Unit = {
    if (Option(url).isEmpty) throw new IllegalArgumentException("null is not a legal argument to setRequestURL")

    requestUrl = url
  }

  override def getRequestURI: String = requestUri

  def setRequestURI(uri: String): Unit = {
    if (Option(uri).isEmpty) throw new IllegalArgumentException("null is not a legal argument to setRequestURI")

    requestUri = uri
  }

  /**
    * @return a string representation of the query parameters for this request
    */
  override def getQueryString: String = {
    queryParameterMap match {
      case Some(parameterMap) if parameterMap.isEmpty =>
        null
      case Some(parameterMap) =>
        parameterMap map { case (key, values) =>
          values.map(value => key + "=" + value).mkString("&")
        } mkString "&"
      case None =>
        super.getQueryString
    }
  }

  /**
    * @param key a query parameter key
    * @return the first query parameter value associated with the provided key for this request, or null if no value exists
    */
  override def getParameter(key: String): String =
    Option(getParameterValues(key)).map(_.head).orNull
  /**
    * @param key a query parameter key
    * @return all query parameter values associated with the provided key for this request
    */
  override def getParameterValues(key: String): Array[String] =
    queryParameterMap.map(_.get(key).orNull).getOrElse(super.getParameterValues(key))

  /**
    * @return all query parameter names for this request
    */
  override def getParameterNames: util.Enumeration[String] =
    queryParameterMap.map(_.keysIterator.asJavaEnumeration).getOrElse(super.getParameterNames)

  /**
    * @return the query parameter map for this request
    */
  override def getParameterMap: util.Map[String, Array[String]] =
    queryParameterMap.map(_.asJava).getOrElse(super.getParameterMap)

  /** Sets the query parameter map for this request.
    *
    * The provided map parameter will have its contents copied into an immutable map.
    * As a result, modifications to the map parameter after calling this method will have no effect on the query
    * parameters of this request.
    * The iteration order of the provided map will be maintained.
    *
    * @param map a [[java.util.Map]] containing all of the query parameters for this request
    */
  def setParameterMap(map: util.Map[String, Array[String]]): Unit = {
    if (Option(map).isEmpty) throw new IllegalArgumentException("null is not a legal argument to setParameterMap")

    val mapCopy = mutable.LinkedHashMap.empty[String, Array[String]]
    map.entrySet().asScala foreach { entry =>
      val arrayCopy = util.Arrays.copyOf(entry.getValue, entry.getValue.length)
      mapCopy += (entry.getKey -> arrayCopy)
    }
    queryParameterMap = Option(ListMap(mapCopy.toSeq: _*))
  }
}
