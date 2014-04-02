package com.rackspace.papi.components.slf4jlogging;

import org.slf4j.Logger;

public class Slf4jLoggerWrapper {

    private Logger logger;
    private String formatString;

    public Slf4jLoggerWrapper(Logger logger, String formatString) {
        this.logger = logger;
        this.formatString = formatString;
    }

    public String getFormatString() {
        return formatString;
    }

    public Logger getLogger() {
        return logger;
    }
}
