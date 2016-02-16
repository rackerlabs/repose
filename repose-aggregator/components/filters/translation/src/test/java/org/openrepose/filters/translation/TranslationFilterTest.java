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
package org.openrepose.filters.translation;

import com.mockrunner.mock.web.MockFilterConfig;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.Diff;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.commons.utils.io.BufferedServletInputStream;
import org.openrepose.commons.utils.io.RawInputStreamReader;
import org.openrepose.commons.utils.servlet.http.HandleRequestResult;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.openrepose.commons.utils.servlet.http.HttpServletResponseWrapper;
import org.openrepose.commons.utils.servlet.http.ResponseMode;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.filters.translation.config.*;
import org.xml.sax.SAXException;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;

import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.openrepose.core.filter.logic.FilterDirector.SC_UNSUPPORTED_RESPONSE_CODE;

@RunWith(Enclosed.class)
public class TranslationFilterTest {
    public static class WhenHandlingResponses {
        private TranslationFilter filter;
        private String xml = "application/xml";
        private HttpServletRequest mockedRequest;
        private HttpServletResponse mockedResponse;
        private HttpServletRequestWrapper httpServletRequestWrapper;
        private HttpServletResponseWrapper httpServletResponseWrapper;
        private ConfigurationService configurationService;
        private String configurationRoot;

        @Before
        public void setup() throws Exception {
            configurationService = mock(ConfigurationService.class);
            configurationRoot = "";
            filter = new TranslationFilter(configurationService, configurationRoot);

            MockFilterConfig mockFilterConfig = new MockFilterConfig();
            mockFilterConfig.setFilterName("TranslationFilter");
            filter.init(mockFilterConfig);

            TranslationConfig config = new TranslationConfig();

            RequestTranslations requestTranslations = new RequestTranslations();
            requestTranslations.getRequestTranslation().add(new RequestTranslation());

            ResponseTranslations responseTranslations = new ResponseTranslations();
            ResponseTranslation trans2 = new ResponseTranslation();
            StyleSheets sheets = new StyleSheets();
            StyleSheet sheet = new StyleSheet();
            sheet.setId("sheet1");
            sheet.setHref("classpath:///identity.xsl");
            sheets.getStyle().add(sheet);

            sheet = new StyleSheet();
            sheet.setId("sheet2");
            sheet.setHref("classpath:///add-element.xsl");
            sheets.getStyle().add(sheet);

            sheet = new StyleSheet();
            sheet.setId("sheet2");
            sheet.setHref("classpath:///remove-element.xsl");
            sheets.getStyle().add(sheet);

            trans2.setAccept(xml);
            trans2.setContentType(xml);
            trans2.setCodeRegex("[\\d]{3}");
            trans2.setTranslatedContentType(xml);
            trans2.setStyleSheets(sheets);

            responseTranslations.getResponseTranslation().add(trans2);

            config.setRequestTranslations(requestTranslations);
            config.setResponseTranslations(responseTranslations);
            filter.configurationUpdated(config);

            mockedRequest = mock(HttpServletRequest.class);
            when(mockedRequest.getRequestURI()).thenReturn("/129.0.0.1/servers/");
            when(mockedRequest.getMethod()).thenReturn("POST");
            when(mockedRequest.getHeader(argThat(equalToIgnoringCase("Accept")))).thenReturn("application/xml");
            when(mockedRequest.getHeaders(argThat(equalToIgnoringCase("accept")))).thenReturn((Enumeration) new StringTokenizer("application/xml"));
            when(mockedRequest.getHeaderNames()).thenReturn((Enumeration) new StringTokenizer("Accept"));

            List<String> headerNames = new ArrayList<>();
            headerNames.add("content-type");

            mockedResponse = mock(HttpServletResponse.class);
            when(mockedResponse.getHeaderNames()).thenReturn(headerNames);
            when(mockedResponse.getHeader(argThat(equalToIgnoringCase("content-type")))).thenReturn("application/xml");
            when(mockedResponse.getStatus()).thenReturn(200);

        }

        @Test
        public void shouldTranslateEmptyResponseBody() throws IOException, SAXException {
            InputStream response = this.getClass().getResourceAsStream("/empty.xml");
            when(mockedResponse.getContentType()).thenReturn("application/xml");
            when(mockedResponse.getHeader(argThat(equalToIgnoringCase("Content-Type")))).thenReturn("application/xml");

            httpServletRequestWrapper = new HttpServletRequestWrapper(mockedRequest);
            httpServletResponseWrapper = new HttpServletResponseWrapper(mockedResponse, ResponseMode.MUTABLE, ResponseMode.MUTABLE);
            httpServletResponseWrapper.setHeader("Content-Type", "application/xml");
            httpServletResponseWrapper.setOutput(response);

            filter.handleResponse(httpServletRequestWrapper, httpServletResponseWrapper);
            String actual = IOUtils.toString(httpServletResponseWrapper.getOutputStreamAsInputStream());
            final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><add-me xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><root/></add-me>";

            Diff diff1 = new Diff(expected, actual);

            assertTrue(diff1.similar());
        }

