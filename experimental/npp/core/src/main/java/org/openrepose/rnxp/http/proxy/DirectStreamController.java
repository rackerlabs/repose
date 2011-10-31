package org.openrepose.rnxp.http.proxy;

import java.net.InetSocketAddress;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jboss.netty.channel.ChannelPipelineFactory;

/**
 *
 * @author zinic
 */
public class DirectStreamController implements StreamController {

    private final ChannelPipelineFactory channelPipelineFacotry;
    private final OriginChannelFactory channelFactory;

    /**
     * 
     * @param channelPipelineFacotry Netty pipeline factory to use when connection to the origin
     * @param channelFactory Connection channel factory. This object is responsible for opening up http conduits to the origin
     */
    public DirectStreamController(ChannelPipelineFactory channelPipelineFacotry, OriginChannelFactory channelFactory) {
        this.channelPipelineFacotry = channelPipelineFacotry;
        this.channelFactory = channelFactory;
    }

    @Override
    public void commitRequest(HttpServletRequest request) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void commitResponse(HttpServletResponse response) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void engageRemote(InetSocketAddress addr) {
        channelFactory.connect(addr, channelPipelineFacotry);
    }
}
