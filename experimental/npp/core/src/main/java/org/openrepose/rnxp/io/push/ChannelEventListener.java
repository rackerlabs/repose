package org.openrepose.rnxp.io.push;

import org.jboss.netty.buffer.ChannelBuffer;
import org.openrepose.rnxp.http.proxy.InboundOutboundCoordinator;
import org.openrepose.rnxp.pipe.MessagePipe;

/**
 *
 * @author zinic
 */
public interface ChannelEventListener {

    void channelOpen(MessagePipe<ChannelBuffer> messagePipe, InboundOutboundCoordinator coordinator);
    
    void exception(Throwable cause);
}