        @Test
        public void shouldTranslateNonEmptyResponseBody() throws IOException, SAXException {
            InputStream response = this.getClass().getResourceAsStream("/remove-me-element.xml");
            when(mockedResponse.getContentType()).thenReturn("application/xml");
            when(mockedResponse.getHeader(argThat(equalToIgnoringCase("Content-Type")))).thenReturn("application/xml");
            httpServletRequestWrapper = new HttpServletRequestWrapper(mockedRequest);
            httpServletResponseWrapper = new HttpServletResponseWrapper(mockedResponse, ResponseMode.MUTABLE, ResponseMode.MUTABLE);
            httpServletResponseWrapper.setHeader("Content-Type", "application/xml");
            httpServletResponseWrapper.setOutput(response);

            filter.handleResponse(httpServletRequestWrapper, httpServletResponseWrapper);
            String actual = IOUtils.toString(httpServletResponseWrapper.getOutputStreamAsInputStream());
            final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><add-me xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><root>\n    This is  a test.\n</root></add-me>";

            Diff diff1 = new Diff(expected, actual);

            assertTrue(diff1.similar());
        }

        @Test
        public void shouldTranslateNullResponseBody() throws IOException, SAXException {
            when(mockedResponse.getContentType()).thenReturn("application/xml");
            when(mockedResponse.getHeader(argThat(equalToIgnoringCase("Content-Type")))).thenReturn("application/xml");
            httpServletRequestWrapper = new HttpServletRequestWrapper(mockedRequest);
            httpServletResponseWrapper = new HttpServletResponseWrapper(mockedResponse, ResponseMode.MUTABLE, ResponseMode.MUTABLE);
            httpServletResponseWrapper.setHeader("Content-Type", "application/xml");
            httpServletResponseWrapper.setOutput(null);

            filter.handleResponse(httpServletRequestWrapper, httpServletResponseWrapper);
            String actual = IOUtils.toString(httpServletResponseWrapper.getOutputStreamAsInputStream());

            assertTrue(actual.isEmpty());
        }

        @Test
        public void shouldNotTranslateResponseBodyForUnconfiguredAccept() throws IOException, SAXException {
            InputStream response = this.getClass().getResourceAsStream("/remove-me-element.xml");
            when(mockedRequest.getHeader(argThat(equalToIgnoringCase("Accept")))).thenReturn("application/json");
            when(mockedRequest.getHeaders(argThat(equalToIgnoringCase("accept")))).thenReturn((Enumeration) new StringTokenizer("application/json"));
            when(mockedRequest.getContentType()).thenReturn("application/xml");
            when(mockedResponse.getContentType()).thenReturn("application/xml");
            when(mockedResponse.getHeader(argThat(equalToIgnoringCase("Content-Type")))).thenReturn("application/xml");
            httpServletRequestWrapper = new HttpServletRequestWrapper(mockedRequest);
            httpServletResponseWrapper = new HttpServletResponseWrapper(mockedResponse, ResponseMode.MUTABLE, ResponseMode.MUTABLE);
            httpServletResponseWrapper.setHeader("Content-Type", "application/xml");
            httpServletResponseWrapper.setOutput(response);

            filter.handleResponse(httpServletRequestWrapper, httpServletResponseWrapper);
            String actual = IOUtils.toString(httpServletResponseWrapper.getOutputStreamAsInputStream());
            String expected = new String(RawInputStreamReader.instance().readFully(getClass().getResourceAsStream("/remove-me-element.xml")));

            Diff diff1 = new Diff(expected, actual);

            assertTrue(diff1.similar());
        }

        @Test
        public void shouldNotModifyResponseStatusIf() throws Exception {
            when(mockedRequest.getHeaderNames()).thenReturn(new Enumeration<String>() {
                @Override
                public boolean hasMoreElements() {
                    return false;
                }

                @Override
                public String nextElement() {
                    return null;
                }
            });
            when(mockedResponse.getStatus()).thenReturn(SC_UNSUPPORTED_RESPONSE_CODE);
            httpServletRequestWrapper = new HttpServletRequestWrapper(mockedRequest);
            httpServletResponseWrapper = new HttpServletResponseWrapper(mockedResponse, ResponseMode.MUTABLE, ResponseMode.MUTABLE);

            filter.handleResponse(httpServletRequestWrapper, httpServletResponseWrapper);
            assertEquals("Must return the received response status code", SC_UNSUPPORTED_RESPONSE_CODE, httpServletResponseWrapper.getStatus());
        }
    }

