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
package org.openrepose.external.pjlcompression;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public final class CompressingFilterTest {
    private static final String ACCEPT_ENCODING = "Accept-Encoding";
    private static final String CONTENT_ENCODING = "Content-Encoding";

    private CompressingFilter compressingFilter;

    @Before
    public void setup() throws Exception {
        MockFilterConfig filterConfig = new MockFilterConfig();

        compressingFilter = new CompressingFilter();
        compressingFilter.init(filterConfig);
        compressingFilter.setForRepose();
    }

    @Test
    public void doFilter_acceptEncodingShouldBeRemovedFromRequest() throws Exception {
        // Given
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();
        FilterChain mockFilterChain = mock(FilterChain.class);
        ArgumentCaptor<ServletRequest> capturedRequest = ArgumentCaptor.forClass(ServletRequest.class);

        mockRequest.addHeader(CONTENT_ENCODING, "identity");
        mockRequest.addHeader(ACCEPT_ENCODING, "identity");

        // When
        compressingFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Then
        verify(mockFilterChain).doFilter(capturedRequest.capture(), any(ServletResponse.class));
        assertThat(((HttpServletRequest) capturedRequest.getValue()).getHeader(ACCEPT_ENCODING), nullValue());
    }
}
