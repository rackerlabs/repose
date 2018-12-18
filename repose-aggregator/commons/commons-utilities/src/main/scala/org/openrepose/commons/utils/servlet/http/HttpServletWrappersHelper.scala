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

import org.apache.http.HttpHeaders._
import org.openrepose.commons.utils.http.media.{MediaType, MimeType}

import scala.collection.JavaConverters._

/**
 * This class is full of helper methods to replace the old org.openrepose.commons.utils.http.media classes.
 */
object HttpServletWrappersHelper {
  def processMediaType(request: HttpServletRequestWrapper): util.List[MediaType] = {
    processMediaType(request.getPreferredSplittableHeadersWithParameters("Accept"))
  }

  def processMediaType(headerValues: util.List[String]): util.List[MediaType] = {
    headerValues.asScala.map(processMediaType).asJava
  }

  def processMediaType(headerValue: String): MediaType = {
    val mediaTypeWithParamtersStripped: String = headerValue.split(";")(0)
    var mediaType: MimeType = MimeType.getMatchingMimeType(mediaTypeWithParamtersStripped)
    if (MimeType.UNKNOWN == mediaType) {
      mediaType = MimeType.guessMediaTypeFromString(mediaTypeWithParamtersStripped)
    }
    new MediaType(mediaTypeWithParamtersStripped, mediaType)
  }

  def getAcceptValues(request: HttpServletRequestWrapper): util.List[MediaType] = {
    processMediaType(request)
  }

  def getContentType(request: HttpServletRequestWrapper): MediaType = {
    getContentType(request.getHeader(CONTENT_TYPE))
  }

  def getContentType(response: HttpServletResponseWrapper): MediaType = {
    getContentType(response.getHeader(CONTENT_TYPE))
  }

  def getContentType(contentType: String): MediaType = {
    val contentMimeType: MimeType = MimeType.guessMediaTypeFromString(if (contentType != null) contentType else "")
    new MediaType(contentMimeType)
  }
}
