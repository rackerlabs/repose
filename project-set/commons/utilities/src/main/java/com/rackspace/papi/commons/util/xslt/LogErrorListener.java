package com.rackspace.papi.commons.util.xslt;

import com.rackspace.papi.commons.util.logging.ExceptionLogger;
import org.slf4j.Logger;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

public final class LogErrorListener implements ErrorListener {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(LogErrorListener.class);
   private static final ExceptionLogger EXCEPTION_LOG = new ExceptionLogger(LOG);
   private static final String STOCK_ERROR_MSG = "Fatal error while processing XSLT, see previous LogErrorListener WARN for a hint. MSG : ";

   public void warning(TransformerException te) {
      LOG.warn(te.getMessageAndLocation());
   }

   public void error(TransformerException te) {
      throw EXCEPTION_LOG.newException(STOCK_ERROR_MSG + te.getMessageAndLocation(), te, RuntimeException.class);
   }

   public void fatalError(TransformerException te) {
      throw EXCEPTION_LOG.newException(STOCK_ERROR_MSG + te.getMessageAndLocation(), te, RuntimeException.class);
   }
}
