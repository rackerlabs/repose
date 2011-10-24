package org.openrepose.rnxp.http.proxy;

import org.openrepose.rnxp.http.domain.HttpPartial;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zinic
 */
public class OutboundChannel extends SimpleChannelUpstreamHandler {

    private static final Logger LOG = LoggerFactory.getLogger(OutboundChannel.class);
    private final InboundOutboundCoordinator coordinator;

    public OutboundChannel(InboundOutboundCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        coordinator.setOutboundChannel(e.getChannel());
    }
    
    
    /**
     * The origin service is talking to us! Exciting!
     * 
     * @param ctx
     * @param e
     * @throws Exception 
     */
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (e.getMessage() instanceof ChannelBuffer) {
            final ChannelBuffer buffer = (ChannelBuffer) e.getMessage();
            
            coordinator.writeInbound(buffer);
        } else if (e.getMessage() instanceof HttpPartial) {
            
        } else {
            LOG.error("Proxy Outbound Handler unable to process message of type: " + e.getMessage().getClass());
        }
    }

    @Override
    public void channelInterestChanged(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        coordinator.outboundChannelInterestChanged(e);
    }
}
