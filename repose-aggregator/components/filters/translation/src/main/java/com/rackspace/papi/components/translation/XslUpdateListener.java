package com.rackspace.papi.components.translation;

import org.openrepose.core.service.config.manager.UpdateListener;
import com.rackspace.papi.service.config.parser.generic.GenericResourceConfigurationParser;
import org.openrepose.core.service.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.util.StringUtilities;
import org.openrepose.core.service.config.ConfigurationService;

import java.util.HashSet;
import java.util.Set;

public class XslUpdateListener implements UpdateListener<ConfigurationResource> {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(XslUpdateListener.class);
    private final TranslationHandlerFactory factory;
    private final ConfigurationService configService;
    private final Set<String> watchList;
    private final String configRoot;
    private boolean isInitialized = false;

    public XslUpdateListener(TranslationHandlerFactory factory, ConfigurationService configService, String configRoot) {
        this.factory = factory;
        this.configService = configService;
        this.watchList = new HashSet<String>();
        this.configRoot = configRoot;
    }

    private String getAbsolutePath(String xslPath) {
        return !xslPath.contains("://") ? StringUtilities.join("file://", configRoot, "/", xslPath) : xslPath;
    }

    public void addToWatchList(String path) {
        watchList.add(getAbsolutePath(path));
    }

    public void listen() {
        for (String xsl : watchList) {
            LOG.info("Watching XSL: " + xsl);
            configService.subscribeTo("translation",xsl, this, new GenericResourceConfigurationParser(), false);
        }
    }

    public void unsubscribe() {
        for (String xsl : watchList) {
            configService.unsubscribeFrom(xsl, this);
        }

        watchList.clear();
    }

    @Override
    public void configurationUpdated(ConfigurationResource config) {
        LOG.info("XSL file changed: " + config.name());
        factory.buildProcessorPools();
        isInitialized = true;
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }
  
}
