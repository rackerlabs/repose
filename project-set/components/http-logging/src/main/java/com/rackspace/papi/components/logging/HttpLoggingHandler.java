package com.rackspace.papi.components.logging;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.logging.apache.HttpLogFormatter;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.components.logging.config.FileTarget;
import com.rackspace.papi.components.logging.config.HttpLog;
import com.rackspace.papi.components.logging.config.HttpLoggingConfig;
import com.rackspace.papi.components.logging.config.Targets;
import com.rackspace.papi.components.logging.util.FileLogger;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandler;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author jhopper
 */
public class HttpLoggingHandler extends AbstractConfiguredFilterHandler<HttpLoggingConfig> {

   private final List<HttpLoggerWrapper> loggers;

   public HttpLoggingHandler() {
      loggers = new LinkedList<HttpLoggerWrapper>();
   }

   @Override
   public void configurationUpdated(HttpLoggingConfig modifiedConfig) {
      //Clean up~
      destroy();

      for (HttpLog log : modifiedConfig.getHttpLog()) {
         final HttpLoggerWrapper loggerWrapper = new HttpLoggerWrapper(new HttpLogFormatter(log.getFormat()));
         final Targets targets = log.getTargets();

         if (targets.getFile() != null) {
            final FileTarget fTarget = targets.getFile();

            loggerWrapper.addLogger(new FileLogger(new File(fTarget.getLocation())));
         }

         loggers.add(loggerWrapper);
      }
   }

   /**
     * NOT THREAD SAFE!
     */
   private void destroy() {
      for (HttpLoggerWrapper loggerWrapper : loggers) {
         loggerWrapper.destroy();
      }

      loggers.clear();
   }

   public void handleResponse(MutableHttpServletRequest request, MutableHttpServletResponse response) {
      for (HttpLoggerWrapper loggerWrapper : loggers) {
         loggerWrapper.handle(request, response);
      }
   }
}
