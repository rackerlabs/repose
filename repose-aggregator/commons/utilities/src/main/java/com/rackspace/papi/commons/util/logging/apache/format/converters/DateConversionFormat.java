package com.rackspace.papi.commons.util.logging.apache.format.converters;

public enum DateConversionFormat {

    RFC_1123("E, dd MMM yyyy HH:mm:ss z"),
    ISO_8601("yyyy-MM-dd'T'HH:mm:ss'Z'");  //this is incorrect, but there isn't a right solution in java 1.6
//    ISO_8601("yyyy-MM-dd'T'HH:mm:ssX");  //use this one when we switch to java 1.7 it's correct
    private String pattern;

    DateConversionFormat(String pattern) {
        this.pattern = pattern;
    }

    public static String getPattern(String name) {
        for (DateConversionFormat format : DateConversionFormat.values()) {
            if (format.name().equals(name)) {
                return format.pattern;
            }
        }

        return RFC_1123.pattern;
    }

    public String getPattern() {
        return this.pattern;
    }
}
