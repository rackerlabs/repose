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
import org.openrepose.commons.utils.io.buffer.ByteBuffer;

import java.io.IOException;

import static org.mockito.Mockito.*;

public class ByteBufferServletOutputStreamTest {

    private ByteBuffer buffer;
    private ByteBufferServletOutputStream stream;

    @Before
    public void setUp() {
        buffer = mock(ByteBuffer.class);

        stream = new ByteBufferServletOutputStream(buffer);
    }

    @Test
    public void shouldWriteByte() throws IOException {
        int b = 1;
        stream.write(b);
        verify(buffer).put(eq((byte) b));
    }

    @Test
    public void shouldWriteBytes() throws IOException {
        byte[] bytes = new byte[10];
        stream.write(bytes);
        verify(buffer).put(eq(bytes));
    }

    @Test
    public void shouldWriteBytesWithOffsetAndLength() throws IOException {
        byte[] bytes = new byte[10];
        int offset = 1;
        int length = 10;
        stream.write(bytes, offset, length);
        verify(buffer).put(eq(bytes), eq(offset), eq(length));
    }

}
