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

import java.util
import javax.servlet.http.HttpServletRequest

import scala.collection.JavaConverters._

/**
 * Created with IntelliJ IDEA.
 * User: adrian
 * Date: 5/27/15
 * Time: 10:25 AM
 */
class HttpServletRequestWrapper(originalRequest: HttpServletRequest)
  extends javax.servlet.http.HttpServletRequestWrapper(originalRequest)
  with HeaderInteractor {


  override def getHeaderNames: util.Enumeration[String] = super.getHeaderNames

  override def getIntHeader(s: String): Int = super.getIntHeader(s)

  override def getHeaders(s: String): util.Enumeration[String] = super.getHeaders(s)

  override def getDateHeader(s: String): Long = super.getDateHeader(s)

  override def getHeader(s: String): String = super.getHeader(s)

  override def getHeaderNamesList: util.List[String] = ???

  override def getHeaderList(headerName: String): util.List[String] = {
    originalRequest.getHeaders(headerName).asScala.toList.asJava
  }

  override def addHeader(headerName: String, headerValue: String): Unit = ???

  override def addHeader(headerName: String, headerValue: String, quality: Double): Unit = ???

  override def getPreffereedSplittableHeader(headerName: String): String = ???

  override def appendHeader(headerName: String, headerValue: String): Unit = ???

  override def appendHeader(headerName: String, headerValue: String, quality: Double): Unit = ???

  override def removeHeader(headerName: String): Unit = ???

  override def getPrefferedHeader(headerName: String): String = ???

  override def replaceHeader(headerName: String, headerValue: String): Unit = ???

  override def replaceHeader(headerName: String, headerValue: String, quality: Double): Unit = ???

  override def getSplittableHeader(headerName: String): util.List[String] = ???
}
