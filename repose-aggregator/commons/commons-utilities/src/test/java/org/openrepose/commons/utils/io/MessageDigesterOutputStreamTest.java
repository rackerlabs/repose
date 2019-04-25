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
import java.security.MessageDigest;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.*;

public class MessageDigesterOutputStreamTest {

    private MessageDigesterOutputStream stream;
    private MessageDigest digest;
    private byte[] digestBytes = {0, 1, 2, 3, 4, 5};

    @Before
    public void setUp() {
        digest = mock(MessageDigest.class);
        when(digest.digest()).thenReturn(digestBytes);
        stream = new MessageDigesterOutputStream(digest);
    }

    @Test
    public void shouldWriteBytes() throws IOException {
        int b = 1;
        stream.write(b);
        verify(digest).update(eq((byte) b));
    }

    @Test
    public void shouldGetDigestWhenClosingStream() throws IOException {
        stream.closeStream();
        verify(digest).digest();
    }

    @Test
    public void shouldResetDigestWhenFlushingStream() throws IOException {
        stream.flushStream();
        verify(digest).reset();
    }

    @Test
    public void shouldGetDigestBytes() throws IOException {
        stream.closeStream();
        byte[] actual = stream.getDigest();
        //assertEquals(digestBytes, actual);
        assertArrayEquals(digestBytes, actual);
    }

}
