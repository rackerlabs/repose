package com.rackspace.papi.components.translation;

import com.rackspace.papi.commons.util.io.BufferedServletInputStream;
import com.rackspace.papi.commons.util.io.RawInputStreamReader;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.components.translation.config.*;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.service.config.ConfigurationService;
import org.custommonkey.xmlunit.Diff;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class TranslationHandlerTest {
    public static class WhenHandlingResponses {
        private TranslationHandlerFactory factory;
        private String xml = "application/xml";
        private TranslationHandler handler;
        private HttpServletRequest mockedRequest;
        private HttpServletResponse mockedResponse;
        private MutableHttpServletRequest mutableHttpRequest;
        private MutableHttpServletResponse mutableHttpResponse;
        private ConfigurationService manager;

        @Before
        public void setup() {
            manager = mock(ConfigurationService.class);
            factory = new TranslationHandlerFactory(manager, "", "");
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
            factory.configurationUpdated(config);
            handler = factory.buildHandler();

            mockedRequest = mock(HttpServletRequest.class);
            when(mockedRequest.getRequestURI()).thenReturn("/129.0.0.1/servers/");
            when(mockedRequest.getMethod()).thenReturn("POST");
            when(mockedRequest.getHeader(eq("Accept"))).thenReturn("application/xml");
            when(mockedRequest.getHeaders(eq("accept"))).thenReturn((Enumeration) new StringTokenizer("application/xml"));
            when(mockedRequest.getHeaderNames()).thenReturn((Enumeration) new StringTokenizer("Accept"));

            List<String> headerNames = new ArrayList<String>();
            headerNames.add("content-type");
            
            mockedResponse = mock(HttpServletResponse.class);
            when(mockedResponse.getHeaderNames()).thenReturn(headerNames);
            when(mockedResponse.getHeader(eq("content-type"))).thenReturn("application/xml");
            when(mockedResponse.getStatus()).thenReturn(200);

        }

        @Test
        public void shouldTranslateEmptyResponseBody() throws IOException, SAXException {

            InputStream response = this.getClass().getResourceAsStream("/empty.xml");
            when(mockedRequest.getAttribute(eq("repose.response.input.stream"))).thenReturn(response);
            when(mockedResponse.getContentType()).thenReturn("application/xml");
            when(mockedResponse.getHeader(eq("Content-Type"))).thenReturn("application/xml");

            mutableHttpRequest = MutableHttpServletRequest.wrap((HttpServletRequest) mockedRequest);
            mutableHttpResponse = MutableHttpServletResponse.wrap(mockedRequest, mockedResponse);
            mutableHttpResponse.setHeader("Content-Type", "application/xml");

            FilterDirector director = handler.handleResponse(mutableHttpRequest, mutableHttpResponse);
            String actual = director.getResponseMessageBody();
            final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><add-me xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><root/></add-me>";

            Diff diff1 = new Diff(expected, actual);

            assertEquals(director.getFilterAction(), FilterAction.PASS);
            assertTrue(diff1.similar());
        }        

        @Test
        public void shouldTranslateNonEmptyResponseBody() throws IOException, SAXException {

            InputStream response = this.getClass().getResourceAsStream("/remove-me-element.xml");
            when(mockedRequest.getAttribute(eq("repose.response.input.stream"))).thenReturn(response);
            when(mockedResponse.getContentType()).thenReturn("application/xml");
            when(mockedResponse.getHeader(eq("Content-Type"))).thenReturn("application/xml");
            mutableHttpRequest = MutableHttpServletRequest.wrap((HttpServletRequest) mockedRequest);
            mutableHttpResponse = MutableHttpServletResponse.wrap(mockedRequest, mockedResponse);
            mutableHttpResponse.setHeader("Content-Type", "application/xml");

            FilterDirector director = handler.handleResponse(mutableHttpRequest, mutableHttpResponse);
            String actual = director.getResponseMessageBody();
            final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><add-me xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><root>\n    This is  a test.\n</root></add-me>";
            
            Diff diff1 = new Diff(expected, actual);

            assertEquals(director.getFilterAction(), FilterAction.PASS);
            assertTrue(diff1.similar());
        }        

        @Test
        public void shouldTranslateNullResponseBody() throws IOException, SAXException {

            when(mockedRequest.getAttribute(eq("repose.response.input.stream"))).thenReturn(null);
            when(mockedResponse.getContentType()).thenReturn("application/xml");
            when(mockedResponse.getHeader(eq("Content-Type"))).thenReturn("application/xml");
            mutableHttpRequest = MutableHttpServletRequest.wrap((HttpServletRequest) mockedRequest);
            mutableHttpResponse = MutableHttpServletResponse.wrap(mockedRequest, mockedResponse);
            mutableHttpResponse.setHeader("Content-Type", "application/xml");

            FilterDirector director = handler.handleResponse(mutableHttpRequest, mutableHttpResponse);
            String actual = director.getResponseMessageBody();

            assertEquals(director.getFilterAction(), FilterAction.PASS);
            assertTrue(actual.isEmpty());
        }        

        @Test
        public void shouldNotTranslateResponseBodyForUnconfiguredAccept() throws IOException, SAXException {

            InputStream response = this.getClass().getResourceAsStream("/remove-me-element.xml");
            when(mockedRequest.getAttribute(eq("repose.response.input.stream"))).thenReturn(response);
            when(mockedRequest.getHeader(eq("Accept"))).thenReturn("application/json");
            when(mockedRequest.getHeaders(eq("accept"))).thenReturn((Enumeration) new StringTokenizer("application/json"));
            when(mockedRequest.getContentType()).thenReturn("application/xml");
            when(mockedResponse.getContentType()).thenReturn("application/xml");
            when(mockedResponse.getHeader(eq("Content-Type"))).thenReturn("application/xml");
            mutableHttpRequest = MutableHttpServletRequest.wrap((HttpServletRequest) mockedRequest);
            mutableHttpResponse = MutableHttpServletResponse.wrap(mockedRequest, mockedResponse);
            mutableHttpResponse.setHeader("Content-Type", "application/xml");

            FilterDirector director = handler.handleResponse(mutableHttpRequest, mutableHttpResponse);
            String actual = new String(RawInputStreamReader.instance().readFully(mutableHttpResponse.getInputStream()));
            //String actual = director.getResponseMessageBody();
            String expected = new String(RawInputStreamReader.instance().readFully(getClass().getResourceAsStream("/remove-me-element.xml")));
            
            Diff diff1 = new Diff(expected, actual);

            assertEquals(director.getFilterAction(), FilterAction.PASS);
            assertTrue(diff1.similar());
        }        
    }

    public static class WhenHandlingRequests {
        private TranslationHandlerFactory factory;
        private String xml = "application/xml";
        private TranslationHandler handler;
        private HttpServletRequest mockedRequest;
        private HttpServletResponse mockedResponse;
        private MutableHttpServletRequest mutableHttpRequest;
        private MutableHttpServletResponse mutableHttpResponse;
        private ConfigurationService manager;

        @Before
        public void setup() {
            manager = mock(ConfigurationService.class);
            factory = new TranslationHandlerFactory(manager, "", "");
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
            factory.configurationUpdated(config);
            handler = factory.buildHandler();

            mockedRequest = mock(HttpServletRequest.class);
            when(mockedRequest.getRequestURI()).thenReturn("/129.0.0.1/servers/");
            when(mockedRequest.getMethod()).thenReturn("POST");
            when(mockedRequest.getHeader(eq("Accept"))).thenReturn("application/xml");
            when(mockedRequest.getHeaders(eq("accept"))).thenReturn((Enumeration) new StringTokenizer("application/xml"));
            when(mockedRequest.getHeaders(eq("content-type"))).thenReturn((Enumeration) new StringTokenizer("application/xml"));
            when(mockedRequest.getHeaderNames()).thenReturn((Enumeration) new StringTokenizer("Accept,content-type", ",", false));

            List<String> headerNames = new ArrayList<String>();
            headerNames.add("content-type");
            
            mockedResponse = mock(HttpServletResponse.class);
            when(mockedResponse.getHeaderNames()).thenReturn(headerNames);
            when(mockedResponse.getHeader(eq("content-type"))).thenReturn("application/xml");
            when(mockedResponse.getStatus()).thenReturn(200);

        }

        @Test
        public void shouldTranslateEmptyRequestBody() throws IOException, SAXException {

            ServletInputStream response = new BufferedServletInputStream(this.getClass().getResourceAsStream("/empty.xml"));
            when(mockedRequest.getInputStream()).thenReturn(response);
            when(mockedRequest.getContentType()).thenReturn("application/xml");
            when(mockedResponse.getHeader(eq("Content-Type"))).thenReturn("application/xml");

            mutableHttpRequest = MutableHttpServletRequest.wrap((HttpServletRequest) mockedRequest);
            mutableHttpResponse = MutableHttpServletResponse.wrap(mockedRequest, mockedResponse);
            mutableHttpResponse.setHeader("Content-Type", "application/xml");

            FilterDirector director = handler.handleRequest(mutableHttpRequest, mutableHttpResponse);
            String actual = new String(RawInputStreamReader.instance().readFully(mutableHttpRequest.getInputStream()));
            final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><add-me><root/></add-me>";

            Diff diff1 = new Diff(expected, actual);

            assertEquals(director.getFilterAction(), FilterAction.PROCESS_RESPONSE);
            assertTrue(diff1.similar());
        }        

        @Test
        public void shouldTranslateNonEmptyRequestBody() throws IOException, SAXException {

            ServletInputStream response = new BufferedServletInputStream(this.getClass().getResourceAsStream("/remove-me-element.xml"));
            when(mockedRequest.getInputStream()).thenReturn(response);
            mutableHttpRequest = MutableHttpServletRequest.wrap((HttpServletRequest) mockedRequest);
            mutableHttpResponse = MutableHttpServletResponse.wrap(mockedRequest, mockedResponse);
            when(mockedRequest.getContentType()).thenReturn("application/xml");
            when(mockedResponse.getHeader(eq("Content-Type"))).thenReturn("application/xml");

            FilterDirector director = handler.handleRequest(mutableHttpRequest, mutableHttpResponse);
            String actual = new String(RawInputStreamReader.instance().readFully(mutableHttpRequest.getInputStream()));
            final String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><add-me xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><root>\n    This is  a test.\n</root></add-me>";

            Diff diff1 = new Diff(expected, actual);

            assertEquals(director.getFilterAction(), FilterAction.PROCESS_RESPONSE);
            assertTrue(diff1.similar());
        }        

        @Test
        public void shouldNotTranslateRequestBodyForUnconfiguredAccept() throws IOException, SAXException {

            ServletInputStream response = new BufferedServletInputStream(this.getClass().getResourceAsStream("/remove-me-element.xml"));
            when(mockedRequest.getInputStream()).thenReturn(response);
            when(mockedRequest.getHeader(eq("Accept"))).thenReturn("application/other");
            when(mockedRequest.getHeaders(eq("accept"))).thenReturn((Enumeration) new StringTokenizer("application/other"));
            when(mockedRequest.getContentType()).thenReturn("application/other");
            when(mockedResponse.getHeader(eq("Content-Type"))).thenReturn("application/other");
            mutableHttpRequest = MutableHttpServletRequest.wrap((HttpServletRequest) mockedRequest);
            mutableHttpResponse = MutableHttpServletResponse.wrap(mockedRequest, mockedResponse);
            mutableHttpResponse.setHeader("Content-Type", "application/xml");

            FilterDirector director = handler.handleRequest(mutableHttpRequest, mutableHttpResponse);
            String actual = new String(RawInputStreamReader.instance().readFully(mutableHttpRequest.getInputStream()));
            //String actual = director.getResponseMessageBody();
            String expected = new String(RawInputStreamReader.instance().readFully(getClass().getResourceAsStream("/remove-me-element.xml")));
            
            Diff diff1 = new Diff(expected, actual);

            assertEquals(director.getFilterAction(), FilterAction.PROCESS_RESPONSE);
            assertTrue(diff1.similar());
        }        
    }
}
