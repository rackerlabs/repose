package org.openrepose.rnxp.servlet.http;

import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author zinic
 */
public class ServletOutputStream extends javax.servlet.ServletOutputStream {

    private final OutputStream outputStream;

    public ServletOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public void write(int i) throws IOException {
        outputStream.write(i);
    }
}
