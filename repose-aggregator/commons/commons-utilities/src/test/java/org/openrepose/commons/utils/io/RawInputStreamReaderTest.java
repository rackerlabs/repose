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
package org.openrepose.commons.utils.io;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class RawInputStreamReaderTest {

    private String data = "Some String of Data";
    private ByteArrayInputStream inStream;
    private RawInputStreamReader reader;

    @Before
    public void setUp() {

        reader = RawInputStreamReader.instance();
        inStream = new ByteArrayInputStream(data.getBytes());
    }

    @Test
    public void shouldReadBuffer() throws IOException {
        byte[] actual = reader.readFully(inStream);

        assertEquals(data, new String(actual));
    }

    @Test
    public void shouldReadBufferWithLimit() throws IOException {
        byte[] actual = reader.readFully(inStream, data.length());

        assertEquals(data, new String(actual));
    }

    @Test(expected = BufferCapacityException.class)
    public void shouldThrowBufferCapacityException() throws IOException {
        byte[] actual = reader.readFully(inStream, 1);
    }

    @Test
    public void shouldCopyInputStreamToOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        reader.copyTo(inStream, baos);

        assertEquals(data, new String(baos.toByteArray()));
    }

    @Test
    public void shouldReturnNumberOfBytesCopied() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        long actual = reader.copyTo(inStream, baos);

        assertEquals(data.length(), actual);
    }

}
