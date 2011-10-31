package org.openrepose.rnxp.http.proxy;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

/**
 *
 * @author zinic
 */
public class ChannelUpdater implements ChannelFutureListener {

    private final Channel channel;

    public ChannelUpdater(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void operationComplete(ChannelFuture cf) throws Exception {
        channel.setReadable(true);
    }
}
