package org.openrepose.rnxp.servlet.http.detached;

import java.util.Iterator;
import org.jboss.netty.buffer.ChannelBuffer;
import org.openrepose.rnxp.http.HttpMessageComponent;
import org.openrepose.rnxp.http.io.control.HttpMessageSerializer;

import static org.jboss.netty.buffer.ChannelBuffers.*;

/**
 *
 * @author zinic
 */
public class DetachedResponseSerializer implements HttpMessageSerializer {

    private final DetachedHttpServletResponse response;
    private final ChannelBuffer buffer;
    private byte[] currentHeaderKeyBytes;
    private Iterator<String> headerKeysRemaining;
    private Iterator<String> headerValuesRemaining;
    private HttpMessageComponent nextComponent;

    public DetachedResponseSerializer(DetachedHttpServletResponse response) {
        this.response = response;

        nextComponent = HttpMessageComponent.HTTP_VERSION;
        buffer = buffer(16384);
    }

    @Override
    public int read() {
        try {
            if (!buffer.readable()) {
                if (loadNextComponent()) {
                    return -1;
                }
            }

            return buffer.readByte();
        } catch (Exception ex) {
            ex.printStackTrace();
            return -1;
        }
    }

    public boolean loadNextComponent() {
        switch (nextComponent) {
            case HTTP_VERSION:
                buffer.writeBytes("HTTP/1.1 ".getBytes());
                nextComponent = HttpMessageComponent.RESPONSE_STATUS_CODE;

                break;

            case RESPONSE_STATUS_CODE:
                buffer.writeBytes((response.getStatus() + " \r\n").getBytes());

                nextComponent = HttpMessageComponent.HEADER;
                break;

            case HEADER:
                if (headerKeysRemaining == null) {
                    headerKeysRemaining = response.getHeaderNames().iterator();
                }

                if (headerValuesRemaining == null || !headerValuesRemaining.hasNext()) {
                    if (headerKeysRemaining.hasNext()) {
                        final String headerKey = headerKeysRemaining.next();
                        currentHeaderKeyBytes = headerKey.getBytes();

                        headerValuesRemaining = response.getHeaders(headerKey).iterator();
                    } else {
                        return true;
                    }
                }

                if (headerValuesRemaining.hasNext()) {
                    final String nextVal = headerValuesRemaining.next();

                    buffer.writeBytes(currentHeaderKeyBytes);
                    buffer.writeBytes(":".getBytes());
                    buffer.writeBytes(nextVal == null ? new byte[0] : nextVal.getBytes());
                    buffer.writeBytes("\r\n".getBytes());
                }

                break;
        }

        return false;
    }
}