    public static class WhenHandlingRequests {
        private TranslationFilter filter;
        private String xml = "application/xml";
        private HttpServletRequest mockedRequest;
        private HttpServletResponse mockedResponse;
        private HttpServletRequestWrapper httpServletRequestWrapper;
        private HttpServletResponseWrapper httpServletResponseWrapper;
        private ConfigurationService configurationService;
        private String configurationRoot;

        @Before
        public void setup() throws Exception {
            configurationService = mock(ConfigurationService.class);
            configurationRoot = "";
            filter = new TranslationFilter(configurationService, configurationRoot);

            MockFilterConfig mockFilterConfig = new MockFilterConfig();
            mockFilterConfig.setFilterName("TranslationFilter");
            filter.init(mockFilterConfig);

            TranslationConfig config = new TranslationConfig();

            RequestTranslations requestTranslations = new RequestTranslations();
            RequestTranslation trans1 = new RequestTranslation();
            requestTranslations.getRequestTranslation().add(trans1);

            StyleSheets sheets = new StyleSheets();
            StyleSheet sheet = new StyleSheet();
            sheet.setId("sheet1");
            sheet.setHref("classpath:///identity.xsl");
            sheets.getStyle().add(sheet);

            sheet = new StyleSheet();
            sheet.setId("sheet2");
            sheet.setHref("classpath:///add-element.xsl");
            sheets.getStyle().add(sheet);

            sheet = new StyleSheet();
            sheet.setId("sheet2");
            sheet.setHref("classpath:///remove-element.xsl");
            sheets.getStyle().add(sheet);

            trans1.setAccept(xml);
            trans1.setContentType(xml);
            trans1.setTranslatedContentType(xml);
            trans1.setStyleSheets(sheets);
            trans1.getHttpMethods().add(HttpMethod.ALL);

            ResponseTranslations responseTranslations = new ResponseTranslations();
            responseTranslations.getResponseTranslation().add(new ResponseTranslation());

            config.setRequestTranslations(requestTranslations);
            config.setResponseTranslations(responseTranslations);
            filter.configurationUpdated(config);

            mockedRequest = mock(HttpServletRequest.class);
            when(mockedRequest.getRequestURI()).thenReturn("/129.0.0.1/servers/");
            when(mockedRequest.getMethod()).thenReturn("POST");
            when(mockedRequest.getHeader(argThat(equalToIgnoringCase("Accept")))).thenReturn("application/xml");
            when(mockedRequest.getHeaders(argThat(equalToIgnoringCase("accept")))).thenReturn((Enumeration) new StringTokenizer("application/xml"));
            when(mockedRequest.getHeaders(argThat(equalToIgnoringCase("content-type")))).thenReturn((Enumeration) new StringTokenizer("application/xml"));
            when(mockedRequest.getHeaderNames()).thenReturn((Enumeration) new StringTokenizer("Accept,content-type", ",", false));

            List<String> headerNames = new ArrayList<>();
            headerNames.add("content-type");

            mockedResponse = mock(HttpServletResponse.class);
            when(mockedResponse.getHeaderNames()).thenReturn(headerNames);
            when(mockedResponse.getHeader(argThat(equalToIgnoringCase("content-type")))).thenReturn("application/xml");
            when(mockedResponse.getStatus()).thenReturn(200);

        }

        @Test
        public void shouldTranslateEmptyRequestBody() throws IOException, SAXException {
            ServletInputStream response = new BufferedServletInputStream(this.getClass().getResourceAsStream("/empty.xml"));
            when(mockedRequest.getInputStream()).thenReturn(response);
            when(mockedRequest.getContentType()).thenReturn("application/xml");
            when(mockedResponse.getHeader(argThat(equalToIgnoringCase("Content-Type")))).thenReturn("application/xml");

            httpServletRequestWrapper = new HttpServletRequestWrapper(mockedRequest);
            httpServletResponseWrapper = new HttpServletResponseWrapper(mockedResponse, ResponseMode.MUTABLE, ResponseMode.MUTABLE);
            httpServletResponseWrapper.setHeader("Content-Type", "application/xml");

            HandleRequestResult handleRequestResult = filter.handleRequest(httpServletRequestWrapper, httpServletResponseWrapper);
            String actual = IOUtils.toString(handleRequestResult.getRequest().getInputStream());
            final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><add-me><root/></add-me>";

            Diff diff1 = new Diff(expected, actual);

            assertTrue(diff1.similar());
        }

