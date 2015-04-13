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
package org.openrepose.filters.headeridmapping;

import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.core.filter.logic.AbstractConfiguredFilterHandlerFactory;
import org.openrepose.filters.headeridmapping.config.HeaderIdMappingConfig;
import org.openrepose.filters.headeridmapping.config.HttpHeader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeaderIdMappingHandlerFactory extends AbstractConfiguredFilterHandlerFactory<HeaderIdMappingHandler> {

    private List<HttpHeader> sourceHeaders;

    public HeaderIdMappingHandlerFactory() {
        sourceHeaders = new ArrayList<HttpHeader>();
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {

        return new HashMap<Class, UpdateListener<?>>() {
            {
                put(HeaderIdMappingConfig.class, new HeaderIdMappingConfigurationListener());
            }
        };
    }

    @Override
    protected HeaderIdMappingHandler buildHandler() {
        if (!this.isInitialized()) {
            return null;
        }
        return new HeaderIdMappingHandler(sourceHeaders);
    }

    private class HeaderIdMappingConfigurationListener implements UpdateListener<HeaderIdMappingConfig> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(HeaderIdMappingConfig configurationObject) {

            sourceHeaders = configurationObject.getSourceHeaders().getHeader();
            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }
}
