/*
 *  Copyright 2010 Rackspace.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package com.rackspace.papi.components.logging;

import com.rackspace.papi.commons.util.logging.apache.HttpLogFormatter;
import com.rackspace.papi.components.logging.util.SimpleLogger;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author jhopper
 */
public class HttpLoggerWrapper {
    private final HttpLogFormatter formatter;
    private final List<SimpleLogger> loggers;

    public HttpLoggerWrapper(HttpLogFormatter formatter) {
        this.formatter = formatter;

        loggers = new LinkedList<SimpleLogger>();
    }

    public HttpLogFormatter getFormatter() {
        return formatter;
    }

    public void addLogger(SimpleLogger logger) {
        loggers.add(logger);
    }

    public synchronized void destroy() {
        for (SimpleLogger l : loggers) {
            l.destroy();
        }

        loggers.clear();
    }

    public synchronized void handle(HttpServletRequest request, HttpServletResponse response) {
        for (SimpleLogger logger : loggers) {
            logger.log(formatter.format(request, response));
        }
    }
}
