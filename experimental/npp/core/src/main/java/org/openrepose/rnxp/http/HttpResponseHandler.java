package org.openrepose.rnxp.http;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;
import org.openrepose.rnxp.http.context.SimpleRequestContext;
import org.openrepose.rnxp.http.io.control.BlockingUpdateController;
import org.openrepose.rnxp.http.io.control.HttpMessageUpdateController;
import org.openrepose.rnxp.http.proxy.ConnectionFuture;
import org.openrepose.rnxp.netty.valve.ChannelReadValve;
import org.openrepose.rnxp.netty.valve.ChannelValve;
import org.openrepose.rnxp.servlet.http.LiveHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zinic
 */
public class HttpResponseHandler extends SimpleChannelUpstreamHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HttpResponseHandler.class);
    
    private final SimpleRequestContext requestContext;
    private final ConnectionFuture proxyConnectionFuture;
    private HttpMessageUpdateController updateController;
    private ChannelValve readValve;

    public HttpResponseHandler(SimpleRequestContext requestContext, ConnectionFuture proxyConnectionFuture) {
        this.requestContext = requestContext;
        this.proxyConnectionFuture = proxyConnectionFuture;
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        final Channel channel = e.getChannel();

        // Set up the read valve and shut the channel for now
        readValve = new ChannelReadValve(channel, ChannelReadValve.VALVE_OPEN);
        readValve.shut();

        proxyConnectionFuture.connected(channel);

        // Set up our update controller for Response < -- > Channel communication
        updateController = new BlockingUpdateController(readValve);

        // Build the request object and make it aware of the update controller
        final LiveHttpServletResponse liveHttpServletResponse = new LiveHttpServletResponse();
        liveHttpServletResponse.setUpdateController(updateController);

        // Let's kick off the worker
        requestContext.responseConnected(liveHttpServletResponse);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        final HttpMessagePartial partial = (HttpMessagePartial) e.getMessage();

        if (HttpMessageComponent.CONTENT_START == partial.getHttpMessageComponent()) {
        }

        if (!partial.isError()) {
            // Apply the partial
            updateController.applyPartial(partial);
        }
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        final Throwable cause = e.getCause();
        
        LOG.error(cause.getMessage(), cause);
        e.getChannel().close();
    }
}
