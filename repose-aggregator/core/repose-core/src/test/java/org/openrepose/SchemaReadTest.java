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
package org.openrepose;

import org.junit.Test;
import org.openrepose.commons.utils.io.ByteBufferInputStream;
import org.openrepose.commons.utils.io.ByteBufferOutputStream;
import org.openrepose.commons.utils.io.buffer.ByteBuffer;
import org.openrepose.commons.utils.io.buffer.CyclicByteBuffer;
import org.openrepose.core.container.config.ObjectFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;

import static org.junit.Assert.assertNotNull;

/**
 * I know this might feel like its in an odd place but I needed a schema to test
 * handing the xml unmarshaller a bytebuffer backed input stream
 */
public class SchemaReadTest {

    @Test
    public void shouldReadFromByteBuffers() throws Exception {
        final ByteBuffer buffer = new CyclicByteBuffer();
        final ByteBufferOutputStream bos = new ByteBufferOutputStream(buffer);

        final InputStream xmlInput = SchemaReadTest.class.getResourceAsStream("/META-INF/schema/examples/container.cfg.xml");

        final byte[] bytes = new byte[1024];
        int read;

        while ((read = xmlInput.read(bytes)) != -1) {
            bos.write(bytes, 0, read);
        }

        xmlInput.close();
        bos.close();

        final JAXBContext context = JAXBContext.newInstance(ObjectFactory.class);
        final Unmarshaller unmarshaller = context.createUnmarshaller();

        final ByteBufferInputStream sbbis = new ByteBufferInputStream(buffer);

        final Object o = unmarshaller.unmarshal(sbbis);

        assertNotNull(o);
    }
}
