package org.openrepose.rnxp.http;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
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
import org.openrepose.rnxp.servlet.http.LiveHttpServletResponse;
import org.openrepose.rnxp.netty.valve.ChannelReadValve;
import org.openrepose.rnxp.netty.valve.ChannelValve;

/**
 *
 * @author zinic
 */
public class HttpRequestHandler extends SimpleChannelUpstreamHandler {

    private final RequestContext requestContext;
    private ChannelValve inboundReadValve;
            
    private HttpMessageUpdateController updateController;

    public HttpRequestHandler(PowerProxy powerProxyInstance) {
        requestContext = new SimpleRequestContext(powerProxyInstance);
    }

    //        final Channel inboundChannel = e.getChannel();
    //        inboundChannel.setReadable(false);
    //
    //        ClientBootstrap cb = new ClientBootstrap(cf);
    //        cb.getPipeline().addLast("handler", new OutboundHandler(e.getChannel()));
    //        ChannelFuture f = cb.connect(new InetSocketAddress(remoteHost, remotePort));    
    //        e.getChannel().write(ChannelBuffers.copiedBuffer("HTTP/1.1 200 OK\r\nX-NXP-HW: Hello world!\r\nContent-Length: 0\r\n\r\n", CharsetUtil.UTF_8)).addListener(ChannelFutureListener.CLOSE);
    
    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        // Set up the read valve and shut the channel for now
        inboundReadValve = new ChannelReadValve(ctx.getChannel(), ChannelReadValve.VALVE_OPEN);
        inboundReadValve.shut();

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
        e.getCause().printStackTrace();
        e.getChannel().close();
    }
}
