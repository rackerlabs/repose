/*
 *  Copyright 2010 Rackspace.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package com.rackspace.papi.components.logging.util;

import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;


/**
 *
 * @author jhopper
 */
public class FileLogger implements SimpleLogger {
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(FileLogger.class);

    private static final ByteBuffer NEWLINE = ByteBuffer.wrap("\n".getBytes());
    static final int BUFFER_LIMIT = 2048;

    private final ByteBuffer buffer;
    private final File f;

    public FileLogger(File f) {
        this.f = f;

        buffer = ByteBuffer.allocate(BUFFER_LIMIT);
    }

    @Override
    public void log(String st) {
        final byte[] stringBytes = st.getBytes();

        try {
            final FileOutputStream fileOutputStream = new FileOutputStream(f, true);
            final FileChannel channel = fileOutputStream.getChannel();

            int index = buffer.arrayOffset();

            while (index < stringBytes.length) {
                final int remainder = stringBytes.length - index;
                final int length = getRemainderAvailable(remainder);

                buffer.put(stringBytes, index, length);
                buffer.flip();

                channel.write(buffer);

                if (buffer.hasRemaining()) {
                    buffer.compact();
                } else {
                    buffer.clear();
                }

                index += length;
            }

            channel.write(NEWLINE);
            NEWLINE.rewind();

            channel.close();
            fileOutputStream.close();
        } catch (IOException ioe) {
            LOG.error(ioe.getMessage(), ioe);
        }
    }

    int getRemainderAvailable(int remainder) {
        return remainder > BUFFER_LIMIT ? BUFFER_LIMIT : remainder;
    }

    @Override
    public void destroy() {
    }
}
