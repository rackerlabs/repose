package org.openrepose.rnxp.http.proxy;

import org.jboss.netty.buffer.ChannelBuffer;
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
    
    public synchronized void close() {
        inboundChannel.close();
        outboundChannel.close();
    }

    public synchronized boolean streamable() {
        return inboundChannel != null && outboundChannel != null;
    }

    public synchronized void setInboundChannel(Channel channel) {
        inboundChannel = channel;

        outboundUpdate = new ChannelUpdater(channel);
    }

    public synchronized void setOutboundChannel(Channel channel) {
        outboundChannel = channel;

        inboundUpdate = new ChannelUpdater(channel);
    }

    public synchronized ChannelFuture write(ChannelBuffer buffer) {
        return outboundChannel.write(buffer);
    }

    public void streamOutbound(Object o) {
        write(outboundChannel, inboundChannel, outboundUpdate, o);
    }

    public void streamInbound(Object o) {
        write(inboundChannel, outboundChannel, inboundUpdate, o);
    }

    private synchronized void write(Channel destination, Channel source, ChannelUpdater updateMe, Object o) {
        final ChannelFuture future = destination.write(o);

        if (destination.isWritable()) {
            source.setReadable(false);
            future.addListener(updateMe);
        }
    }
}
