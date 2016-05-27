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
package org.openrepose.commons.utils.io.buffer;

import java.io.IOException;

public class SynchronizedByteBuffer implements ByteBuffer {

    private final ByteBuffer internalBuffer;

    public SynchronizedByteBuffer(ByteBuffer internalBuffer) {
        this.internalBuffer = internalBuffer;
    }

    @Override
    public synchronized void clear() {
        internalBuffer.clear();
    }

    @Override
    public synchronized int skip(int bytes) {
        return internalBuffer.skip(bytes);
    }

    @Override
    public synchronized int remaining() {
        return internalBuffer.remaining();
    }

    @Override
    public synchronized int put(byte[] b, int off, int len) throws IOException {
        return internalBuffer.put(b, off, len);
    }

    @Override
    public synchronized int put(byte[] b) throws IOException {
        return internalBuffer.put(b);
    }

    @Override
    public synchronized void put(byte b) throws IOException {
        internalBuffer.put(b);
    }

    @Override
    public synchronized int get(byte[] b, int off, int len) throws IOException {
        return internalBuffer.get(b, off, len);
    }

    @Override
    public synchronized int get(byte[] b) throws IOException {
        return internalBuffer.get(b);
    }

    @Override
    public synchronized byte get() throws IOException {
        return internalBuffer.get();
    }

    @Override
    public synchronized ByteBuffer copy() {
        return internalBuffer.copy();
    }

    @Override
    public synchronized int available() {
        return internalBuffer.available();
    }
}
