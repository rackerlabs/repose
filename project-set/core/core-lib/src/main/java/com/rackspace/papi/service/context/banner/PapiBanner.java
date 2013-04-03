package com.rackspace.papi.service.context.banner;

import com.rackspace.papi.commons.util.io.RawInputStreamReader;
import com.rackspace.papi.commons.util.io.charset.CharacterSets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public final class PapiBanner {

    private static final Logger LOG = LoggerFactory.getLogger(PapiBanner.class);
    
    private PapiBanner() {
    }

    public static void print(Logger log) {
        try {
            final InputStream bannerInputStream = PapiBanner.class.getResourceAsStream("papi.banner");

            if (bannerInputStream != null) {
                final String bannerString = new String(RawInputStreamReader.instance().readFully(bannerInputStream),CharacterSets.UTF_8);
                log.info(bannerString);
            }
        } catch (IOException ioe) {
            LOG.warn("NON-FATAL - Failure in reading Papi the Narwhal's start banner. Reason: " + ioe.getMessage(), ioe);
        }
    }
}
