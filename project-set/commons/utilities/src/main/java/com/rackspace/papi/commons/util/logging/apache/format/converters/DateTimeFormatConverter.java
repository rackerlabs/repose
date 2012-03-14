package com.rackspace.papi.commons.util.logging.apache.format.converters;

import com.rackspace.papi.commons.util.StringUtilities;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.slf4j.LoggerFactory;

public class DateTimeFormatConverter implements FormatConverter {

   private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(DateTimeFormatConverter.class);

   @Override
   public String convert(String value, String outputFormat, String inputFormat) {

      if (!StringUtilities.isBlank(value) && !StringUtilities.isBlank(outputFormat)) {
         try {
            String inputPattern = DateConversionFormat.getPattern(inputFormat);
            String outputPattern = DateConversionFormat.getPattern(outputFormat);
            return new SimpleDateFormat(outputPattern).format(new SimpleDateFormat(inputPattern).parse(value));
         } catch (ParseException ex) {
            LOG.warn("Invalid date conversion parameters: " + value + "/" + inputFormat + "/" + outputFormat, ex);
         }
      }

      return value;
   }
}
