package org.openrepose.commons.utils.logging.apache.format.converters;

public interface FormatConverter {
   String convert(String value, String inputFormat, String outputFormat);
}
