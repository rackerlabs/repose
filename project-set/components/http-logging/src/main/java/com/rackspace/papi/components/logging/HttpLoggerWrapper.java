package com.rackspace.papi.components.logging;

import com.rackspace.papi.commons.util.logging.apache.HttpLogFormatter;
import com.rackspace.papi.components.logging.util.SimpleLogger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedList;
import java.util.List;

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
    
    protected List<SimpleLogger> getLoggers() {
       return loggers;
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
