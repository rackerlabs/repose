package com.rackspace.repose.service.ratelimit.cache.util;

import java.io.*;

public final class ObjectSerializer {

    private static final ObjectSerializer INSTANCE = new ObjectSerializer();

    public static ObjectSerializer instance() {
        return INSTANCE;
    }

    private ObjectSerializer() {
    }

    public byte[] writeObject(Serializable o) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(baos);

        oos.writeObject(o);

        return baos.toByteArray();
    }
}
