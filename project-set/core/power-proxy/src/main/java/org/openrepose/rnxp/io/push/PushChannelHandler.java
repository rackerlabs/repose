package org.openrepose.rnxp.io.push;

import org.jboss.netty.buffer.ChannelBuffer;
import org.openrepose.rnxp.pipe.MessagePipe;

/**
 *
 * @author zinic
 */
public interface PushChannelHandler {

    MessagePipe<ChannelBuffer> getMessagePipe();
}
