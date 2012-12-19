package com.rackspace.papi.commons.util.classloader.digest;

import com.rackspace.papi.commons.util.pooling.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * 
 */
public final class Sha1Digester {

    public static final String DEFAULT_DIGEST_SPEC = "SHA1";
    
    private static final Pool<MessageDigest> MESSAGE_DIGEST_POOL = new GenericBlockingResourcePool<MessageDigest>(
            new ConstructionStrategy<MessageDigest>() {

                @Override
                public MessageDigest construct() {
                    try {
                        return MessageDigest.getInstance(DEFAULT_DIGEST_SPEC);
                    } catch (NoSuchAlgorithmException nsae) {
                        throw new ResourceConstructionException(
                                "Digest algorithm: " + DEFAULT_DIGEST_SPEC + " is not registered or available.", nsae);
                    }
                }
            });

    private final byte[] digest;

    public Sha1Digester(final byte[] sourceBytes) {
        digest = MESSAGE_DIGEST_POOL.use(new ResourceContext<MessageDigest, byte[]>() {

            @Override
            public byte[] perform(MessageDigest resource) {
                return resource.digest(sourceBytes);
            }
        });
    }

    public byte[] getDigest() {
        return (byte[])digest.clone();
    }
}
