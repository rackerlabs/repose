package org.openrepose.rnxp.http.proxy;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;

/**
 *
 * @author zinic
 */
public class InboundOutboundCoordinator {

    private StreamController inboundStream, outboundStream;
    private ChannelUpdater inboundUpdate, outboundUpdate;
    private Channel inboundChannel, outboundChannel;

    public InboundOutboundCoordinator() {
    }

    public synchronized boolean streamable() {
        return inboundChannel != null && outboundChannel != null;
    }

    public synchronized void setInboundChannel(Channel channel, StreamController stl) {
        inboundChannel = channel;
        inboundStream = stl;

        outboundUpdate = new ChannelUpdater(channel);
    }

    public synchronized void setOutboundChannel(Channel channel, StreamController stl) {
        outboundChannel = channel;
        outboundStream = stl;

        inboundUpdate = new ChannelUpdater(channel);
    }

    public void streamInbound() {
        setStream(inboundStream);
    }

    public void streamOutbound() {
        setStream(outboundStream);
    }

    private synchronized void setStream(StreamController stl) {
        if (!stl.isStreaming()) {
            stl.stream();
        }
    }

    public void writeOutbound(Object o) {
        write(outboundChannel, inboundChannel, outboundUpdate, o);
    }

    public void writeInbound(Object o) {
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
