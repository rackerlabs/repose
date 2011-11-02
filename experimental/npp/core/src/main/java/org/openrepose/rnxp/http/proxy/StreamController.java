package org.openrepose.rnxp.http.proxy;

/**
 *
 * @author zinic
 */
public interface StreamController {

    boolean isStreaming();

    void stream();
}