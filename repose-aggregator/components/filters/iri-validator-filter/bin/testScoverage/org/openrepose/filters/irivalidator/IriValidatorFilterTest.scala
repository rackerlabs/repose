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
package org.openrepose.filters.irivalidator

import javax.servlet.FilterChain
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.junit.runner.RunWith
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.FunSpec
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class IriValidatorFilterTest extends FunSpec with MockitoSugar {

  describe("doFilter") {
    it("should forward requests where the request URL is a valid IRI") {
      val iriValidatorFilter = new IriValidatorFilter
      val mockRequest = mock[HttpServletRequest]
      val mockResponse = mock[HttpServletResponse]
      val mockFilterChain = mock[FilterChain]
      when(mockRequest.getRequestURL).thenReturn(new StringBuffer("http://www.example.com/test"))

      iriValidatorFilter.doFilter(mockRequest, mockResponse, mockFilterChain)

      verify(mockFilterChain).doFilter(mockRequest, mockResponse)
    }

    it("should reject requests where the request URL is a not valid IRI with a 400") {
      val iriValidatorFilter = new IriValidatorFilter
      val mockRequest = mock[HttpServletRequest]
      val mockResponse = mock[HttpServletResponse]
      val mockFilterChain = mock[FilterChain]
      when(mockRequest.getRequestURL).thenReturn(new StringBuffer("http://www.example.com/%aa"))

      iriValidatorFilter.doFilter(mockRequest, mockResponse, mockFilterChain)

      verifyZeroInteractions(mockFilterChain)
      verify(mockResponse).sendError(Matchers.eq(HttpServletResponse.SC_BAD_REQUEST), Matchers.anyString)
    }

    it("should forward requests where the request URL is a valid IRI with valid query parameters") {
      val iriValidatorFilter = new IriValidatorFilter
      val mockRequest = mock[HttpServletRequest]
      val mockResponse = mock[HttpServletResponse]
      val mockFilterChain = mock[FilterChain]
      when(mockRequest.getRequestURL).thenReturn(new StringBuffer("http://www.example.com/test?a=b"))

      iriValidatorFilter.doFilter(mockRequest, mockResponse, mockFilterChain)

      verify(mockFilterChain).doFilter(mockRequest, mockResponse)
    }

    it("should reject requests where the request URL is a valid IRI with invalid query parameters") {
      val iriValidatorFilter = new IriValidatorFilter
      val mockRequest = mock[HttpServletRequest]
      val mockResponse = mock[HttpServletResponse]
      val mockFilterChain = mock[FilterChain]
      when(mockRequest.getRequestURL).thenReturn(new StringBuffer("http://www.example.com/test?%aa=b"))

      iriValidatorFilter.doFilter(mockRequest, mockResponse, mockFilterChain)

      verifyZeroInteractions(mockFilterChain)
      verify(mockResponse).sendError(Matchers.eq(HttpServletResponse.SC_BAD_REQUEST), Matchers.anyString)
    }
  }
}
