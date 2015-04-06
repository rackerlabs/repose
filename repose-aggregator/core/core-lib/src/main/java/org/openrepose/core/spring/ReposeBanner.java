/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
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
     *
     * @param log
     */
    public static void print(Logger log) {
        try {
            final InputStream bannerInputStream = ReposeBanner.class.getResourceAsStream("/repose.banner");
            Scanner s = new Scanner(bannerInputStream).useDelimiter("\\A");

            if (s.hasNext()) {
                log.info("\n" + s.next());
            } else {
                log.warn("Unable to find the beautiful narwhal banner. This is a bad thing.");
            }
        } catch (Exception e) {
            LOG.warn("NON-FATAL - Unable to log the beautiful narwhal banner. Sad day.", e);
        }
    }
}