        @Test
        public void shouldTranslateNonEmptyRequestBody() throws IOException, SAXException {
            ServletInputStream response = new BufferedServletInputStream(this.getClass().getResourceAsStream("/remove-me-element.xml"));
            when(mockedRequest.getInputStream()).thenReturn(response);
            httpServletRequestWrapper = new HttpServletRequestWrapper(mockedRequest);
            httpServletResponseWrapper = new HttpServletResponseWrapper(mockedResponse, ResponseMode.MUTABLE, ResponseMode.MUTABLE);
            when(mockedRequest.getContentType()).thenReturn("application/xml");
            when(mockedResponse.getHeader(argThat(equalToIgnoringCase("Content-Type")))).thenReturn("application/xml");

            HandleRequestResult handleRequestResult = filter.handleRequest(httpServletRequestWrapper, httpServletResponseWrapper);
            String actual = IOUtils.toString(handleRequestResult.getRequest().getInputStream());
            final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><add-me xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><root>\n    This is  a test.\n</root></add-me>";

            Diff diff1 = new Diff(expected, actual);

            assertTrue(diff1.similar());
        }

        @Test
        public void shouldNotTranslateRequestBodyForUnconfiguredAccept() throws IOException, SAXException {
            ServletInputStream response = new BufferedServletInputStream(this.getClass().getResourceAsStream("/remove-me-element.xml"));
            when(mockedRequest.getInputStream()).thenReturn(response);
            when(mockedRequest.getHeader(argThat(equalToIgnoringCase("Accept")))).thenReturn("application/other");
            when(mockedRequest.getHeaders(argThat(equalToIgnoringCase("accept")))).thenReturn((Enumeration) new StringTokenizer("application/other"));
            when(mockedRequest.getContentType()).thenReturn("application/other");
            when(mockedResponse.getHeader(argThat(equalToIgnoringCase("Content-Type")))).thenReturn("application/other");
            httpServletRequestWrapper = new HttpServletRequestWrapper(mockedRequest);
            httpServletResponseWrapper = new HttpServletResponseWrapper(mockedResponse, ResponseMode.MUTABLE, ResponseMode.MUTABLE);
            httpServletResponseWrapper.setHeader("Content-Type", "application/xml");

            HandleRequestResult handleRequestResult = filter.handleRequest(httpServletRequestWrapper, httpServletResponseWrapper);
            String actual = IOUtils.toString(handleRequestResult.getRequest().getInputStream());
            String expected = new String(RawInputStreamReader.instance().readFully(getClass().getResourceAsStream("/remove-me-element.xml")));

            Diff diff1 = new Diff(expected, actual);

            assertTrue(diff1.similar());
        }
    }

    public static class WhenBuildingHandlers {
        private TranslationFilter filter;
        private String xml = "application/xml";
        private ConfigurationService configurationService;
        private String configurationRoot;

        @Before
        public void setup() throws Exception {
            configurationService = mock(ConfigurationService.class);
            configurationRoot = "";
            filter = new TranslationFilter(configurationService, configurationRoot);

            MockFilterConfig mockFilterConfig = new MockFilterConfig();
            mockFilterConfig.setFilterName("TranslationFilter");
            filter.init(mockFilterConfig);
        }

        @Test
        public void shouldCreateProcessorPoolsOnConfigUpdate() throws Exception {
            TranslationConfig config = new TranslationConfig();
            RequestTranslations requestTranslations = new RequestTranslations();
            ResponseTranslations responseTranslations = new ResponseTranslations();

            RequestTranslation trans1 = new RequestTranslation();
            StyleSheets sheets = new StyleSheets();
            StyleSheet sheet = new StyleSheet();
            sheet.setId("sheet1");
            sheet.setHref("classpath:///style.xsl");
            sheets.getStyle().add(sheet);
            trans1.setAccept(xml);
            trans1.setContentType(xml);
            trans1.setTranslatedContentType(xml);
            trans1.setStyleSheets(sheets);

            requestTranslations.getRequestTranslation().add(trans1);

            ResponseTranslation trans2 = new ResponseTranslation();
            trans2.setAccept(xml);
            trans2.setContentType(xml);
            trans2.setCodeRegex("4[\\d]{2}");
            trans2.setTranslatedContentType(xml);
            trans2.setStyleSheets(sheets);

            responseTranslations.getResponseTranslation().add(trans2);

            config.setRequestTranslations(requestTranslations);
            config.setResponseTranslations(responseTranslations);
            filter.configurationUpdated(config);
//            assertEquals(1, filter.getRequestProcessors().size());
//            assertEquals(1, filter.getResponseProcessors().size());
        }
    }
}
