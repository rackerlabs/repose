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
package org.openrepose.filters.translation;

import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.config.parser.generic.GenericResourceConfigurationParser;
import org.openrepose.commons.config.resource.ConfigurationResource;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.core.services.config.ConfigurationService;

import java.util.HashSet;
import java.util.Set;

public class XslUpdateListener implements UpdateListener<ConfigurationResource> {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(XslUpdateListener.class);
    private final TranslationFilter translationFilter;
    private final ConfigurationService configurationService;
    private final Set<String> watchList;
    private final String configRoot;
    private boolean isInitialized = false;

    public XslUpdateListener(TranslationFilter translationFilter, ConfigurationService configurationService, String configRoot) {
        this.translationFilter = translationFilter;
        this.configurationService = configurationService;
        this.watchList = new HashSet<>();
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
            configurationService.subscribeTo("translation", xsl, this, new GenericResourceConfigurationParser(), false);
        }
    }

    public void unsubscribe() {
        for (String xsl : watchList) {
            configurationService.unsubscribeFrom(xsl, this);
        }

        watchList.clear();
    }

    @Override
    public void configurationUpdated(ConfigurationResource config) {
        LOG.info("XSL file changed: " + config.name());
        translationFilter.buildProcessorPools();
        isInitialized = true;
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }
}
