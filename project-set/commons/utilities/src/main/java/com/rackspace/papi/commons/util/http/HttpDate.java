package com.rackspace.papi.commons.util.http;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class HttpDate {

   private static final TimeZone GMT_TIMEZONE = TimeZone.getTimeZone("GMT");
   private final Date utcTime;

   public HttpDate(final Date utcTime) {
      this.utcTime = (Date) utcTime.clone();
   }

   //Sun, 06 Nov 1994 08:49:37 GMT
   public String toRFC1123() {
      final SimpleDateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
      formatter.setTimeZone(GMT_TIMEZONE);

      return formatter.format(utcTime);
   }

}
