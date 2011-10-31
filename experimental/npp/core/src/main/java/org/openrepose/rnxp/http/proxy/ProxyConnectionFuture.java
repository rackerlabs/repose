package org.openrepose.rnxp.http.proxy;

import org.jboss.netty.channel.Channel;

/**
 *
 * @author zinic
 */
public class ProxyConnectionFuture implements ConnectionFuture {

    final InboundOutboundCoordinator coordinator;

    public ProxyConnectionFuture(InboundOutboundCoordinator coordinator) {
        this.coordinator = coordinator;
    }
    
    @Override
    public void connected(Channel channel) {
        coordinator.setOutboundChannel(channel);
    }
}
