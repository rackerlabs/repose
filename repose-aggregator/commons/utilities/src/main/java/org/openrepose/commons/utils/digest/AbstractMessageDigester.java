package org.openrepose.commons.utils.digest;

import org.openrepose.commons.utils.io.MessageDigesterOutputStream;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.SoftReferenceObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.security.MessageDigest;

public abstract class AbstractMessageDigester implements MessageDigester {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractMessageDigester.class);
    private final ObjectPool<MessageDigest> MESSAGE_DIGEST_POOL;
    private static final int BYTE_BUFFER_SIZE = 1024;

    public AbstractMessageDigester() {
        MESSAGE_DIGEST_POOL = new SoftReferenceObjectPool<>(
                new MessageDigestConstructionStrategy(digestSpecName()));
    }

    protected abstract String digestSpecName();

    @Override
    public byte[] digestBytes(byte[] bytes) {
        byte[] rtn = new byte[0];
        MessageDigest pooledObject;
        try {
            pooledObject = MESSAGE_DIGEST_POOL.borrowObject();
            try {
                rtn = pooledObject.digest(bytes);
            } catch (Exception e) {
                MESSAGE_DIGEST_POOL.invalidateObject(pooledObject);
                pooledObject = null;
                LOG.error("Failed to utilize the MessageDigest. Reason: {}", e.getLocalizedMessage());
                LOG.trace("", e);
            } finally {
                if (pooledObject != null) {
                    MESSAGE_DIGEST_POOL.returnObject(pooledObject);
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
            pooledObject = MESSAGE_DIGEST_POOL.borrowObject();
            try {
                final MessageDigesterOutputStream output = new MessageDigesterOutputStream(pooledObject);
                final byte[] buffer = new byte[BYTE_BUFFER_SIZE];
                for (int read; (read = stream.read(buffer)) != -1; /*DO-NOTHING*/) {
                    output.write(buffer, 0, read);
                }
                stream.close();
                output.close();
                rtn = output.getDigest();
            } catch (Exception e) {
                MESSAGE_DIGEST_POOL.invalidateObject(pooledObject);
                pooledObject = null;
                LOG.error("Failed to utilize the MessageDigest. Reason: {}", e.getLocalizedMessage());
                LOG.trace("", e);
            } finally {
                if (pooledObject != null) {
                    MESSAGE_DIGEST_POOL.returnObject(pooledObject);
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to obtain a MessageDigest. Reason: {}", e.getLocalizedMessage());
            LOG.trace("", e);
        }
        return rtn;
    }
}
