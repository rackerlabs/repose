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

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openrepose.commons.utils.servlet.http.HandleRequestResult;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.openrepose.commons.utils.servlet.http.HttpServletResponseWrapper;
import org.openrepose.commons.utils.servlet.http.ResponseMode;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.filters.translation.config.*;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.xml.sax.SAXException;

import java.io.IOException;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.xmlunit.matchers.CompareMatcher.isSimilarTo;

public class TranslationFilterMethodsTest {
    private static byte[] contentRemoveMe;

    private TranslationFilter filter;
    private MockHttpServletRequest mockRequest;
    private MockHttpServletResponse mockResponse;
    private HttpServletRequestWrapper httpServletRequestWrapper;
    private HttpServletResponseWrapper httpServletResponseWrapper;

    @BeforeClass
    public static void setupSpec() throws Exception {
        contentRemoveMe = IOUtils.toByteArray(TranslationFilterMethodsTest.class.getResourceAsStream("/remove-me-element.xml"));
    }

    @Before
    public void setup() throws Exception {
        ConfigurationService configurationService = mock(ConfigurationService.class);
        String configurationRoot = "";
        filter = new TranslationFilter(configurationService, configurationRoot);

        MockFilterConfig mockFilterConfig = new MockFilterConfig("TranslationFilter");
        filter.init(mockFilterConfig);

        TranslationConfig config = new TranslationConfig();

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

        RequestTranslations requestTranslations = new RequestTranslations();
        RequestTranslation trans1 = new RequestTranslation();
        trans1.setAccept(APPLICATION_XML_VALUE);
        trans1.setContentType(APPLICATION_XML_VALUE);
        trans1.setTranslatedContentType(APPLICATION_XML_VALUE);
        trans1.setStyleSheets(sheets);
        trans1.getHttpMethods().add(HttpMethod.POST);
        requestTranslations.getRequestTranslation().add(trans1);

        ResponseTranslations responseTranslations = new ResponseTranslations();
        responseTranslations.getResponseTranslation().add(new ResponseTranslation());

        config.setRequestTranslations(requestTranslations);
        config.setResponseTranslations(responseTranslations);
        filter.configurationUpdated(config);

        mockRequest = new MockHttpServletRequest(HttpMethod.PUT.value(), "/129.0.0.1/servers/");
        mockRequest.addHeader(ACCEPT, APPLICATION_XML_VALUE);
        mockRequest.setContentType(APPLICATION_XML_VALUE);

        mockResponse = new MockHttpServletResponse();
        mockResponse.setContentType(APPLICATION_XML_VALUE);
        mockResponse.setStatus(200);
    }

    @Test
    public void shouldNotTranslateRequestBodyForNonMatchingMethod() throws IOException, SAXException {
        mockRequest.setContent(contentRemoveMe);
        httpServletRequestWrapper = new HttpServletRequestWrapper(mockRequest);
        httpServletResponseWrapper = new HttpServletResponseWrapper(mockResponse, ResponseMode.MUTABLE, ResponseMode.MUTABLE);

        HandleRequestResult handleRequestResult = filter.handleRequest(httpServletRequestWrapper, httpServletResponseWrapper);
        String actual = IOUtils.toString(handleRequestResult.getRequest().getInputStream());
        String expected = new String(contentRemoveMe);

        assertThat(actual, isSimilarTo(expected));
    }
}
