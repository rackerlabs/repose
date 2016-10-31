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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author zinic
 */
public class MessageDigesterOutputStream extends OneTimeUseOutputStream {

    private static final Logger LOG = LoggerFactory.getLogger(MessageDigesterOutputStream.class);
    private static final int BUFFER_SIZE = 2048;
    private final MessageDigest digest;
    private byte[] digestBytes;

    public MessageDigesterOutputStream(MessageDigest digest) {
        this.digest = digest;
    }

    public static byte[] calculateMd5Hash(URL url) {
        {
            InputStream urlInput = null;
            try (MessageDigesterOutputStream mdos = new MessageDigesterOutputStream(MessageDigest.getInstance("MD5"))) {
                urlInput = url.openStream();

                int read;
                byte[] buffer = new byte[BUFFER_SIZE];
                while ((read = urlInput.read(buffer)) > -1) {
                    mdos.write(buffer, 0, read);
                }

                mdos.close();

                return mdos.getDigest();
            } catch (IOException ex) {
                LOG.warn("Error reading stream: " + url.toExternalForm(), ex);
            } catch (NoSuchAlgorithmException ex) {
                LOG.warn("Invalid digest algorithm: MD5", ex);
            } finally {
                try {
                    if (urlInput != null) {
                        urlInput.close();
                    }
                } catch (IOException ex) {
                    LOG.warn("Error closing stream", ex);
                }
            }
        }

        return new byte[0];

    }

    public byte[] getDigest() {
        return digestBytes.clone();
    }

    @Override
    public void writeByte(int b) throws IOException {
        digest.update((byte) b);
    }

    @Override
    public void closeStream() throws IOException {
        digestBytes = digest.digest();
    }

    @Override
    public void flushStream() throws IOException {
        digest.reset();
    }
}
