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
package org.openrepose.powerfilter.intrafilterLogging

import java.io.ByteArrayInputStream

import org.junit.runner.RunWith
import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse
import org.openrepose.core.systemmodel.Filter
import org.openrepose.powerfilter.filtercontext.FilterContext
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class ResponseLogTest extends FunSpec with Matchers with MockitoSugar with BeforeAndAfter {

  import org.mockito.Mockito.when

  var mutableHttpServletResponse: MutableHttpServletResponse = _
  var filterContext: FilterContext = _
  val dummyInputStream = new ByteArrayInputStream(" ".getBytes)

  before {
    mutableHttpServletResponse = mock[MutableHttpServletResponse]
    filterContext = mock[FilterContext]

    // the code under test makes some static method calls, so we gotta do this mess
    when(mutableHttpServletResponse.getBufferedOutputAsInputStream).thenReturn(dummyInputStream)
  }

  describe("a response log") {
    describe("filter description") {

      it("should include the filter ID when it is present") {
        // given a filter ID and name
        val filterId = "filter1"
        val filterName = "test-filter"

        val filter = new Filter()
        filter.setId(filterId)
        filter.setName(filterName)

        when(filterContext.getFilterConfig).thenReturn(filter)

        // when we create a new ResponseLog
        val responseLog = new ResponseLog(mutableHttpServletResponse, filterContext)

        // then the filter description includes both the ID and name
        s"$filterId-$filterName" shouldEqual responseLog.currentFilter
      }

      it("should not include the filter ID when it is null") {
        // given a null filter ID
        val filterId = null
        val filterName = "test-filter"

        val filter = new Filter()
        filter.setId(filterId)
        filter.setName(filterName)

        when(filterContext.getFilterConfig).thenReturn(filter)

        // when we create a new ResponseLog
        val responseLog = new ResponseLog(mutableHttpServletResponse, filterContext)

        // then the filter description includes just the filter name
        filterName shouldEqual responseLog.currentFilter
      }

      it("should not include the filter ID when it is an empty string") {
        // given an empty string for the filter ID
        val filterId = ""
        val filterName = "test-filter"

        val filter = new Filter()
        filter.setId(filterId)
        filter.setName(filterName)

        when(filterContext.getFilterConfig).thenReturn(filter)

        // when we create a new ResponseLog
        val responseLog = new ResponseLog(mutableHttpServletResponse, filterContext)

        // then the filter description includes just the filter name
        filterName shouldEqual responseLog.currentFilter
      }
    }
  }
}
