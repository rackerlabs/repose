package com.rackspace.papi.commons.util.logging.apache.format.stock;

import com.rackspace.papi.commons.util.logging.apache.format.FormatterLogic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class ResponseTimeHandler implements FormatterLogic {

    private static final String START_TIME_ATTRIBUTE = "com.rackspace.repose.logging.start.time";
    private static long MICROSECOND_MULTIPLIER = 1000;
    

    @Override
    public String handle(HttpServletRequest request, HttpServletResponse response) {
        Object startTime = request.getAttribute(START_TIME_ATTRIBUTE);
        String responseTime = "";

        if (startTime != null) {
            responseTime = Long.toString( (System.currentTimeMillis() - (Long)startTime) * MICROSECOND_MULTIPLIER);
        }

        return responseTime;
    }
}
