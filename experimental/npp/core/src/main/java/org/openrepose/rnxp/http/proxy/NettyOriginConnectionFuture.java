package org.openrepose.rnxp.http.proxy;

import java.net.InetSocketAddress;
import org.jboss.netty.channel.ChannelPipelineFactory;

/**
 *
 * @author zinic
 */
public class NettyOriginConnectionFuture implements OriginConnectionFuture {

    private final ChannelPipelineFactory channelPipelineFactory;
    private final OriginChannelFactory channelFactory;

    /**
     * 
     * @param channelPipelineFactory Netty pipeline factory to use when connection to the origin
     * @param channelFactory Connection channel factory. This object is responsible for opening up http conduits to the origin
     */
    public NettyOriginConnectionFuture(ChannelPipelineFactory channelPipelineFactory, OriginChannelFactory channelFactory) {
        this.channelPipelineFactory = channelPipelineFactory;
        this.channelFactory = channelFactory;
    }

    @Override
    public void connect(InetSocketAddress addr) {
        channelFactory.connect(addr, channelPipelineFactory);
    }
}
