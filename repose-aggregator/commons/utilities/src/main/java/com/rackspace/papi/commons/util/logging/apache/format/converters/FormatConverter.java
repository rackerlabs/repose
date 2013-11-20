package com.rackspace.papi.commons.util.logging.apache.format.converters;

public interface FormatConverter {
   String convert(String value, String inputFormat, String outputFormat);
}
