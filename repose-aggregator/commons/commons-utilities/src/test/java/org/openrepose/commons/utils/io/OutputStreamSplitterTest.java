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

import java.io.IOException;
import java.io.OutputStream;

import static org.mockito.Mockito.*;

public class OutputStreamSplitterTest {

    private OutputStream stream1;
    private OutputStream stream2;
    private OutputStream stream3;
    private OutputStreamSplitter splitter;

    @Before
    public void setUp() {
        stream1 = mock(OutputStream.class);
        stream2 = mock(OutputStream.class);
        stream3 = mock(OutputStream.class);

        splitter = new OutputStreamSplitter(stream1, stream2, stream3);
    }

    @Test
    public void shouldWriteToAllStreams() throws IOException {
        int value = 1;
        splitter.write(value);

        verify(stream1).write(eq(value));
        verify(stream2).write(eq(value));
        verify(stream3).write(eq(value));
    }

    @Test
    public void shouldWriteBytesToAllStreams() throws IOException {
        byte[] value = {1, 2, 3, 4, 5};
        splitter.write(value);

        verify(stream1).write(eq(value));
        verify(stream2).write(eq(value));
        verify(stream3).write(eq(value));
    }

    @Test
    public void shouldWriteBytesToAllStreams2() throws IOException {
        byte[] value = {1, 2, 3, 4, 5};
        int i = 3;
        int i1 = 7;

        splitter.write(value, i, i1);

        verify(stream1).write(eq(value), eq(i), eq(i1));
        verify(stream2).write(eq(value), eq(i), eq(i1));
        verify(stream3).write(eq(value), eq(i), eq(i1));
    }

    @Test
    public void shouldCloseAllStreams() throws IOException {
        splitter.close();

        verify(stream1).close();
        verify(stream2).close();
        verify(stream3).close();
    }

    @Test
    public void shouldFlushAllStreams() throws IOException {
        splitter.flush();

        verify(stream1).flush();
        verify(stream2).flush();
        verify(stream3).flush();
    }

}
