package org.openrepose.rnxp.http.io.netty;

import java.io.IOException;
import java.io.OutputStream;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.openrepose.rnxp.http.proxy.InboundOutboundCoordinator;
import org.openrepose.rnxp.logging.ThreadStamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zinic
 */
public class ChannelOutputStream extends OutputStream {
    
    private static final Logger LOG = LoggerFactory.getLogger(ChannelOutputStream.class);
    private final InboundOutboundCoordinator coordinator;
    private final ChannelBuffer writeBuffer;
    
    public ChannelOutputStream(InboundOutboundCoordinator coordinator) {
        this.coordinator = coordinator;
        
        writeBuffer = ChannelBuffers.buffer(512);
    }
    
    @Override
    public void flush() throws IOException {
        ThreadStamp.log(LOG, "Flushing: " + writeBuffer.readableBytes());
        
        try {
            coordinator.writeOutbound(writeBuffer).await();
            writeBuffer.clear();
        } catch (InterruptedException ie) {
            LOG.error("Interrupted while flushing data to outbound channel", ie);
        }
    }
    
    @Override
    public void write(int i) throws IOException {
        if (!writeBuffer.writable()) {
            flush();
        }
        
        writeBuffer.writeByte(i);
    }
}
