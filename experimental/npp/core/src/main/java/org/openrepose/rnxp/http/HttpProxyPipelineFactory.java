package org.openrepose.rnxp.http;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.openrepose.rnxp.PowerProxy;
import org.openrepose.rnxp.http.proxy.OriginChannelFactory;

/**
 *
 * @author zinic
 */
public class HttpProxyPipelineFactory implements ChannelPipelineFactory {

    private final PowerProxy powerProxy;
    private final OriginChannelFactory proxyRemoteFactory;

    public HttpProxyPipelineFactory(PowerProxy powerProxy, OriginChannelFactory proxyRemoteFactory) {
        this.powerProxy = powerProxy;
        this.proxyRemoteFactory = proxyRemoteFactory;
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        return Channels.pipeline(
                new HttpRequestDecoder(),
                new HttpRequestHandler(powerProxy, proxyRemoteFactory));
    }
}
