package org.openrepose.rnxp.http;

import org.jboss.netty.channel.Channel;
import org.openrepose.rnxp.http.context.RequestContext;
import org.openrepose.rnxp.http.context.SimpleRequestContext;
import org.openrepose.rnxp.http.io.control.BlockingUpdateController;
import org.jboss.netty.channel.ChannelStateEvent;
import org.openrepose.rnxp.PowerProxy;
import org.openrepose.rnxp.servlet.http.LiveHttpServletRequest;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;
import org.openrepose.rnxp.http.io.control.HttpMessageUpdateController;
import org.openrepose.rnxp.http.proxy.ConnectionFuture;
import org.openrepose.rnxp.http.proxy.InboundOutboundCoordinator;
import org.openrepose.rnxp.http.proxy.OriginChannelFactory;
import org.openrepose.rnxp.servlet.http.LiveHttpServletResponse;
import org.openrepose.rnxp.netty.valve.ChannelReadValve;
import org.openrepose.rnxp.netty.valve.ChannelValve;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zinic
 */
public class HttpRequestHandler extends SimpleChannelUpstreamHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HttpRequestHandler.class);
    
    private final RequestContext requestContext;
    private final OriginChannelFactory proxyChannelFactory;
    private ConnectionFuture proxyConnectionFuture;
    private InboundOutboundCoordinator coordinator;
    private ChannelValve inboundReadValve;
    private HttpMessageUpdateController updateController;

    public HttpRequestHandler(PowerProxy powerProxyInstance, OriginChannelFactory proxyChannelFactory) {
        this.proxyChannelFactory = proxyChannelFactory;
        requestContext = new SimpleRequestContext(powerProxyInstance);
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        final Channel channel = e.getChannel();

        // Set up the read valve and shut the channel for now
        inboundReadValve = new ChannelReadValve(channel, ChannelReadValve.VALVE_OPEN);
        inboundReadValve.shut();

        coordinator = new InboundOutboundCoordinator();
        coordinator.setInboundChannel(channel);

        proxyConnectionFuture = new ConnectionFuture() {

            @Override
            public void connected(Channel channel) {
                coordinator.setOutboundChannel(channel);
            }
        };

        // Set up our update controller for Request < -- > Channel communication
        updateController = new BlockingUpdateController(inboundReadValve);

        // Build the request object and make it aware of the update controller
        final LiveHttpServletRequest liveHttpServletRequest = new LiveHttpServletRequest();
        liveHttpServletRequest.setUpdateController(updateController);

        // Let's kick off the worker thread
        requestContext.startRequest(liveHttpServletRequest, new LiveHttpServletResponse());
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
