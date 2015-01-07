package org.openrepose.filters.irivalidator

import javax.servlet.FilterChain
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.mockito.Mockito._
import org.scalatest.FunSpec
import org.scalatest.mock.MockitoSugar

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
      when(mockRequest.getRequestURL).thenReturn(new StringBuffer("http://www.example.com/%%a"))

      iriValidatorFilter.doFilter(mockRequest, mockResponse, mockFilterChain)

      verify(mockResponse).sendError(HttpServletResponse.SC_BAD_REQUEST)
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
      when(mockRequest.getRequestURL).thenReturn(new StringBuffer("http://www.example.com/test?%%a=b"))

      iriValidatorFilter.doFilter(mockRequest, mockResponse, mockFilterChain)

      verify(mockResponse).sendError(HttpServletResponse.SC_BAD_REQUEST)
    }
  }
}
