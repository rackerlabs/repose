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
package org.openrepose.nodeservice.httpcomponent;

import org.apache.http.HttpEntity;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class HttpComponentInputStreamTest {

    public static class WhenReadingNullInputStream {
        private HttpEntity entity;
        private HttpComponentInputStream input;

        @Before
        public void setUp() throws IOException {
            entity = mock(HttpEntity.class);
            when(entity.getContent()).thenReturn(null);
            input = new HttpComponentInputStream(entity);
        }

        @Test
        public void should() throws IOException {
            final int expected = -1;


            assertEquals("Should return -1 when reading a null input stream", expected, input.read());
            assertEquals("Should return -1 when reading a null input stream", expected, input.read(new byte[10]));
            assertEquals("Should return -1 when reading a null input stream", expected, input.read(new byte[10], 0, 5));
            assertEquals("Should return -1 when skipping a null input stream", expected, input.skip(1));
            assertFalse("Should not support marking a null stream", input.markSupported());

            assertEquals("Should return 0 available when reading null input stream", 0, input.available());
        }
    }

    public static class WhenReadingNonNullInputStream {
        private HttpEntity entity;
        private HttpComponentInputStream input;
        private InputStream stream;

        @Before
        public void setUp() throws IOException {
            entity = mock(HttpEntity.class);
            stream = mock(InputStream.class);
            when(entity.getContent()).thenReturn(stream);
            input = new HttpComponentInputStream(entity);
        }

        @Test
        public void shouldCallUnderlyingStream() throws IOException {

            input.read();
            verify(stream).read();

            byte[] bytes = new byte[10];

            input.read(bytes);
            verify(stream).read(bytes);

            input.read(bytes, 0, 5);
            verify(stream).read(bytes, 0, 5);

            input.markSupported();
            verify(stream).markSupported();

            input.mark(5);
            verify(stream).mark(5);

            input.close();
            verify(stream).close();

            input.available();
            verify(stream).available();

            input.reset();
            verify(stream).reset();
        }
    }
}
