package org.openrepose.rnxp.valve;

import org.jboss.netty.channel.Channel;

/**
 *
 * @author zinic
 */
public class ChannelReadValve extends AbstractChannelValve implements ChannelValve {

    public static final boolean VALVE_OPEN = Boolean.TRUE, VALVE_SHUT = Boolean.FALSE;
    
    private boolean valveState;

    public ChannelReadValve(Channel channel, boolean valveState) {
        super(channel);

        this.valveState = valveState;
    }

    @Override
    public boolean isOpen() {
        return valveState == VALVE_OPEN;
    }

    @Override
    public synchronized void shut() {
        if (valveState != VALVE_OPEN) {
            throw new IllegalArgumentException();
        }

        valveState = VALVE_SHUT;
        updateChannel();
    }

    @Override
    public synchronized void open() {
        if (valveState != VALVE_SHUT) {
            throw new IllegalArgumentException();
        }

        valveState = VALVE_OPEN;
        updateChannel();
    }

    private void updateChannel() {
        channel.setReadable(valveState);
    }
}
