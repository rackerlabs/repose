package org.openrepose.commons.utils.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

public class ClassLoaderAwareObjectInputStream extends ObjectInputStream {
    private static final Logger LOG = LoggerFactory.getLogger(ClassLoaderAwareObjectInputStream.class);
    private final ClassLoader classLoader;

    public ClassLoaderAwareObjectInputStream(InputStream in, ClassLoader classLoader) throws IOException {
        super(in);
        this.classLoader = classLoader;
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        try {
            return classLoader.loadClass(desc.getName());
        } catch (ClassNotFoundException ignored) {
            LOG.trace("Couldn't load class {}.", desc.getName(), ignored);
        }
        return super.resolveClass(desc);
    }
}
