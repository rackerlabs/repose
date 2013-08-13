package com.rackspace.papi.components.hnorm;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.components.hnorm.util.CompiledRegexAndList;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import com.rackspace.papi.service.reporting.metrics.MetricsService;
import com.rackspacecloud.api.docs.repose.header_normalization.v1.HeaderFilterList;
import com.rackspacecloud.api.docs.repose.header_normalization.v1.HeaderNormalizationConfig;
import com.rackspacecloud.api.docs.repose.header_normalization.v1.Target;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class HeaderNormalizationHandlerFactory extends AbstractConfiguredFilterHandlerFactory<HeaderNormalizationHandler> {
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(HeaderNormalizationHandlerFactory.class);
    private final MetricsService metricsService;
    private List<CompiledRegexAndList> compiledTargetList;

    public HeaderNormalizationHandlerFactory(MetricsService metricsService) {
        compiledTargetList = new LinkedList<CompiledRegexAndList>();
        this.metricsService = metricsService;
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        return new HashMap<Class, UpdateListener<?>>() {
            {
                put(HeaderNormalizationConfig.class, new ContentNormalizationConfigurationListener());
            }
        };
    }

    private class ContentNormalizationConfigurationListener implements UpdateListener<HeaderNormalizationConfig> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(HeaderNormalizationConfig configurationObject) {
            compiledTargetList.clear();
            final HeaderFilterList filterList = configurationObject.getHeaderFilters();
            CompiledRegexAndList compiledRegexAndList;
            if (filterList != null) {
                for (Target target : filterList.getTarget()) {
                    // TODO: Build objects with pre-compiled regexes and pass those to the Handler

                    if (target.getBlacklist().isEmpty()) {
                        compiledRegexAndList = new CompiledRegexAndList(target.getUriRegex(), target.getWhitelist().get(0).getHeader(), target.getHttpMethods(), Boolean.FALSE);
                    } else {
                        compiledRegexAndList = new CompiledRegexAndList(target.getUriRegex(), target.getBlacklist().get(0).getHeader(), target.getHttpMethods(), Boolean.TRUE);

                    }
                    compiledTargetList.add(compiledRegexAndList);
                }
            } else {
                LOG.warn("No Header List Configured for Header Normalizer Filter");
            }

            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }

    @Override
    protected HeaderNormalizationHandler buildHandler() {
        if (!this.isInitialized()) {
            return null;
        }
        return new HeaderNormalizationHandler(compiledTargetList, metricsService);
    }
}
