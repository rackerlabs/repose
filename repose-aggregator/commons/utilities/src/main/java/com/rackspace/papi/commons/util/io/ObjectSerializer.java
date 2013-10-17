package com.rackspace.papi.commons.util.io;

import java.io.*;

public final class ObjectSerializer {

    private static final ObjectSerializer INSTANCE = new ObjectSerializer();

    public static ObjectSerializer instance() {
        return INSTANCE;
    }

    private ObjectSerializer() {
    }

    public Serializable readObject(byte[] bytes) throws IOException, ClassNotFoundException {
        return readObject(new ByteArrayInputStream(bytes));
    }
    
    public Serializable readObject(InputStream is) throws IOException, ClassNotFoundException {
        final ObjectInputStream ois = new ThreadContextAwareObjectInputStream(is);
        final Serializable readObject = (Serializable) ois.readObject();

        ois.close();
        return readObject;
    }

    public byte[] writeObject(Serializable o) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(baos);

        oos.writeObject(o);

        return baos.toByteArray();
    }
}
