package org.openrepose.rnxp.http.proxy;

import java.net.InetSocketAddress;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;

public class ProxyClientPipelineFactory implements ChannelPipelineFactory {

    private final ClientSocketChannelFactory cf;
    private final InetSocketAddress remoteHost;

    public ProxyClientPipelineFactory(ClientSocketChannelFactory cf, InetSocketAddress remoteHost) {
        this.cf = cf;
        this.remoteHost = remoteHost;
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        final ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast(null, null);

        return pipeline;
    }
}
