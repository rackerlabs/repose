package com.rackspace.papi.commons.util.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RawInputStreamReader {

    private static final RawInputStreamReader INSTANCE = new RawInputStreamReader();
    private static final int DEFAULT_INTERNAL_BUFFER_SIZE = 1024;

    public static RawInputStreamReader instance() {
        return INSTANCE;
    }

    private RawInputStreamReader() {
    }

    public long copyTo(InputStream is, OutputStream os) throws IOException {
        final byte[] internalBuffer = new byte[DEFAULT_INTERNAL_BUFFER_SIZE];

        long total = 0;
        int read;

        while ((read = is.read(internalBuffer)) != -1) {
            os.write(internalBuffer, 0, read);
            total += read;
        }
        
        os.close();
        
        return total;
    }

    public byte[] readFully(InputStream is) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final byte[] internalBuffer = new byte[DEFAULT_INTERNAL_BUFFER_SIZE];

        int read;

        while ((read = is.read(internalBuffer)) != -1) {
            baos.write(internalBuffer, 0, read);
        }

        return baos.toByteArray();
    }
}
