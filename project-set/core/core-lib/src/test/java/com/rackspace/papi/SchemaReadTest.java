package com.rackspace.papi;

import com.rackspace.papi.commons.util.io.SimpleByteBufferInputStream;
import com.rackspace.papi.commons.util.io.SimpleByteBufferOutputStream;
import com.rackspace.papi.commons.util.io.buffer.CyclicSimpleByteBuffer;
import com.rackspace.papi.commons.util.io.buffer.SimpleByteBuffer;
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
 * handing the xml unmarshaller a simplebytebuffer backed input stream
 * 
 */
@RunWith(Enclosed.class)
public class SchemaReadTest {

    public static class WhenReadingSchemaExamples {

        @Test
        public void shouldReadFromSimpleByteBuffers() throws Exception {
            final SimpleByteBuffer buffer = new CyclicSimpleByteBuffer();
            final SimpleByteBufferOutputStream bos = new SimpleByteBufferOutputStream(buffer);

            final InputStream xmlInput = SchemaReadTest.class.getResourceAsStream("/META-INF/schema/examples/xml/container.cfg.xml");

            final byte[] bytes = new byte[1024];
            int read;

            while ((read = xmlInput.read(bytes)) != -1) {
                bos.write(bytes, 0, read);
            }

            xmlInput.close();
            bos.close();

            final JAXBContext context = JAXBContext.newInstance(ObjectFactory.class);
            final Unmarshaller unmarshaller = context.createUnmarshaller();

            final SimpleByteBufferInputStream sbbis = new SimpleByteBufferInputStream(buffer);

            final Object o = unmarshaller.unmarshal(sbbis);

            assertNotNull(o);
        }
    }
}
