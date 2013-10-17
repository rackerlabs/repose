package com.rackspace.papi.commons.util.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author zinic
 */
public abstract class OneTimeUseOutputStream extends OutputStream {

    private volatile boolean closed = false;

    private void checkForClosedStream() throws IOException {
        if (closed) {
            throw new IOException("InputStream has been closed. Further operations are prohibited");
        }
    }

    @Override
    public final void close() throws IOException {
        checkForClosedStream();

        closeStream();

        closed = true;
    }

    @Override
    public final void flush() throws IOException {
        checkForClosedStream();

        flushStream();
    }

    @Override
    public final void write(int b) throws IOException {
        checkForClosedStream();

        writeByte(b);
    }

    @Override
    public final void write(byte[] b) throws IOException {
        checkForClosedStream();

        super.write(b);
    }

    @Override
    public final void write(byte[] b, int off, int len) throws IOException {
        checkForClosedStream();

        super.write(b, off, len);
    }

    protected abstract void closeStream() throws IOException;

    protected abstract void flushStream() throws IOException;

    protected abstract void writeByte(int b) throws IOException;
}
