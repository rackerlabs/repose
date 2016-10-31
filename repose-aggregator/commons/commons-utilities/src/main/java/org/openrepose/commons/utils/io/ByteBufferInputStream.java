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

import javax.servlet.ServletInputStream;
import java.io.IOException;

public class ByteBufferInputStream extends ServletInputStream {

    private final ByteBuffer sharedBuffer;
    private volatile boolean closed;

    public ByteBufferInputStream(ByteBuffer sharedBuffer) {
        this.sharedBuffer = sharedBuffer;

        closed = false;
    }

    private void checkForClosedStream() throws IOException {
        if (closed) {
            throw new IOException("InputStream has been closed. Futher operations are prohibited");
        }
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public int available() throws IOException {
        checkForClosedStream();

        return sharedBuffer.available();
    }

    @Override
    public void close() throws IOException {
        checkForClosedStream();

        closed = true;
    }

    @Override
    public int read() throws IOException {
        checkForClosedStream();

        return sharedBuffer.get();
    }

    @Override
    public int read(byte[] b) throws IOException {
        checkForClosedStream();

        return normalizeBufferReadLength(sharedBuffer.get(b));
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkForClosedStream();

        return normalizeBufferReadLength(sharedBuffer.get(b, off, len));
    }

    private int normalizeBufferReadLength(int readLength) {
        return readLength == 0 ? -1 : readLength;
    }

    @Override
    public long skip(long n) throws IOException {
        checkForClosedStream();

        long skipped;
        long skippedTotal = 0;
        long c = n;

        if (c > 0) {
            do {
                int toSkip = c > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) c;
                skipped = sharedBuffer.skip(toSkip);

                skippedTotal += skipped;
                c -= skipped;
            } while (c > 0 && skipped > 0);
        }

        return skippedTotal;
    }
}
