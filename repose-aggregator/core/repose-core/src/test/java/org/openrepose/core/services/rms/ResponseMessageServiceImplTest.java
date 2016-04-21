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
package org.openrepose.core.services.rms;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.commons.utils.InputStreamUtilities;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.io.ByteBufferInputStream;
import org.openrepose.commons.utils.io.ByteBufferServletOutputStream;
import org.openrepose.commons.utils.io.buffer.ByteBuffer;
import org.openrepose.commons.utils.io.buffer.CyclicByteBuffer;
import org.openrepose.commons.utils.servlet.http.HttpServletResponseWrapper;
import org.openrepose.commons.utils.servlet.http.ResponseMode;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.rms.config.Message;
import org.openrepose.core.services.rms.config.OverwriteType;
import org.openrepose.core.services.rms.config.ResponseMessagingConfiguration;
import org.openrepose.core.services.rms.config.StatusCodeMatcher;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.*;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.I_AM_A_TEAPOT;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class ResponseMessageServiceImplTest {

    public static class WhenHandlingResponse {
        private static final String MESSAGE = "This is the replaced message";
        private final ResponseMessageServiceImpl rmsImpl = new ResponseMessageServiceImpl(mock(ConfigurationService.class));
        private final ResponseMessagingConfiguration configurationObject = new ResponseMessagingConfiguration();
        private final Vector<String> acceptValues = new Vector<String>(1);
        private Enumeration<String> headerValueEnumeration = null;
        private HttpServletRequest mockedRequest = mock(HttpServletRequest.class);
        private HttpServletResponseWrapper mockedResponse = mock(HttpServletResponseWrapper.class);

        @Before
        public void setup() {
            acceptValues.addAll(Arrays.asList("application/json"));
            headerValueEnumeration = acceptValues.elements();
            List<String> headerNames = new ArrayList<>();
            headerNames.add("Accept");
            when(mockedRequest.getHeaderNames()).thenReturn(Collections.enumeration(headerNames));
            when(mockedRequest.getHeaders("Accept")).thenReturn(headerValueEnumeration);
            when(mockedResponse.getStatus()).thenReturn(413);

            configurationObject.getStatusCode().clear();
            configurationObject.getStatusCode().add(createMatcher(OverwriteType.IF_EMPTY));
            rmsImpl.setInitialized();
            rmsImpl.updateConfiguration(configurationObject.getStatusCode());

        }

        private StatusCodeMatcher createMatcher(OverwriteType overwriteType) {
            StatusCodeMatcher matcher = new StatusCodeMatcher();
            matcher.setId("413");
            matcher.setCodeRegex("413");
            matcher.setOverwrite(overwriteType);

            Message message = new Message();
            message.setMediaType("*/*");
            message.setValue(MESSAGE);

            matcher.getMessage().add(message);

            return matcher;
        }

        @Test
        public void shouldWriteIfEmptyAndNoBody() throws IOException {
            // Hook up response body stream to mocked response
            final ByteBuffer internalBuffer = new CyclicByteBuffer();
            final ServletOutputStream outputStream = new ByteBufferServletOutputStream(internalBuffer);
            final PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream));
            final ByteBufferInputStream inputStream = new ByteBufferInputStream(internalBuffer);
            when(mockedResponse.getOutputStream()).thenReturn(outputStream);
            when(mockedResponse.getOutputStreamAsInputStream()).thenReturn(inputStream);
            when(mockedResponse.getWriter()).thenReturn(printWriter);

            rmsImpl.handle(mockedRequest, mockedResponse);

            String result = InputStreamUtilities.streamToString(new ByteBufferInputStream(internalBuffer));
            assertTrue(StringUtilities.nullSafeEquals(MESSAGE, result));
        }

        @Test
        public void shouldPreserveIfEmptyAndBody() throws IOException {
            // Hook up response body stream to mocked response
            final ByteBuffer internalBuffer = new CyclicByteBuffer();
            internalBuffer.put("hello there".getBytes());
            final ServletOutputStream outputStream = new ByteBufferServletOutputStream(internalBuffer);
            final ByteBufferInputStream inputStream = new ByteBufferInputStream(internalBuffer);
            when(mockedResponse.getOutputStreamAsInputStream()).thenReturn(inputStream);
            when(mockedResponse.getOutputStream()).thenReturn(outputStream);

            rmsImpl.handle(mockedRequest, mockedResponse);

            String result = InputStreamUtilities.streamToString(new ByteBufferInputStream(internalBuffer));
            assertTrue(StringUtilities.nullSafeEquals("hello there", result));
        }
    }

    public static class WhenEscapingTheMessage {
        private static final String ESCAPE_THIS = "\b\n\t\f\r\\\"'/&<>";
        private static final String I_AM_A_TEAPOT_VALUE_STRING = Integer.toString(I_AM_A_TEAPOT.value());
        private static final String MEDIA_TYPE_TEXT_PLAIN = "text/plain";
        private static final String MEDIA_TYPE_APPLICATION_JSON = "application/json";
        private static final String MEDIA_TYPE_APPLICATION_XML = "application/xml";
        private ResponseMessagingConfiguration responseMessagingConfiguration = new ResponseMessagingConfiguration();
        private ResponseMessageServiceImpl responseMessageServiceImpl = new ResponseMessageServiceImpl(mock(ConfigurationService.class));
        private HttpServletRequest mockedRequest = mock(HttpServletRequest.class);
        private HttpServletResponseWrapper response = new HttpServletResponseWrapper(
                new MockHttpServletResponse(),
                ResponseMode.PASSTHROUGH,
                ResponseMode.MUTABLE);

        @Before
        public void setup() {
            StatusCodeMatcher matcher = new StatusCodeMatcher();
            matcher.setId(I_AM_A_TEAPOT_VALUE_STRING);
            matcher.setCodeRegex(I_AM_A_TEAPOT_VALUE_STRING);
            matcher.setOverwrite(OverwriteType.IF_EMPTY);
            matcher.getMessage().add(createMessage(MEDIA_TYPE_TEXT_PLAIN));
            matcher.getMessage().add(createMessage(MEDIA_TYPE_APPLICATION_JSON));
            matcher.getMessage().add(createMessage(MEDIA_TYPE_APPLICATION_XML));
            responseMessagingConfiguration.getStatusCode().add(matcher);
            responseMessageServiceImpl.setInitialized();
            responseMessageServiceImpl.updateConfiguration(responseMessagingConfiguration.getStatusCode());
            when(mockedRequest.getHeaderNames()).thenReturn(Collections.enumeration(Collections.singletonList("Accept")));
            response.sendError(I_AM_A_TEAPOT.value(), ESCAPE_THIS);
            response.uncommit();
        }

        @Test
        public void EscapeTheMessageForPlain() throws Exception {
            when(mockedRequest.getHeaders("Accept")).thenReturn(Collections.enumeration(Collections.singletonList(MEDIA_TYPE_TEXT_PLAIN)));

            responseMessageServiceImpl.handle(mockedRequest, response);

            assertEquals(
                    ESCAPE_THIS.trim(),
                    response.getOutputStreamAsString()
            );
        }

        @Test
        public void EscapeTheMessageForJson() throws Exception {
            when(mockedRequest.getHeaders("Accept")).thenReturn(Collections.enumeration(Collections.singletonList(MEDIA_TYPE_APPLICATION_JSON)));

            responseMessageServiceImpl.handle(mockedRequest, response);

            assertEquals(
                    "\\b\\n\\t\\f\\r\\\\\\\"'\\/&<>".trim(),
                    response.getOutputStreamAsString()
            );
        }

        @Test
        public void EscapeTheMessageForXml() throws Exception {
            when(mockedRequest.getHeaders("Accept")).thenReturn(Collections.enumeration(Collections.singletonList(MEDIA_TYPE_APPLICATION_XML)));

            responseMessageServiceImpl.handle(mockedRequest, response);

            assertEquals(
                    "\n\t\r\\&quot;&apos;/&amp;&lt;&gt;".trim(),
                    response.getOutputStreamAsString()
            );
        }

        private static Message createMessage(String mediaType) {
            Message message = new Message();
            message.setMediaType(mediaType);
            message.setContentType(mediaType);
            message.setValue("%M");
            return message;
        }
    }
}
