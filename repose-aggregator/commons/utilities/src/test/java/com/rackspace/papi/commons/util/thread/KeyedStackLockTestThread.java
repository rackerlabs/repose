package com.rackspace.papi.commons.util.thread;

/**
 *
 * 
 */
public interface KeyedStackLockTestThread {

    void exec();

    void kill();

    boolean passed();

    boolean finished();

    boolean started();
}
