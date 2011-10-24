package org.openrepose.rnxp.http;

import org.openrepose.rnxp.PowerProxy;
import org.openrepose.rnxp.http.domain.HttpMessageComponent;
import org.openrepose.rnxp.http.domain.HttpPartial;
import org.openrepose.rnxp.http.proxy.InboundOutboundCoordinator;
import org.openrepose.rnxp.servlet.http.LiveHttpServletRequest;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 *
 * @author zinic
 */
public class HttpRequestHandler extends SimpleChannelUpstreamHandler {

    private final InboundOutboundCoordinator coordinator;
    private final RequestContext requestContext;
    private final LiveHttpServletRequest request;

    public HttpRequestHandler(PowerProxy powerProxyInstance) {
        requestContext = new SimpleRequestContext(powerProxyInstance);
        coordinator = new InboundOutboundCoordinator();
        request = new LiveHttpServletRequest();
    }

//        final Channel inboundChannel = e.getChannel();
//        inboundChannel.setReadable(false);
//
//        ClientBootstrap cb = new ClientBootstrap(cf);
//        cb.getPipeline().addLast("handler", new OutboundHandler(e.getChannel()));
//        ChannelFuture f = cb.connect(new InetSocketAddress(remoteHost, remotePort));    
// e.getChannel().write(ChannelBuffers.copiedBuffer(
// "HTTP/1.1 200 OK\r\nX-NXP-HW: Hello world!\r\nContent-Length: 0\r\n\r\n", CharsetUtil.UTF_8)).addListener(ChannelFutureListener.CLOSE);

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        final HttpPartial partial = (HttpPartial) e.getMessage();

        if (partial.messageComponent() != HttpMessageComponent.CONTENT_START && partial.messageComponent() != HttpMessageComponent.CONTENT_END) {
            request.applyPartial(partial);
        }
        
        if (partial.messageComponent() == HttpMessageComponent.HTTP_VERSION && !requestContext.started()) {
            requestContext.startRequest(request);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        e.getCause().printStackTrace();
        e.getChannel().close();
    }
}
