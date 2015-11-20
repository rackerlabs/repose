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

package org.openrepose.filters.urlextractortoheader

import org.junit.runner.RunWith
import org.openrepose.filters.urlextractortoheader.config.{UrlExtractorToHeaderConfig, Extractor}
import org.scalatest.{Matchers, BeforeAndAfter, FunSpec}
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UrlExtractorToHeaderFilterTest extends FunSpec with BeforeAndAfter with Matchers {

  describe("doFilter") {
    it("should do nothing") {
      // everyone is a winner!
    }
  }

  describe("configuration") {
    it("can be loaded when a default value is specified") {
      val config = new UrlExtractorToHeaderConfig
      config.getExtraction.add(createConfigExtractor("X-Device-Id", ".*/(hybrid:\\d+)/entities/.+", Some("no-value")))
      val filter = new UrlExtractorToHeaderFilter(null)

      filter.configurationUpdated(config)
    }

    it("can be loaded when a default value is NOT specified") {
      val config = new UrlExtractorToHeaderConfig
      config.getExtraction.add(createConfigExtractor("X-Device-Id", ".*/(hybrid:\\d+)/entities/.+", None))
      val filter = new UrlExtractorToHeaderFilter(null)

      filter.configurationUpdated(config)
    }
  }

  def createConfigExtractor(headerName: String, urlRegex: String, defaultValue: Option[String]): Extractor = {
    val extractor = new Extractor
    extractor.setHeader(headerName)
    extractor.setUrlRegex(urlRegex)
    extractor.setDefault(defaultValue match {
      case Some(default) => default
      case None => null
    })

    extractor
  }
}
