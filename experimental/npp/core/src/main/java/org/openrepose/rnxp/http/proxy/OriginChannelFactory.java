/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openrepose.rnxp.http.proxy;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

/**
 *
 * @author zinic
 */
public class OriginChannelFactory {

    private final ClientSocketChannelFactory cf;

    public OriginChannelFactory() {
        cf = new NioClientSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool());
    }
    
    public void connect(InetSocketAddress addr, ClientPipelineFactory pipelineFactory) {
        final ClientBootstrap clientBootstrap = new ClientBootstrap(cf);
        clientBootstrap.setPipelineFactory(pipelineFactory);
        clientBootstrap.connect(addr);
    }
}
