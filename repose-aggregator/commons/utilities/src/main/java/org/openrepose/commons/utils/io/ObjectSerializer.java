package org.openrepose.commons.utils.io;

import java.io.*;

public final class ObjectSerializer {

    private final ClassLoader classLoader;

    public ObjectSerializer(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public Serializable readObject(byte[] bytes) throws IOException, ClassNotFoundException {
        return readObject(new ByteArrayInputStream(bytes));
    }

    public Serializable readObject(InputStream is) throws IOException, ClassNotFoundException {
        final ObjectInputStream ois = new ClassLoaderAwareObjectInputStream(is, classLoader);
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
