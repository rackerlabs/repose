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
package org.openrepose.commons.utils.digest;

import org.openrepose.commons.utils.io.MessageDigesterOutputStream;
import org.openrepose.commons.utils.pooling.ResourceContext;
import org.openrepose.commons.utils.pooling.ResourceContextException;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

public class MessageDigestResourceContext implements ResourceContext<MessageDigest, byte[]> {

    private static final int BYTE_BUFFER_SIZE = 1024;
    private final InputStream inputStream;

    public MessageDigestResourceContext(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public byte[] perform(MessageDigest resource) {
        final MessageDigesterOutputStream output = new MessageDigesterOutputStream(resource);
        final byte[] buffer = new byte[BYTE_BUFFER_SIZE];

        int read;

        try {
            while ((read = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }

            inputStream.close();
            output.close();
        } catch (IOException ioe) {
            throw new ResourceContextException("I/O Exception caught during input stream message digesting. Reason: " + ioe.getMessage(), ioe);
        }

        return output.getDigest();
    }
}
