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
package org.openrepose.commons.utils.io.stream;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author zinic
 */
public class LimitedReadInputStream extends InputStream {

    private final InputStream delegateStream;
    private final long readLimit;
    private long bytesRead;

    public LimitedReadInputStream(long readLimit, InputStream delegateStream) {
        this.delegateStream = delegateStream;
        this.readLimit = readLimit;
    }

    @Override
    public void mark(int readlimit) {
        delegateStream.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        delegateStream.reset();
    }

    @Override
    public boolean markSupported() {
        return delegateStream.markSupported();
    }

    @Override
    public int read() throws IOException {
        if (++bytesRead > readLimit) {
            throw new ReadLimitReachedException("Read limit of " + readLimit + " for input stream has been reached");
        }

        return delegateStream.read();
    }
}
