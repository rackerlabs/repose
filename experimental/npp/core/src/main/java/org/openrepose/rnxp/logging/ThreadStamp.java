package org.openrepose.rnxp.logging;

import org.slf4j.Logger;

/**
 *
 * @author zinic
 */
public final class ThreadStamp {

    private ThreadStamp() {
    }

    public static void log(Logger log, String message) {
        if (log.isDebugEnabled()) {
            log.debug(System.nanoTime() + "@" + Thread.currentThread() + " :: " + message);
        }
    }
}
