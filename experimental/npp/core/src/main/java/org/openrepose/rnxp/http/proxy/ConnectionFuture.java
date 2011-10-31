/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openrepose.rnxp.http.proxy;

import org.jboss.netty.channel.Channel;

/**
 *
 * @author zinic
 */
public interface ConnectionFuture {

    void connected(Channel channel);
    
}
