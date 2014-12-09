package org.openrepose.core.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Scanner;

/**
 * TODO this should go in some utilities area, but it is used by the spring context when firing up.
 */
public final class ReposeBanner {

    private static final Logger LOG = LoggerFactory.getLogger(ReposeBanner.class);

    private ReposeBanner() {
    }

    /**
     * Print the banner to a log.
     * http://stackoverflow.com/a/5445161/423218 -- using this handy method
     * @param log
     */
    public static void print(Logger log) {
        try {
            final InputStream bannerInputStream = ReposeBanner.class.getResourceAsStream("/repose.banner");
            Scanner s = new Scanner(bannerInputStream).useDelimiter("\\A");

            if(s.hasNext()) {
                log.info("\n" + s);
            } else {
                log.warn("Unable to find the beautiful narwhal banner. This is a bad thing.");
            }
        } catch (Exception e) {
            LOG.warn("NON-FATAL - Unable to log the beautiful narwhal banner. Sad day.", e);
        }
    }
}
