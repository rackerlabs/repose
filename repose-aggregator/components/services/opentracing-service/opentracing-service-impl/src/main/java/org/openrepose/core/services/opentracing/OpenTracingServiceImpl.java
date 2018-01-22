/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.services.opentracing;

import com.uber.jaeger.Configuration;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import org.apache.commons.lang3.NotImplementedException;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.core.service.opentracing.config.OpenTracingConfig;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.healthcheck.HealthCheckService;
import org.openrepose.core.services.healthcheck.HealthCheckServiceProxy;
import org.openrepose.core.services.healthcheck.Severity;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.URL;

/**
 * OpenTracingService - service that integrates OpenTracing standards into Repose
 */
@Named
public class OpenTracingServiceImpl implements OpenTracingService {

    public static final String DEFAULT_CONFIG_NAME = "opentracing.cfg.xml";

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(OpenTracingServiceImpl.class);
    private static final String OPENTRACING_SERVICE_REPORT = "OpenTracingServiceReport";

    private final ConfigurationService configurationService;
    private final ConfigurationListener configurationListener;
    private final HealthCheckServiceProxy healthCheckServiceProxy;

    private boolean isEnabled = false;

    private boolean initialized = false;

    private String serviceName = null;


    /**
     * Initial constructor
     * @param configurationService
     * @param healthCheckService
     */
    @Inject
    public OpenTracingServiceImpl(
        ConfigurationService configurationService,
        HealthCheckService healthCheckService
    ) {
        LOG.debug("Creating New OpenTracing Service");

        this.configurationService = configurationService;
        this.healthCheckServiceProxy = healthCheckService.register();
        this.configurationListener = new ConfigurationListener();
    }

    @PostConstruct
    public void init() {
        LOG.debug("Initializing OpenTracingService");

        healthCheckServiceProxy.reportIssue(
            OPENTRACING_SERVICE_REPORT, "OpenTracing Service Configuration Error",
            Severity.BROKEN);
        URL xsdURL = getClass().getResource("/META-INF/schema/config/opentracing.xsd");
        configurationService.subscribeTo(DEFAULT_CONFIG_NAME, xsdURL, configurationListener, OpenTracingConfig.class);

        try {

            LOG.trace("Check to see if opentracing is initialized.  If not, it's ok.");
            if (!configurationListener.isInitialized() && !configurationService.getResourceResolver().resolve(DEFAULT_CONFIG_NAME).exists()) {
                LOG.trace("this is fine only if we don't want to use opentracing.  " +
                    "By default it's turned off, but we'll have problems if it's on and the configuration is not correct.");
                healthCheckServiceProxy.resolveIssue(OPENTRACING_SERVICE_REPORT);
            }
        } catch (IOException io) {
            LOG.error("Error attempting to search for {}", DEFAULT_CONFIG_NAME, io);
        }
    }

    @PreDestroy
    public void destroy() {
        configurationService.unsubscribeFrom(DEFAULT_CONFIG_NAME, configurationListener);
    }

    @Override
    public boolean isEnabled(){
        // this is done because the issue may have been resolved so we try again.
        if (healthCheckServiceProxy.getDiagnosis(OPENTRACING_SERVICE_REPORT) != null &&
            healthCheckServiceProxy.getDiagnosis(OPENTRACING_SERVICE_REPORT).getLevel() == Severity.BROKEN)
            healthCheckServiceProxy.resolveIssue(OPENTRACING_SERVICE_REPORT);
        return this.isEnabled;
    }

    @Override
    public Tracer getGlobalTracer() {
        LOG.trace("Retrieve global tracer.");
        try {
            verifyInitialized();

            if (!GlobalTracer.isRegistered()) {
                LOG.error("Opentracing configuration is missing.  " +
                    "Check that you have opentracing.cfg.xml properly configured with one of the tracers registered");
                LOG.trace("If we don't disable it, we would through an NPE wherever the tracer is called.  Don't do that.");
                this.isEnabled = false;
            }
        } catch (IllegalStateException ise) {
            LOG.error("Opentracing was not initialized.  We will turn this off.  Check the logs for the issue. " +
                "For example, an invalid tracer host/port");
            this.isEnabled = false;
        }

        return GlobalTracer.get();
    }

    @Override
    public String getServiceName() {
        return this.serviceName;
    }

    @Override
    public String getTracerHeaderName() {
        throw new NotImplementedException("Not yet implemented.");
    }

    public void configure(OpenTracingConfig openTracingConfig) {
        LOG.trace("get the tracer from configuration");
        switch (openTracingConfig.getTracer()) {
            case JAEGER:
                LOG.debug("register Jaeger tracer");
                Configuration configuration = new com.uber.jaeger.Configuration(
                    openTracingConfig.getName(),
                    new com.uber.jaeger.Configuration.SamplerConfiguration("const", 1), // default configuration.  needs to read from config
                    new com.uber.jaeger.Configuration.ReporterConfiguration(
                        true,  // logSpans
                        openTracingConfig.getTracerHost(),
                        openTracingConfig.getTracerPort(),
                        openTracingConfig.getFlushIntervalMs(),
                        openTracingConfig.getMaxBufferSize())
                );

                LOG.trace("register the tracer with global tracer");
                GlobalTracer.register(configuration.getTracer());
                break;
            default:
                LOG.error("Invalid tracer specified.  Problem with opentracing.xsd enumeration");
                this.isEnabled = false;
        }

        this.serviceName = openTracingConfig.getName();
        this.isEnabled = openTracingConfig.isEnabled();
    }

    private void verifyInitialized() {
        if (!initialized) {
            throw new IllegalStateException("The OpenTracingService has not yet been initialized");
        }
    }


    private class ConfigurationListener implements UpdateListener<OpenTracingConfig> {
        @Override
        public void configurationUpdated(OpenTracingConfig openTracingConfig) {

            configure(openTracingConfig);
            initialized = true;
            healthCheckServiceProxy.resolveIssue(OPENTRACING_SERVICE_REPORT);
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }
    }


}
