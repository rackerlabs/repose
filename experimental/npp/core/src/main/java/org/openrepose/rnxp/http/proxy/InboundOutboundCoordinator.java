package org.openrepose.rnxp.http.proxy;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;

/**
 *
 * @author zinic
 */
public class InboundOutboundCoordinator {

    private ChannelUpdater inboundUpdate, outboundUpdate;
    private Channel inboundChannel, outboundChannel;

    public InboundOutboundCoordinator() {
    }
    
    public synchronized boolean streamable() {
        return inboundChannel != null && outboundChannel != null;
    }

    public synchronized void setInboundChannel(Channel inboundChannel) {
        this.inboundChannel = inboundChannel;
        outboundUpdate = new ChannelUpdater(inboundChannel);
    }

    public synchronized void setOutboundChannel(Channel outboundChannel) {
        this.outboundChannel = outboundChannel;
        inboundUpdate = new ChannelUpdater(outboundChannel);
    }

    public void writeOutbound(Object o) {
        write(outboundChannel, inboundChannel, outboundUpdate, o);
    }

    public void writeInbound(Object o) {
        write(inboundChannel, outboundChannel, inboundUpdate, o);
    }

    private void write(Channel destination, Channel source, ChannelUpdater updateMe, Object o) {
        final ChannelFuture future = destination.write(o);

        if (destination.isWritable()) {
            source.setReadable(false);
            future.addListener(updateMe);
        }
    }
}
