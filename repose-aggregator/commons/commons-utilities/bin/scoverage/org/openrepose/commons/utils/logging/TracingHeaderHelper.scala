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

package org.openrepose.commons.utils.logging

import java.io.IOException
import java.util

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.typesafe.scalalogging.StrictLogging
import org.apache.commons.codec.binary.Base64
import org.slf4j.MDC

import scala.collection.JavaConverters._

object TracingHeaderHelper extends StrictLogging {

  // JSON parsing
  private val ObjectMapper = new ObjectMapper(new JsonFactory)
  private val TypeRef = new TypeReference[util.HashMap[String, String]] {}

  // JSON contents
  private val TraceGuidKey = "requestId"
  private val OriginKey = "origin"
  private val UserKey = "user"

  def getTraceGuid(tracingHeader: String): String = {
    (Option(MDC.get(TracingKey.TRACING_KEY)), Option(tracingHeader)) match {
      case (Some(tracingGuid), _) if tracingGuid.trim.nonEmpty => tracingGuid
      case (_, None) => null
      case (_, Some(header)) =>
        try {
          ObjectMapper.readValue[util.HashMap[String, String]](decode(tracingHeader), TypeRef).get(TraceGuidKey)
        } catch {
          case e: IOException =>
            logger.debug("Unable to Base64 decode/JSON parse tracing header: {}", header, e)
            header
        }
    }
  }

  def createTracingHeader(requestId: String, origin: String): String = {
    createTracingHeader(requestId, origin, None)
  }

  def createTracingHeader(requestId: String, origin: String, user: String): String = {
    createTracingHeader(requestId, origin, Option(user))
  }

  def createTracingHeader(requestId: String, origin: String, user: Option[String]): String = {
    Base64.encodeBase64String(
      ObjectMapper.writeValueAsBytes(
        (Map(TraceGuidKey -> requestId, OriginKey -> origin) ++ user.map(UserKey -> _)).asJava))
  }

  def decode(tracingHeader: String): String = {
    if (Base64.isBase64(tracingHeader)) new String(Base64.decodeBase64(tracingHeader)) else tracingHeader
  }
}
