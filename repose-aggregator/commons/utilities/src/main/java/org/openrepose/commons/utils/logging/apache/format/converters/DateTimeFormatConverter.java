package org.openrepose.commons.utils.logging.apache.format.converters;

import org.openrepose.commons.utils.StringUtilities;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class DateTimeFormatConverter implements FormatConverter {

   private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(DateTimeFormatConverter.class);

   @Override
   public String convert(String value, String inputFormat, String outputFormat) {

      if (!StringUtilities.isBlank(value) && !StringUtilities.isBlank(outputFormat)) {
         try {
            String inputPattern = DateConversionFormat.getPattern(inputFormat);
            String outputPattern = DateConversionFormat.getPattern(outputFormat);
            SimpleDateFormat format = new SimpleDateFormat(outputPattern);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            return format.format(new SimpleDateFormat(inputPattern).parse(value));
         } catch (ParseException ex) {
            LOG.warn("Invalid date conversion parameters: " + value + "/" + inputFormat + "/" + outputFormat, ex);
         }
      }

      return value;
   }
}
