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

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author zinic
 */
public abstract class OneTimeUseOutputStream extends OutputStream {

    private volatile boolean closed = false;

    private void checkForClosedStream() throws IOException {
        if (closed) {
            throw new IOException("InputStream has been closed. Further operations are prohibited");
        }
    }

    @Override
    public final void close() throws IOException {
        checkForClosedStream();

        closeStream();

        closed = true;
    }

    @Override
    public final void flush() throws IOException {
        checkForClosedStream();

        flushStream();
    }

    @Override
    public final void write(int b) throws IOException {
        checkForClosedStream();

        writeByte(b);
    }

    @Override
    public final void write(byte[] b) throws IOException {
        checkForClosedStream();

        super.write(b);
    }

    @Override
    public final void write(byte[] b, int off, int len) throws IOException {
        checkForClosedStream();

        super.write(b, off, len);
    }

    protected abstract void closeStream() throws IOException;

    protected abstract void flushStream() throws IOException;

    protected abstract void writeByte(int b) throws IOException;
}
