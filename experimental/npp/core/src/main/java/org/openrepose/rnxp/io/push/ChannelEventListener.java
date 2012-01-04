package org.openrepose.rnxp.io.push;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.openrepose.rnxp.pipe.MessagePipe;

/**
 *
 * @author zinic
 */
public interface ChannelEventListener {

    void channelOpen(Channel channel, MessagePipe<ChannelBuffer> messagePipe);
    
    void exception(Throwable cause);
}
