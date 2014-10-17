package org.openrepose.commons.utils.classloader.digest;

import org.openrepose.commons.utils.pooling.ResourceConstructionException;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.SoftReferenceObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 *
 */
public final class Sha1Digester {

    private static final Logger LOG = LoggerFactory.getLogger(Sha1Digester.class);
    public static final String DEFAULT_DIGEST_SPEC = "SHA1";
    
    private static final ObjectPool<MessageDigest> MESSAGE_DIGEST_POOL = new SoftReferenceObjectPool<>(
            new BasePoolableObjectFactory<MessageDigest>() {

                @Override
                public MessageDigest makeObject() {
                    try {
                        return MessageDigest.getInstance(DEFAULT_DIGEST_SPEC);
                    } catch (NoSuchAlgorithmException nsae) {
                        throw new ResourceConstructionException(
                                "Digest algorithm: " + DEFAULT_DIGEST_SPEC + " is not registered or available.", nsae);
                    }
                }
            });

    private byte[] digest = new byte[0];

    public Sha1Digester(final byte[] sourceBytes) {
        MessageDigest pooledObject = null;
        try {
            pooledObject = MESSAGE_DIGEST_POOL.borrowObject();
            try {
                digest = pooledObject.digest(sourceBytes);
            } catch (Exception e) {
                MESSAGE_DIGEST_POOL.invalidateObject(pooledObject);
                pooledObject = null;
                LOG.error("Failed to utilize the MessageDigest.", e);
            } finally {
                if (null != pooledObject) {
                    MESSAGE_DIGEST_POOL.returnObject(pooledObject);
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to obtain a MessageDigest", e);
        }
    }

    public byte[] getDigest() {
        return digest.clone();
    }
}
