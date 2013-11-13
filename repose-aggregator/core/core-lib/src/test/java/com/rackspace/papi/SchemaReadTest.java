package com.rackspace.papi;

import com.rackspace.papi.commons.util.io.ByteBufferInputStream;
import com.rackspace.papi.commons.util.io.ByteBufferOutputStream;
import com.rackspace.papi.commons.util.io.buffer.ByteBuffer;
import com.rackspace.papi.commons.util.io.buffer.CyclicByteBuffer;
import com.rackspace.papi.container.config.ObjectFactory;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;

import static org.junit.Assert.assertNotNull;

/**
 * I know this might feel like its in an odd place but I needed a schema to test
 * handing the xml unmarshaller a bytebuffer backed input stream
 * 
 */
@RunWith(Enclosed.class)
public class SchemaReadTest {

    public static class WhenReadingSchemaExamples {

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
}
