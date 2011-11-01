package org.openrepose.rnxp.http.io.control;

import java.io.IOException;
import java.io.OutputStream;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.openrepose.rnxp.http.proxy.InboundOutboundCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zinic
 */
public class BlockingOutputStream extends OutputStream {
    
    private static final Logger LOG = LoggerFactory.getLogger(BlockingOutputStream.class);

    private final InboundOutboundCoordinator coordinator;
    private final ChannelBuffer writeBuffer;

    public BlockingOutputStream(InboundOutboundCoordinator coordinator) {
        this.coordinator = coordinator;
        
        writeBuffer = ChannelBuffers.buffer(512);
    }

    @Override
    public void flush() throws IOException {
        LOG.info("Flushing: " + writeBuffer.readableBytes());
        
        try {
            coordinator.write(writeBuffer);
        } catch (InterruptedException ie) {
            throw new IOException("Write thread interrupted", ie);
        }
    }

    @Override
    public void write(int i) throws IOException {
        if (writeBuffer.writable()) {
            writeBuffer.writeByte(i);
        } else {
            flush();
        }
    }
}
