package org.openrepose.rnxp.valve;

/**
 *
 * @author zinic
 */
public interface ChannelValve {

    boolean isOpen();
    
    void open();

    void shut();
}
