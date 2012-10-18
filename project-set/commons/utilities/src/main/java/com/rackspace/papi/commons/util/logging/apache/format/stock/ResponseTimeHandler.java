package com.rackspace.papi.commons.util.logging.apache.format.stock;

import com.rackspace.papi.commons.util.logging.apache.format.FormatterLogic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.DecimalFormat;


public class ResponseTimeHandler implements FormatterLogic {

    private static final String START_TIME_ATTRIBUTE = "com.rackspace.repose.logging.start.time";
    private static final double DEFAULT_MULTIPLIER = 1000;
    private final DecimalFormat decimalFormat = new DecimalFormat("#.##");
    private double multiplier = DEFAULT_MULTIPLIER;

    public ResponseTimeHandler(double multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public String handle(HttpServletRequest request, HttpServletResponse response) {
        Object startTime = request.getAttribute(START_TIME_ATTRIBUTE);
        String responseTime = "";

        if (startTime != null) {
            responseTime = decimalFormat.format((System.currentTimeMillis() - (Long)startTime) * multiplier );
        }

        return responseTime;
    }
}
