package com.rackspace.papi.components.slf4jlogging;

import org.openrepose.commons.utils.logging.apache.HttpLogFormatter;
import org.slf4j.Logger;

public class Slf4jLoggerWrapper {

    private Logger logger;
    private HttpLogFormatter formatter;
    private String formatString;

    public Slf4jLoggerWrapper(Logger logger, String formatString) {
        this.logger = logger;
        this.formatString = formatString;
        this.formatter = new HttpLogFormatter(formatString);
    }

    public HttpLogFormatter getFormatter() {
        return formatter;
    }

    public String getFormatString() {
        return formatString;
    }

    public Logger getLogger() {
        return logger;
    }
}
