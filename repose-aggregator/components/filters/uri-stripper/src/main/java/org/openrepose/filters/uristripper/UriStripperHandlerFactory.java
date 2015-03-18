/*
 *  Copyright (c) 2015 Rackspace US, Inc.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.openrepose.filters.uristripper;

import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.core.filter.logic.AbstractConfiguredFilterHandlerFactory;
import org.openrepose.filters.uristripper.config.UriStripperConfig;

import java.util.HashMap;
import java.util.Map;

public class UriStripperHandlerFactory extends AbstractConfiguredFilterHandlerFactory<UriStripperHandler> {


    int stripId;
    boolean rewriteLocation;

    protected UriStripperHandlerFactory() {
    }

    @Override
    protected UriStripperHandler buildHandler() {
        if (!this.isInitialized()) {
            return null;
        }
        return new UriStripperHandler(stripId,rewriteLocation);
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        return new HashMap<Class, UpdateListener<?>>() {
            {
                put(UriStripperConfig.class, new UriTranslationConfigurationListener());
            }
        };
    }

    private class UriTranslationConfigurationListener implements UpdateListener<UriStripperConfig> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(UriStripperConfig config) {

            stripId = config.getTokenIndex();
            rewriteLocation = config.isRewriteLocation();
            isInitialized = true;

        }
        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }
}
