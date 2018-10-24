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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// todo: replace this class with Apache Commons IOUtils
public final class RawInputStreamReader {

    public static final int DEFAULT_INTERNAL_BUFFER_SIZE = 4096;
    private static final RawInputStreamReader INSTANCE = new RawInputStreamReader();

    private RawInputStreamReader() {
    }

    public static RawInputStreamReader instance() {
        return INSTANCE;
    }

    public long copyTo(InputStream is, OutputStream os) throws IOException {
        return copyTo(is, os, DEFAULT_INTERNAL_BUFFER_SIZE);
    }

    public long copyTo(InputStream is, OutputStream os, int bufferSize) throws IOException {
        final byte[] internalBuffer = new byte[bufferSize];

        long total = 0;
        int read;

        while ((read = is.read(internalBuffer)) != -1) {
            os.write(internalBuffer, 0, read);
            total += read;
        }

        return total;
    }

    public byte[] readFully(InputStream is) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final byte[] internalBuffer = new byte[DEFAULT_INTERNAL_BUFFER_SIZE];

        int read;

        while ((read = is.read(internalBuffer)) != -1) {
            baos.write(internalBuffer, 0, read);
        }

        return baos.toByteArray();
    }

    public byte[] readFully(InputStream is, long byteLimit) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final byte[] internalBuffer = new byte[DEFAULT_INTERNAL_BUFFER_SIZE];

        int read;
        long limit = byteLimit;

        while ((read = is.read(internalBuffer)) != -1) {
            limit -= read;

            if (limit < 0) {
                throw new BufferCapacityException("Read limit reached. Max buffer size: " + limit + " bytes");
            }

            baos.write(internalBuffer, 0, read);
        }

        return baos.toByteArray();
    }
}
