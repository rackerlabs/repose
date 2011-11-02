package org.openrepose.rnxp.http.proxy;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

/**
 *
 * @author zinic
 */
public class InboundOutboundCoordinator {

    private final ChannelFutureListener writeNotifier;
    private StreamController inboundStream, outboundStream;
    private ChannelUpdater inboundUpdate, outboundUpdate;
    private Channel inboundChannel, outboundChannel;

    public InboundOutboundCoordinator() {
        writeNotifier = new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture cf) throws Exception {
                notifyWriter();
            }
        };
    }

    public synchronized void close() {
        inboundChannel.close();
        outboundChannel.close();
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

    public synchronized void write(ChannelBuffer buffer) throws InterruptedException {
        outboundChannel.write(buffer).addListener(writeNotifier);

        wait();
    }

    private synchronized void notifyWriter() {
        notify();
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
