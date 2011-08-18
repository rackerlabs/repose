package com.rackspace.papi.commons.util.io;

import com.rackspace.papi.commons.util.io.SimpleByteBufferInputStream;
import com.rackspace.papi.commons.util.io.buffer.CyclicSimpleByteBuffer;
import com.rackspace.papi.commons.util.io.buffer.SimpleByteBuffer;
import java.io.InputStream;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class SimpleBufferInputStreamTest {

    public static class WhenReadingFromBufferStream {

        @Test
        public void shouldReadTillBufferIsEmpty() throws Exception {
             final SimpleByteBuffer sbb = new CyclicSimpleByteBuffer();
             sbb.put("expected".getBytes());
             
             final InputStream is = new SimpleByteBufferInputStream(sbb);
             
             final byte[] bytes = new byte[1024];
             int b, i;
             
             for (i = 0; (b = is.read()) != -1; i++) {
                 bytes[i] = (byte) b;
             }
             
             assertEquals("expected", new String(bytes, 0, i));
        }

        @Test
        public void shouldReadTillBufferIsEmptyUsingByteArrayRead() throws Exception {
             final SimpleByteBuffer sbb = new CyclicSimpleByteBuffer();
             sbb.put("expected".getBytes());
             
             final InputStream is = new SimpleByteBufferInputStream(sbb);
             
             final byte[] bytes = new byte[1024];
             int read = is.read(bytes);
             
             assertEquals("expected", new String(bytes, 0, read));
        }
    }
}
