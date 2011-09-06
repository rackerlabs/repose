/*
 *  Copyright 2010 Rackspace.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package com.rackspace.papi.components.logging;

import com.rackspace.papi.filter.logic.AbstractFilterLogicHandler;
import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.logging.apache.HttpLogFormatter;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.components.logging.config.FileTarget;
import com.rackspace.papi.components.logging.config.HttpLog;
import com.rackspace.papi.components.logging.config.HttpLoggingConfig;
import com.rackspace.papi.components.logging.config.Targets;
import com.rackspace.papi.components.logging.util.FileLogger;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author jhopper
 */
public class HttpLoggingHandler extends AbstractFilterLogicHandler {

    private final List<HttpLoggerWrapper> loggers;

    public HttpLoggingHandler() {
        loggers = new LinkedList<HttpLoggerWrapper>();
    }
    
    private class ConfigUpdateListener implements UpdateListener<HttpLoggingConfig> {
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
    }
    
    private final UpdateListener<HttpLoggingConfig> httpLoggingConfigurationListener = new ConfigUpdateListener();
    
    public UpdateListener<HttpLoggingConfig> getHttpLoggingConfigurationListener() {
        return httpLoggingConfigurationListener;
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
