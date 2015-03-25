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
