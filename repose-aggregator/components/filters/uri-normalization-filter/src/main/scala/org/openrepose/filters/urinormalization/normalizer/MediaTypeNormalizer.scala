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
package org.openrepose.filters.urinormalization.normalizer

import java.util.regex.Pattern

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.commons.utils.http.media.MimeType
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper
import org.openrepose.filters.urinormalization.config.MediaType

import scala.language.postfixOps

class MediaTypeNormalizer(configuredMediaTypes: Seq[MediaType]) extends LazyLogging {

  private final val VARIANT_EXTRACTOR_REGEX = Pattern.compile("((\\.)[^\\d][\\w]*)")
  private final val VARIANT_EXTENSION_GROUP = 1

  private val configuredPreferredMediaType: Option[MediaType] = configuredMediaTypes.find(_.isPreferred) orElse {
    configuredMediaTypes.headOption map { mt =>
      logger.info("No preferred media type specified in the content normalization configuration. Using the first in the list.")
      mt
    }
  }

  def normalizeContentMediaType(request: HttpServletRequestWrapper): Unit = {
    configuredPreferredMediaType foreach { mediaType =>
      val acceptHeader = Option(request.getHeader(CommonHttpHeader.ACCEPT.toString))

      getMediaTypeForVariant(request) orElse {
        if (acceptHeader.isEmpty || (MimeType.getMatchingMimeType(acceptHeader.get) == MimeType.WILDCARD)) {
          Option(mediaType)
        } else {
          None
        }
      } foreach { mt =>
        request.replaceHeader(CommonHttpHeader.ACCEPT.toString, mt.getName)
      }
    }
  }

  def getMediaTypeForVariant(request: HttpServletRequestWrapper): Option[MediaType] = {
    val variantMatcher = VARIANT_EXTRACTOR_REGEX.matcher(request.getRequestURI)

    if (variantMatcher.find) {
      val requestedVariant = variantMatcher.group(VARIANT_EXTENSION_GROUP)

      configuredMediaTypes dropWhile { mediaType =>
        val variantExtension = formatVariant(mediaType.getVariantExtension)

        if (variantExtension.nonEmpty && requestedVariant.equalsIgnoreCase(variantExtension)) {
          val uriExtensionIndex = request.getRequestURI.lastIndexOf(requestedVariant)
          val urlExtensionIndex = request.getRequestURL.lastIndexOf(requestedVariant)

          if (uriExtensionIndex > 0 && urlExtensionIndex > 0) {
            val uriBuilder = new StringBuilder(request.getRequestURI)

            request.setRequestURI(uriBuilder.delete(uriExtensionIndex, uriExtensionIndex + requestedVariant.length).toString)
          }

          true
        } else {
          false
        }
      } headOption
    } else {
      None
    }
  }

  private def formatVariant(variant: String): String =
    if (variant.isEmpty || variant.startsWith(".")) variant else "." + variant
}