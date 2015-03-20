/*
 * #%L
 * Repose
 * %%
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * %%
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
 * #L%
 */
package org.openrepose.core.services.rms;

import org.openrepose.commons.utils.InputStreamUtilities;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.io.ByteBufferInputStream;
import org.openrepose.commons.utils.io.ByteBufferServletOutputStream;
import org.openrepose.commons.utils.io.buffer.ByteBuffer;
import org.openrepose.commons.utils.io.buffer.CyclicByteBuffer;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.rms.config.Message;
import org.openrepose.core.services.rms.config.OverwriteType;
import org.openrepose.core.services.rms.config.ResponseMessagingConfiguration;
import org.openrepose.core.services.rms.config.StatusCodeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class ResponseMessageServiceImplTest {

   public static class WhenHandlingResponse {
      private final ResponseMessageServiceImpl rmsImpl = new ResponseMessageServiceImpl(mock(ConfigurationService.class));
      private final ResponseMessagingConfiguration configurationObject = new ResponseMessagingConfiguration();
      private Enumeration<String> headerValueEnumeration = null;
      private final Vector<String> acceptValues = new Vector<String>(1);

      private static final String MESSAGE = "This is the replaced message";

      private HttpServletRequest mockedRequest = mock(HttpServletRequest.class);
      private MutableHttpServletResponse mockedResponse = mock(MutableHttpServletResponse.class);

      @Before
      public void setup() {
         acceptValues.addAll(Arrays.asList("application/json"));
         headerValueEnumeration = acceptValues.elements();
         List<String> headerNames = new ArrayList<String>();
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
         when(mockedResponse.hasBody()).thenReturn(false);

         // Hook up response body stream to mocked response
         final ByteBuffer internalBuffer = new CyclicByteBuffer();
         final ServletOutputStream outputStream = new ByteBufferServletOutputStream(internalBuffer);
         final ByteBufferInputStream inputStream = new ByteBufferInputStream(internalBuffer);
         when(mockedResponse.getOutputStream()).thenReturn(outputStream);
         when(mockedResponse.getBufferedOutputAsInputStream()).thenReturn(inputStream);
        

         rmsImpl.handle(mockedRequest, mockedResponse);

         String result = InputStreamUtilities.streamToString(new ByteBufferInputStream(internalBuffer));
         assertTrue(StringUtilities.nullSafeEquals(MESSAGE, result));
      }

      @Test
      public void shouldPreserveIfEmptyAndBody() throws IOException {
         when(mockedResponse.hasBody()).thenReturn(true);

         // Hook up response body stream to mocked response
         final ByteBuffer internalBuffer = new CyclicByteBuffer();
         internalBuffer.put("hello there".getBytes());
         final ServletOutputStream outputStream = new ByteBufferServletOutputStream(internalBuffer);
         final ByteBufferInputStream inputStream = new ByteBufferInputStream(internalBuffer);
         when(mockedResponse.getBufferedOutputAsInputStream()).thenReturn(inputStream);
         when(mockedResponse.getOutputStream()).thenReturn(outputStream);
        
         rmsImpl.handle(mockedRequest, mockedResponse);

         String result = InputStreamUtilities.streamToString(new ByteBufferInputStream(internalBuffer));
         assertTrue(StringUtilities.nullSafeEquals("hello there", result));
      }           
   }
}
