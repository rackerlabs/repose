package org.openrepose.rnxp.http;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.openrepose.rnxp.decoder.partial.ContentMessagePartial;
import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;
import org.openrepose.rnxp.http.context.RequestContext;
import org.openrepose.rnxp.http.io.control.BlockingConnectionController;
import org.openrepose.rnxp.http.io.control.HttpConnectionController;
import org.openrepose.rnxp.http.proxy.InboundOutboundCoordinator;
import org.openrepose.rnxp.http.proxy.StreamController;
import org.openrepose.rnxp.netty.valve.ChannelReadValve;
import org.openrepose.rnxp.netty.valve.ChannelValve;
import org.openrepose.rnxp.servlet.http.live.LiveHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zinic
 */
public class HttpResponseHandler extends SimpleChannelUpstreamHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HttpResponseHandler.class);
    private final RequestContext requestContext;
    private final InboundOutboundCoordinator coordinator;
    private final StreamController streamController;
    private HttpConnectionController updateController;
    private ChannelValve readValve;

    public HttpResponseHandler(RequestContext requestContext, InboundOutboundCoordinator coordinator, StreamController streamController) {
        this.requestContext = requestContext;
        this.coordinator = coordinator;
        this.streamController = streamController;
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        final Channel channel = e.getChannel();

        // Set up the read valve and shut the channel for now
        readValve = new ChannelReadValve(channel, ChannelReadValve.VALVE_OPEN);
        readValve.shut();

        // Let the coordinator know we're here
        coordinator.setOutboundChannel(channel, streamController);

        // Set up our update controller for Response < -- > Channel communication
        updateController = new BlockingConnectionController(coordinator, readValve);

        // Build the request object and make it aware of the update controller
        final LiveHttpServletResponse liveHttpServletResponse = new LiveHttpServletResponse(updateController);

        // Let's swap responses and kick off the servlet worker again
        requestContext.responseConnected(liveHttpServletResponse);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        final HttpMessagePartial partial = (HttpMessagePartial) e.getMessage();

        if (HttpMessageComponent.UNPARSED_STREAMABLE == partial.getHttpMessageComponent()) {
            coordinator.streamInbound(((ContentMessagePartial) partial).getData());
        }

        if (!partial.isError()) {
            // Apply the partial
            updateController.applyPartial(partial);
        } else {
            // TODO:Implement - Write error output functionality here
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        final Throwable cause = e.getCause();

        LOG.error(cause.getMessage(), cause);
        e.getChannel().close();
    }
}
