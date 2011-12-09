package org.openrepose.rnxp.http.proxy;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.openrepose.rnxp.servlet.http.detached.HttpErrorSerializer;

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
        
        if (inboundChannel != outboundChannel) {
            outboundChannel.close();
        }
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

    public synchronized ChannelFuture writeInbound(HttpErrorSerializer error) {
        return error.writeTo(inboundChannel);
    }

    public synchronized ChannelFuture writeOutbound(ChannelBuffer buffer) {
        return outboundChannel.write(buffer);
    }
    
    public synchronized ChannelFuture writeInbound(ChannelBuffer buffer) {
        return inboundChannel.write(buffer);
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
