package org.openrepose.rnxp.http;

import org.openrepose.rnxp.io.push.ChannelEventListener;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.openrepose.rnxp.http.context.RequestContext;
import org.openrepose.rnxp.http.io.control.BlockingConnectionController;
import org.openrepose.rnxp.PowerProxy;
import org.openrepose.rnxp.decoder.HttpRequestDecoder;
import org.openrepose.rnxp.http.context.SimpleRequestContext;
import org.openrepose.rnxp.http.io.control.HttpConnectionController;
import org.openrepose.rnxp.http.proxy.OriginPipelineFactory;
import org.openrepose.rnxp.http.proxy.NettyOriginConnectionFuture;
import org.openrepose.rnxp.http.proxy.OutboundCoordinator;
import org.openrepose.rnxp.http.proxy.OriginChannelFactory;
import org.openrepose.rnxp.http.proxy.OriginConnectionFuture;
import org.openrepose.rnxp.pipe.MessagePipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zinic
 */
public class HttpRequestHandler implements ChannelEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(HttpRequestHandler.class);
    
    private final OutboundCoordinator coordinator;
    private final OriginChannelFactory proxyChannelFactory;
    private final RequestContext requestContext;

    public HttpRequestHandler(PowerProxy powerProxyInstance, OutboundCoordinator coordinator, OriginChannelFactory proxyChannelFactory) {
        requestContext = new SimpleRequestContext(powerProxyInstance, coordinator);

        this.proxyChannelFactory = proxyChannelFactory;
        this.coordinator = coordinator;
    }

    @Override
    public void channelOpen(Channel channel, MessagePipe<ChannelBuffer> messagePipe) {
        // Set up the origin connection future for initiating conversations with the origin server
        final OriginConnectionFuture originConnectionFuture = new NettyOriginConnectionFuture(
                new OriginPipelineFactory(requestContext), proxyChannelFactory);

        // Set up our update controller for Request < -- > Channel communication
        final HttpConnectionController updateController = new BlockingConnectionController(coordinator, messagePipe, new HttpRequestDecoder());

        // Update the coordinator with our channel
        coordinator.setClientChannel(channel);
        
        // Let's kick off the worker thread
        requestContext.startRequest(updateController, originConnectionFuture);
    }

    @Override
    public void exception(Throwable cause) {
        LOG.error("Connection exception caught, aborting worker. Reason: " + cause.getMessage(), cause);

        requestContext.conversationAborted();
    }
}
