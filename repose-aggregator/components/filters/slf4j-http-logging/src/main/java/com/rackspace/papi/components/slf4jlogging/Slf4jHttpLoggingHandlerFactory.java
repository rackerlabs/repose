package com.rackspace.papi.components.slf4jlogging;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.components.slf4jlogging.config.Slf4JHttpLoggingConfig;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jhopper
 */
public class Slf4jHttpLoggingHandlerFactory extends AbstractConfiguredFilterHandlerFactory<Slf4jHttpLoggingHandler> {

    private final List<Logger> loggers;

    public Slf4jHttpLoggingHandlerFactory() {
        loggers = new LinkedList<Logger>();
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        return new HashMap<Class, UpdateListener<?>>() {
            {
                put(Slf4JHttpLoggingConfig.class, new Slf4jHttpLoggingConfigurationListener());
            }
        };
    }

    protected List<Logger> getLoggers() {
        return loggers;
    }

    private class Slf4jHttpLoggingConfigurationListener implements UpdateListener<Slf4JHttpLoggingConfig> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(Slf4JHttpLoggingConfig modifiedConfig) {
            //Clean up~
            destroy();

            //Create the new log4j targets for the log4j backend.
            //For each item in the configuration, create the new log targets

            isInitialized = true;
        }

        private void destroy() {
            //Clobber the existing log4j appenders, don't want them any more
            //Need to only remove the existing appenders, can't risk clobbering more than that.
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }

    @Override
    protected Slf4jHttpLoggingHandler buildHandler() {

        if (!this.isInitialized()) {
            return null;
        }
        return new Slf4jHttpLoggingHandler(new LinkedList<Logger>(loggers));
    }
}
