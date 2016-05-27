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
import java.util.Arrays;
import java.util.List;

/**
 *
 *
 */
public final class OutputStreamSplitter extends OutputStream {

    private final List<OutputStream> streamsToSplitTo;

    public OutputStreamSplitter(OutputStream... jis) {
        streamsToSplitTo = Arrays.asList(jis);
    }

    @Override
    public void write(int i) throws IOException {
        for (OutputStream os : streamsToSplitTo) {
            os.write(i);
        }
    }

    @Override
    public void close() throws IOException {
        for (OutputStream os : streamsToSplitTo) {
            os.close();
        }
    }

    @Override
    public void flush() throws IOException {
        for (OutputStream os : streamsToSplitTo) {
            os.flush();
        }
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        for (OutputStream os : streamsToSplitTo) {
            os.write(bytes);
        }
    }

    @Override
    public void write(byte[] bytes, int i, int i1) throws IOException {
        for (OutputStream os : streamsToSplitTo) {
            os.write(bytes, i, i1);
        }
    }
}
