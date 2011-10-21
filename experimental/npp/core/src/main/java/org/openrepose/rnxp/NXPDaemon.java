package org.openrepose.rnxp;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.openrepose.rnxp.http.HttpRequestHandler;
import org.openrepose.rnxp.decoder.HttpDecoder;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

/**
 *
 * @author zinic
 */
public class NXPDaemon {

    public static void main(String[] args) {
        final PowerProxy powerProxyInstance = new PowerProxy();
        powerProxyInstance.init();

        final ChannelFactory factory = new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool());

        final ServerBootstrap bootstrap = new ServerBootstrap(factory);

        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

            @Override
            public ChannelPipeline getPipeline() {
                return Channels.pipeline(
                        new HttpDecoder(),
                        new HttpRequestHandler(powerProxyInstance));
            }
        });

        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);

        bootstrap.bind(new InetSocketAddress(8080));
    }
}
