package org.openrepose.rnxp.logging;

import org.slf4j.Logger;

/**
 *
 * @author zinic
 */
public class ThreadStamp {
    public static void outputThreadStamp(Logger log, String message) {
        log.info(System.nanoTime() + "@" + Thread.currentThread() + " :: " + message);
    }
}
