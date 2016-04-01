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
package org.openrepose.nodeservice.httpcomponent;

import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;

public class HttpComponentInputStream extends InputStream {
    private final HttpEntity entity;
    private final InputStream source;

    public HttpComponentInputStream(HttpEntity entity) throws IOException {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        this.entity = entity;
        source = entity.getContent();
    }

    @Override
    public void close() throws IOException {
        if (entity != null) {
            EntityUtils.consume(entity);
        }

        if (source != null) {
            source.close();
        }
    }

    @Override
    public int read() throws IOException {
        if (source == null) {
            return -1;
        }

        return source.read();
    }

    @Override
    public int read(byte[] bytes) throws IOException {
        if (source == null) {
            return -1;
        }

        return source.read(bytes);
    }

    @Override
    public int read(byte[] bytes, int i, int i1) throws IOException {
        if (source == null) {
            return -1;
        }

        return source.read(bytes, i, i1);
    }

    @Override
    public long skip(long l) throws IOException {
        if (source == null) {
            return -1;
        }
        return source.skip(l);
    }

    @Override
    public int available() throws IOException {
        if (source == null) {
            return 0;
        }

        return source.available();
    }

    @Override
    public synchronized void mark(int i) {
        if (source != null) {
            source.mark(i);
        }
    }

    @Override
    public synchronized void reset() throws IOException {
        if (source != null) {
            source.reset();
        }
    }

    @Override
    public boolean markSupported() {
        if (source != null) {
            return source.markSupported();
        }

        return false;
    }

}
