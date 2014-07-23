package com.rackspace.papi.components.slf4jlogging;

import com.rackspace.papi.service.config.manager.UpdateListener;
import com.rackspace.papi.components.slf4jlogging.config.Slf4JHttpLog;
import com.rackspace.papi.components.slf4jlogging.config.Slf4JHttpLoggingConfig;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;


public class Slf4jHttpLoggingHandlerFactory extends AbstractConfiguredFilterHandlerFactory<Slf4jHttpLoggingHandler> {

    private final List<Slf4jLoggerWrapper> loggerWrappers;

    public Slf4jHttpLoggingHandlerFactory() {
        loggerWrappers = new CopyOnWriteArrayList<Slf4jLoggerWrapper>();
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        return new HashMap<Class, UpdateListener<?>>() {
            {
                put(Slf4JHttpLoggingConfig.class, new Slf4jHttpLoggingConfigurationListener());
            }
        };
    }

    protected List<Slf4jLoggerWrapper> getLoggerWrappers() {
        return loggerWrappers;
    }

    private class Slf4jHttpLoggingConfigurationListener implements UpdateListener<Slf4JHttpLoggingConfig> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(Slf4JHttpLoggingConfig modifiedConfig) {
            List<Slf4jLoggerWrapper> transaction = new LinkedList<Slf4jLoggerWrapper>();

            for (Slf4JHttpLog logConfig : modifiedConfig.getSlf4JHttpLog()) {
                String loggerName = logConfig.getId();
                String formatString = logConfig.getFormat();

                Slf4jLoggerWrapper existingWrapper = updateExisting(loggerWrappers, loggerName, formatString);
                if (existingWrapper == null) {
                    existingWrapper = new Slf4jLoggerWrapper(LoggerFactory.getLogger(loggerName), formatString);
                }

                transaction.add(existingWrapper);
            }

            //commit the transaction
            //This will replace the contents of the copy on write array list with the new items.
            loggerWrappers.clear();
            loggerWrappers.addAll(transaction);

            isInitialized = true;
        }

        private Slf4jLoggerWrapper updateExisting(List<Slf4jLoggerWrapper> existing, String name, String formatString) {
            Slf4jLoggerWrapper returnWrapper = null;
            for (Slf4jLoggerWrapper existingWrapper : existing) {
                if (existingWrapper.getLogger().getName().equals(name)) {
                    //an existing logger has the same name as we're changing
                    if (formatString.equals(existingWrapper.getFormatter())) {
                        //They're the same, we'll keep it, nothing actually changed
                        returnWrapper = existingWrapper;
                    } else {
                        //and create the new one based on the config
                        returnWrapper = new Slf4jLoggerWrapper(LoggerFactory.getLogger(name), formatString);
                    }
                }
            }
            return returnWrapper;
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
        return new Slf4jHttpLoggingHandler(new LinkedList<Slf4jLoggerWrapper>(loggerWrappers));
    }
}
