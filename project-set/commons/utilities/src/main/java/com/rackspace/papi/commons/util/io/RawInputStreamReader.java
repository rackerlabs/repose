package com.rackspace.papi.commons.util.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class RawInputStreamReader {
    private static final RawInputStreamReader INSTANCE = new RawInputStreamReader();
    private static final int DEFAULT_INTERNAL_BUFFER_SIZE = 1024;
    
    public static RawInputStreamReader instance() {
        return INSTANCE;
    }
    
    private RawInputStreamReader() {}
    
    public byte[] readFully(InputStream is) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final byte[] internalBuffer = new byte[DEFAULT_INTERNAL_BUFFER_SIZE];
        
        int read;
        
        while((read = is.read(internalBuffer)) != -1) {
            baos.write(internalBuffer, 0, read);
        }
        
        is.close();
        
        return baos.toByteArray();
    }
}
