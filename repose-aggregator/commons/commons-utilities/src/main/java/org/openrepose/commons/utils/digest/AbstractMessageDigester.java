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

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.SoftReferenceObjectPool;
import org.openrepose.commons.utils.io.MessageDigesterOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.security.MessageDigest;

public abstract class AbstractMessageDigester implements MessageDigester {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractMessageDigester.class);
    private static final int BYTE_BUFFER_SIZE = 1024;
    private final ObjectPool<MessageDigest> messageDigestPool;

    public AbstractMessageDigester() {
        messageDigestPool = new SoftReferenceObjectPool<>(
                new MessageDigestConstructionStrategy(digestSpecName()));
    }

    protected abstract String digestSpecName();

    @Override
    public byte[] digestBytes(byte[] bytes) {
        byte[] rtn = new byte[0];
        MessageDigest pooledObject;
        try {
            pooledObject = messageDigestPool.borrowObject();
            try {
                rtn = pooledObject.digest(bytes);
            } catch (Exception e) {
                messageDigestPool.invalidateObject(pooledObject);
                pooledObject = null;
                LOG.error("Failed to utilize the MessageDigest. Reason: {}", e.getLocalizedMessage());
                LOG.trace("", e);
            } finally {
                if (pooledObject != null) {
                    messageDigestPool.returnObject(pooledObject);
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to obtain a MessageDigest. Reason: {}", e.getLocalizedMessage());
            LOG.trace("", e);
        }
        return rtn;
    }

    @Override
    public byte[] digestStream(final InputStream stream) {
        byte[] rtn = new byte[0];
        MessageDigest pooledObject;
        try {
            pooledObject = messageDigestPool.borrowObject();
            try (MessageDigesterOutputStream output = new MessageDigesterOutputStream(pooledObject)) {
                final byte[] buffer = new byte[BYTE_BUFFER_SIZE];
                for (int read; (read = stream.read(buffer)) != -1; /*DO-NOTHING*/) {
                    output.write(buffer, 0, read);
                }
                stream.close();
                output.close();
                rtn = output.getDigest();
            } catch (Exception e) {
                messageDigestPool.invalidateObject(pooledObject);
                pooledObject = null;
                LOG.error("Failed to utilize the MessageDigest. Reason: {}", e.getLocalizedMessage());
                LOG.trace("", e);
            } finally {
                if (pooledObject != null) {
                    messageDigestPool.returnObject(pooledObject);
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to obtain a MessageDigest. Reason: {}", e.getLocalizedMessage());
            LOG.trace("", e);
        }
        return rtn;
    }
}
