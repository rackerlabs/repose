package org.openrepose.rnxp.valve;

import org.jboss.netty.channel.Channel;

/**
 *
 * @author zinic
 */
public class AbstractChannelValve {

    protected final Channel channel;

    public AbstractChannelValve(Channel channel) {
        this.channel = channel;
    }
}
