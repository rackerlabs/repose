package com.rackspace.papi.commons.util.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

/**
 *
 * 
 */
public final class OutputStreamSplitter extends OutputStream {

    private final List<OutputStream> streamsToSplitTo;

    public OutputStreamSplitter(OutputStream... jis) {
        streamsToSplitTo = Arrays.asList(jis);
    }

    @Override
    public void write(int i) throws IOException {
        for (OutputStream os : streamsToSplitTo) {
            os.write(i);
        }
    }

    @Override
    public void close() throws IOException {
        for (OutputStream os : streamsToSplitTo) {
            os.close();
        }
    }

    @Override
    public void flush() throws IOException {
        for (OutputStream os : streamsToSplitTo) {
            os.flush();
        }
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        for (OutputStream os : streamsToSplitTo) {
            os.write(bytes);
        }
    }

    @Override
    public void write(byte[] bytes, int i, int i1) throws IOException {
        for (OutputStream os : streamsToSplitTo) {
            os.write(bytes, i, i1);
        }
    }
}
