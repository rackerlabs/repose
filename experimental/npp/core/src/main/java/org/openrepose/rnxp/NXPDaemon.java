package org.openrepose.rnxp;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.openrepose.rnxp.netty.HttpProxyPipelineFactory;
import org.openrepose.rnxp.http.proxy.OriginChannelFactory;

/**
 * RNGP
 * Repose Next Generation Proxy
 * 
 * @author zinic
 */
public class NXPDaemon {

    // This isn't so much a daemon as it is just a crap bucket to run this thing while I work on it
    public static void main(String[] args) {
        final PowerProxy powerProxyInstance = new PowerProxy();
        powerProxyInstance.init();
        
        final OriginChannelFactory proxyChannelFactory = new OriginChannelFactory();

        final ChannelFactory factory = new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool());

        final ServerBootstrap bootstrap = new ServerBootstrap(factory);

        bootstrap.setPipelineFactory(new HttpProxyPipelineFactory(powerProxyInstance, proxyChannelFactory));
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);

        bootstrap.bind(new InetSocketAddress(8081));
    }
}
