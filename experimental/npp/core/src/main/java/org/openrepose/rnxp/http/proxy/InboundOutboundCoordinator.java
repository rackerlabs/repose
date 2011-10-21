package org.openrepose.rnxp.http.proxy;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelStateEvent;

/**
 *
 * @author zinic
 */
public class InboundOutboundCoordinator {

    private Channel inboundChannel;
    private Channel outboundChannel;

    public InboundOutboundCoordinator() {
    }

    public void setInboundChannel(Channel inboundChannel) {
        this.inboundChannel = inboundChannel;
    }

    public void setOutboundChannel(Channel outboundChannel) {
        this.outboundChannel = outboundChannel;
    }
    
    public void inboundChannelInterestChanged(ChannelStateEvent e) {
        
    }
    
    public void outboundChannelInterestChanged(ChannelStateEvent e) {
        
    }

    public void writeOutbound(Object o) {
        outboundChannel.write(o);
    }

    public void writeInbound(Object o) {
        inboundChannel.write(o);
    }
}
