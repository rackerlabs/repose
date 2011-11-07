package org.openrepose.rnxp.io.push;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.openrepose.rnxp.logging.ThreadStamp;
import org.openrepose.rnxp.http.proxy.InboundOutboundCoordinator;
import org.openrepose.rnxp.pipe.BlockingMessagePipe;
import org.openrepose.rnxp.pipe.MessagePipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushChannelUpstreamHandler extends SimpleChannelUpstreamHandler implements PushChannelHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PushChannelUpstreamHandler.class);
    private final ChannelEventListener channelEventListener;
    private final BlockingMessagePipe<ChannelBuffer> messagePipe;
    private PushController channelPushController;

    public PushChannelUpstreamHandler(ChannelEventListener channelEventListener) {
        this.channelEventListener = channelEventListener;
        
        messagePipe = new BlockingMessagePipe<ChannelBuffer>();
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        final ChannelBuffer input = (ChannelBuffer) e.getMessage();

        if (input.readable()) {
            channelPushController.stopMessageFlow();
            messagePipe.pushMessage(input);
        }
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        final Channel channel = e.getChannel();
        
        channelPushController = new SimplePushController(channel);
        messagePipe.setPushController(channelPushController);
        
        final InboundOutboundCoordinator coordinator = new InboundOutboundCoordinator();
        coordinator.setInboundChannel(channel);
        coordinator.setOutboundChannel(channel);
        
        channelEventListener.channelOpen(messagePipe, coordinator);
    }

    @Override
    public void channelInterestChanged(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        ThreadStamp.outputThreadStamp(LOG, "Channel interest changed. Interest: " + e.getChannel().getInterestOps());
    }

    @Override
    public MessagePipe<ChannelBuffer> getMessagePipe() {
        return messagePipe;
    }
}
