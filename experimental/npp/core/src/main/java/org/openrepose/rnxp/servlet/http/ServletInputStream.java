package org.openrepose.rnxp.servlet.http;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author zinic
 */
public class ServletInputStream extends javax.servlet.ServletInputStream {

    private final InputStream inputStream;

    public ServletInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public int read() throws IOException {
        return inputStream.read();
    }
}
