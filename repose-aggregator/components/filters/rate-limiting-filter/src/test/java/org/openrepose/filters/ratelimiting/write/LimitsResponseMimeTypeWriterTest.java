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
package org.openrepose.filters.ratelimiting.write;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.filters.ratelimiting.util.LimitsEntityStreamTransformer;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class LimitsResponseMimeTypeWriterTest {

    public static class WhenWriting {
        private final LimitsResponseMimeTypeWriter writer;
        private final byte[] readableContents = {42};
        private final OutputStream out;
        private final LimitsEntityStreamTransformer transformer;

        public WhenWriting() throws IOException {
            transformer = mock(LimitsEntityStreamTransformer.class);
            out = mock(OutputStream.class);
            final InputStream in = mock(InputStream.class);
            this.writer = new LimitsResponseMimeTypeWriter(transformer);

            doNothing().when(transformer).streamAsJson(in, out);
            doNothing().when(out).write(readableContents);
        }

        @Test
        public void shouldChooseXmlPath() throws IOException {
            writer.writeLimitsResponse(readableContents, MediaType.APPLICATION_XML_TYPE, out);

            verify(out, times(1)).write(readableContents);
        }

        @Test
        public void shouldChooseJsonPath() throws IOException {
            writer.writeLimitsResponse(readableContents, MediaType.APPLICATION_JSON_TYPE, out);

            verify(transformer, times(1)).streamAsJson(any(InputStream.class), any(OutputStream.class));
        }
    }

}
