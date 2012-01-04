package org.openrepose.rnxp.http.proxy;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.openrepose.rnxp.http.HttpResponseHandler;
import org.openrepose.rnxp.http.context.RequestContext;
import org.openrepose.rnxp.io.push.PushChannelUpstreamHandler;

public class ClientPipelineFactory implements ChannelPipelineFactory {

    private final RequestContext requestContext;

    public ClientPipelineFactory(RequestContext requestContext) {
        this.requestContext = requestContext;
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        final HttpResponseHandler eventHandler = new HttpResponseHandler(requestContext);
        final PushChannelUpstreamHandler pushChannelUpstreamHandler = new PushChannelUpstreamHandler(eventHandler);
        
        return Channels.pipeline(pushChannelUpstreamHandler);
    }
}
