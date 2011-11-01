package org.openrepose.rnxp.http.proxy;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.openrepose.rnxp.decoder.HttpResponseDecoder;
import org.openrepose.rnxp.http.HttpResponseHandler;
import org.openrepose.rnxp.http.context.RequestContext;

public class ClientPipelineFactory implements ChannelPipelineFactory {

    private final InboundOutboundCoordinator coordinator;
    private final RequestContext requestContext;

    public ClientPipelineFactory(RequestContext requestContext, InboundOutboundCoordinator coordinator) {
        this.requestContext = requestContext;
        this.coordinator = coordinator;
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        final HttpResponseDecoder decoder = new HttpResponseDecoder();
        
        return Channels.pipeline(
                decoder,
                new HttpResponseHandler(requestContext, coordinator, decoder.getStreamController()));
    }
}
