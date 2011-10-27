package org.openrepose.rnxp.netty.valve;

/**
 *
 * @author zinic
 */
public interface ChannelValve {

    boolean isOpen();
    
    void open();

    void shut();
}
