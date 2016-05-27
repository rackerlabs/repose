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

import org.openrepose.commons.utils.io.buffer.ByteBuffer;

import javax.servlet.ServletOutputStream;
import java.io.IOException;

public class ByteBufferServletOutputStream extends ServletOutputStream {

    private final ByteBuffer sharedBuffer;
    private volatile boolean closed;

    public ByteBufferServletOutputStream(ByteBuffer sharedBuffer) {
        this.sharedBuffer = sharedBuffer;

        closed = false;
    }

    private void checkForClosedStream() throws IOException {
        //TODO: We need to compensate for systems outside of powerapi trying to close the streams
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() throws IOException {
        checkForClosedStream();

        closed = true;
    }

    @Override
    public void flush() throws IOException {
        checkForClosedStream();
    }

    @Override
    public void write(int b) throws IOException {
        checkForClosedStream();

        sharedBuffer.put((byte) b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        checkForClosedStream();

        sharedBuffer.put(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        checkForClosedStream();

        sharedBuffer.put(b, off, len);
    }
}
