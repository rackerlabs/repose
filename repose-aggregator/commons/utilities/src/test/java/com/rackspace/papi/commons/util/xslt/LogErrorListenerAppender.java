package com.rackspace.papi.commons.util.xslt;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;


public class LogErrorListenerAppender extends AppenderSkeleton {
   @Override
   protected void append (LoggingEvent le) {
      //
      //  There are two messages that we expect from
      //  com.rackspace.papi.commons.util.xslt.LogErrorListener
      //
      //  "This is simply a warning", "Throwing Error!", and "Fatal
      //  error while processing XSLT:" if we get anything else we
      //  throw an AssertionError.
      //
      String msg = (String) le.getMessage();
      if (le.getLoggerName().equals("com.rackspace.papi.commons.util.xslt.LogErrorListener")) {
         if (!msg.contains("This is simply a warning") &&
             !msg.contains("Throwing Error!") &&
             !msg.contains("Fatal error while processing XSLT:") &&
             !msg.contains("Termination forced by an xsl:message instruction")) {
            throw new AssertionError ("Unexpected message from LogErrorListener: "+msg);
         }
      }
   }

   @Override
   public boolean requiresLayout() {
      return true;
   }

   @Override
   public void close() {}
}
