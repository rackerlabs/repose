package com.rackspace.papi.commons.util.io;

import com.rackspace.papi.commons.util.io.buffer.ByteBuffer;
import com.rackspace.papi.commons.util.io.buffer.CyclicByteBuffer;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;

@RunWith(Enclosed.class)
public class SimpleBufferInputStreamTest {

    public static class WhenReadingFromBufferStream {

        @Test
        public void shouldReadTillBufferIsEmpty() throws Exception {
             final ByteBuffer sbb = new CyclicByteBuffer();
             sbb.put("expected".getBytes());
             
             final InputStream is = new ByteBufferInputStream(sbb);
             
             final byte[] bytes = new byte[1024];
             int b, i;
             
             for (i = 0; (b = is.read()) != -1; i++) {
                 bytes[i] = (byte) b;
             }
             
             assertEquals("expected", new String(bytes, 0, i));
        }

        @Test
        public void shouldReadTillBufferIsEmptyUsingByteArrayRead() throws Exception {
             final ByteBuffer sbb = new CyclicByteBuffer();
             sbb.put("expected".getBytes());
             
             final InputStream is = new ByteBufferInputStream(sbb);
             
             final byte[] bytes = new byte[1024];
             int read = is.read(bytes);
             
             assertEquals("expected", new String(bytes, 0, read));
        }
    }
}
