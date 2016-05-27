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
package org.openrepose.core.services.headers.response;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author malconis
 */
@RunWith(Enclosed.class)
public class ViaResponseHeaderBuilderTest {

    @Before
    public void setUp() {


    }

    @Ignore
    public static abstract class TestParent {

        protected ViaResponseHeaderBuilder builder;
        protected HttpServletRequest request;

        @Before
        public final void beforeAll() {
            builder = new ViaResponseHeaderBuilder("2.4.0", getVia());
            request = mock(HttpServletRequest.class);


        }

        public abstract String getVia();
    }

    public static class WhenBuildingResponseViaHeadersWithEmptyReceivedBy extends TestParent {

        @Override
        public String getVia() {
            return "";
        }

        @Before
        public void standUp() {

            when(request.getProtocol()).thenReturn("HTTP/1.1");
        }

        @Test
        public void shouldGenerateViaHeaderWithStandardValues() {
            String via = builder.buildVia(request);

            assertEquals("1.1 Repose (Repose/2.4.0)", via);
        }
    }

    public static class WhenBuildingResponseViaHeadersWithReceivedBy extends TestParent {

        @Override
        public String getVia() {
            return "ReposeTest";
        }

        @Before
        public void standUp() {

            when(request.getProtocol()).thenReturn("HTTP/1.1");
        }

        @Test
        public void shouldGenerateViaHeaderWithStandardValues() {
            String via = builder.buildVia(request);

            assertEquals("1.1 ReposeTest (Repose/2.4.0)", via);
        }
    }

    public static class WhenBuildingResponseViaHeadersWithEmptyResponseProtocolAndReceivedBy extends TestParent {

        @Override
        public String getVia() {
            return "ReposeTest";
        }

        @Before
        public void standUp() {

            when(request.getProtocol()).thenReturn("");
        }

        @Ignore
        @Test
        public void shouldGenerateViaHeaderWithStandardValues() {
            String via = builder.buildVia(request);

            assertEquals("1.1 ReposeTest (Repose/2.4.0)", via);
        }
    }
}
