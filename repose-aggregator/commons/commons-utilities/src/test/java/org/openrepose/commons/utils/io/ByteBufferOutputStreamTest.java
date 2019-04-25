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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openrepose.commons.utils.io.buffer.ByteBuffer;

import java.io.IOException;

import static org.mockito.Mockito.*;

public class ByteBufferOutputStreamTest {

    private final Integer BUFFER_SIZE = 100;
    private ByteBuffer buffer;
    private ByteBufferOutputStream stream;

    @Before
    public void setUp() {
        buffer = mock(ByteBuffer.class);
        when(buffer.available()).thenReturn(BUFFER_SIZE);
        stream = new ByteBufferOutputStream(buffer);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void shouldWriteByte() throws IOException {
        int b = 1;
        stream.writeByte(b);
        verify(buffer).put(eq((byte) b));
    }

    @Test
    public void shouldFlushStream() throws IOException {
        stream.flushStream();
        verify(buffer, times(1)).skip(BUFFER_SIZE);
    }

    @Test
    public void shouldCallWriteByte() throws IOException {
        int b = 1;
        stream.write(b);
        verify(buffer).put(eq((byte) b));
    }

    @Test(expected = IOException.class)
    public void whenWritingDataShouldThrowExceptionIfStreamIsClosed() throws IOException {
        byte[] b = {1};
        stream.close();
        stream.write(b);
    }

    @Test
    public void shouldCallFlushStream() throws IOException {
        stream.flush();
        verify(buffer).skip(anyInt());
    }

    @Test(expected = IOException.class)
    public void whenFlushingStreamsShouldThrowExceptionIfStreamIsClosed() throws IOException {
        stream.close();
        stream.flush();
    }

}
