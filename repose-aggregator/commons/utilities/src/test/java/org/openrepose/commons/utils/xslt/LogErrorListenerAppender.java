package org.openrepose.commons.utils.xslt;

import org.apache.logging.log4j.core.*;

import java.io.Serializable;


public class LogErrorListenerAppender implements Appender {
    private static volatile State state = State.INITIALIZED;

    @Override
    public void append(LogEvent logEvent) {
      //
      // IF we receive anything other than one of the limited number of expected
      // messages from org.openrepose.commons.utils.xslt.LogErrorListener,
      // THEN we throw an AssertionError.
      //
      String msg = logEvent.getMessage().getFormattedMessage();
      if (logEvent.getLoggerName().equals("org.openrepose.commons.utils.xslt.LogErrorListener")) {
         if (!msg.contains("This is simply a warning") &&
             !msg.contains("Throwing Error!") &&
             !msg.contains("Fatal error while processing XSLT:") &&
             !msg.contains("Termination forced by an xsl:message instruction")) {
            throw new AssertionError ("Unexpected message from LogErrorListener: "+msg);
         }
      }
   }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Layout<? extends Serializable> getLayout() {
        return null;
    }

    @Override
    public boolean ignoreExceptions() {
        return false;
    }

    @Override
    public ErrorHandler getHandler() {
        return null;
    }

    @Override
    public void setHandler(ErrorHandler errorHandler) {

    }

    @Override
    public void start() {
        state = LifeCycle.State.STARTED;
    }

    @Override
    public void stop() {
        state = LifeCycle.State.STOPPED;
    }

    @Override
    public boolean isStarted() {
        return state == LifeCycle.State.STARTED;
    }

    @Override
    public boolean isStopped() {
        return state == LifeCycle.State.STOPPED;
    }
}
