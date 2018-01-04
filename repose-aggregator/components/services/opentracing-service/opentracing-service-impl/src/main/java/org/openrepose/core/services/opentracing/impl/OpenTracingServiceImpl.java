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
package org.openrepose.core.services.opentracing.impl;

import io.opentracing.Scope;
import io.opentracing.Tracer;
import io.opentracing.tag.AbstractTag;
import io.opentracing.util.GlobalTracer;
import org.openrepose.core.service.opentracing.config.OpenTracingConfig;
import org.openrepose.core.service.opentracing.config.TracerType;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.healthcheck.HealthCheckService;
import org.openrepose.core.services.healthcheck.HealthCheckServiceProxy;
import org.openrepose.core.services.healthcheck.Severity;
import org.slf4j.Logger;
import org.openrepose.commons.config.manager.UpdateListener;
import org.springframework.beans.factory.annotation.Value;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

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

    private static Tracer globalTracer;


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

        healthCheckServiceProxy.reportIssue(OPENTRACING_SERVICE_REPORT, "OpenTracing Service Configuration Error", Severity.BROKEN);
        URL xsdURL = getClass().getResource("/META-INF/schema/config/opentracing.xsd");
        configurationService.subscribeTo(DEFAULT_CONFIG_NAME, xsdURL, configurationListener, OpenTracingConfig.class);

        try {
            // this is fine only if we don't want to use opentracing.  By default it's turned off, but we'll have problems if it's on
            if (!configurationListener.isInitialized() && !configurationService.getResourceResolver().resolve(DEFAULT_CONFIG_NAME).exists()) {
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
        return this.isEnabled;
    }

    @Override
    public Tracer getGlobalTracer() {
        // if globalTracer is not set, we got a problem.  We're going to throw a null pointer exception once
        // where this was called but update the enabled flag to false so that we never hit that condition again
        if (globalTracer == null) {
            LOG.error("Opentracing configuration is missing.  " +
                "Check that you have opentracing.cfg.xml properly configured with one of the tracers registered");
            this.isEnabled = false;
        }

        return globalTracer;
    }


    /**
     * Get current ActiveSpan.  This could be either the root span (initial request), a new span of a parent
     * active span, or a sibling span (a span that's not a direct descendant or a parent span but is related
     * to it somehow.
     * <p/>
     * For more information, https://github.com/opentracing/specification/blob/master/specification.md
     * <p/>
     *
     * Span will have:
     *
     * * Operation name
     * * Start timestamp
     * * Finish timestamp
     * * (optional) Span tags
     * * (optional) Span logs
     *
     * @return a ActiveSpan which corresponds to the clientId parameter
     */
    @Override
    public Scope getActiveSpan() {
        verifyInitialized();
        throw new NotImplementedException();
    }

    @Override
    public Scope startNewSpan(String spanName, boolean ignoreParent, Map<String, String> tags) {
        verifyInitialized();
        throw new NotImplementedException();
    }


    public void configure(OpenTracingConfig openTracingConfig) {
        switch (openTracingConfig.getTracer()) {
            case JAEGER:
                GlobalTracer.register(
                    new com.uber.jaeger.Configuration(
                        openTracingConfig.getName(),
                        new com.uber.jaeger.Configuration.SamplerConfiguration("const", 1),
                        new com.uber.jaeger.Configuration.ReporterConfiguration(
                            true,  // logSpans
                            openTracingConfig.getTracerHost(),
                            openTracingConfig.getTracerPort(),
                            openTracingConfig.getFlushIntervalMs(),
                            openTracingConfig.getMaxBufferSize())
                    ).getTracer());
                break;
            default:
                LOG.error("Invalid tracer specified.  Problem with opentracing.xsd enumeration");
                this.isEnabled = false;
        }

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
