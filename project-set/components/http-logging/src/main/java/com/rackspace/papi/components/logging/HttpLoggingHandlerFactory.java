package com.rackspace.papi.components.logging;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.logging.apache.HttpLogFormatter;
import com.rackspace.papi.components.logging.config.FileTarget;
import com.rackspace.papi.components.logging.config.HttpLog;
import com.rackspace.papi.components.logging.config.HttpLoggingConfig;
import com.rackspace.papi.components.logging.config.Targets;
import com.rackspace.papi.components.logging.util.FileLogger;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jhopper
 */
public class HttpLoggingHandlerFactory extends AbstractConfiguredFilterHandlerFactory<HttpLoggingHandler> {

    private final List<HttpLoggerWrapper> loggers;

    public HttpLoggingHandlerFactory() {
        loggers = new LinkedList<HttpLoggerWrapper>();
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        return new HashMap<Class, UpdateListener<?>>() {
            {
                put(HttpLoggingConfig.class, new HttpLoggingConfigurationListener());
            }
        };
    }

    protected List<HttpLoggerWrapper> getLoggers() {
        return loggers;
    }

    private class HttpLoggingConfigurationListener implements UpdateListener<HttpLoggingConfig> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(HttpLoggingConfig modifiedConfig) {
            //Clean up~
            destroy();

            for (HttpLog log : modifiedConfig.getHttpLog()) {
                final HttpLoggerWrapper loggerWrapper = new HttpLoggerWrapper(new HttpLogFormatter(log.getFormat()));
                final Targets targets = log.getTargets();

                for (FileTarget target : targets.getFile()) {
                    loggerWrapper.addLogger(new FileLogger(new File(target.getLocation())));
                }
                loggers.add(loggerWrapper);
            }

            isInitialized = true;
        }

        private void destroy() {
            for (HttpLoggerWrapper loggerWrapper : loggers) {
                loggerWrapper.destroy();
            }

            loggers.clear();
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }

    @Override
    protected HttpLoggingHandler buildHandler() {

        if (!this.isInitialized()) {
            return null;
        }
        return new HttpLoggingHandler(new LinkedList<HttpLoggerWrapper>(loggers));
    }
}
