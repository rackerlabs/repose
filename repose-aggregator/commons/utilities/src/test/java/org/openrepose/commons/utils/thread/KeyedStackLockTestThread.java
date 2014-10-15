package org.openrepose.commons.utils.thread;

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
