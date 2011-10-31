package org.openrepose.rnxp.http.proxy;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.openrepose.rnxp.http.HttpResponseHandler;
import org.openrepose.rnxp.http.context.RequestContext;

public class ClientPipelineFactory implements ChannelPipelineFactory {

    private final RequestContext requestContext;
    private final ConnectionFuture proxyConnectionFuture;

    public ClientPipelineFactory(RequestContext requestContext, ConnectionFuture proxyConnectionFuture) {
        this.requestContext = requestContext;
        this.proxyConnectionFuture = proxyConnectionFuture;
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        final ChannelPipeline pipeline = Channels.pipeline(
                new HttpResponseDecoder(),
                new HttpResponseHandler(requestContext, proxyConnectionFuture));

        return pipeline;
    }
}
