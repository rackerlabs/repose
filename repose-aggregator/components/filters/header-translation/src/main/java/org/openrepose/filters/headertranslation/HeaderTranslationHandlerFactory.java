/*
 * #%L
 * Repose
 * %%
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * %%
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
 * #L%
 */
package org.openrepose.filters.headertranslation;

import org.openrepose.filters.headertranslation.config.Header;
import org.openrepose.filters.headertranslation.config.HeaderTranslationType;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.core.filter.logic.AbstractConfiguredFilterHandlerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeaderTranslationHandlerFactory extends AbstractConfiguredFilterHandlerFactory<HeaderTranslationHandler> {

    private List<Header> sourceHeaders;

    protected HeaderTranslationHandlerFactory() {
        sourceHeaders = new ArrayList<Header>();
    }

    @Override
    protected HeaderTranslationHandler buildHandler() {
        if (!this.isInitialized()) {
            return null;
        }
        return new HeaderTranslationHandler(sourceHeaders);
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        return new HashMap<Class, UpdateListener<?>>() {
            {
                put(HeaderTranslationType.class, new HeaderTranslationConfigurationListener());
            }
        };
    }

    private class HeaderTranslationConfigurationListener implements UpdateListener<HeaderTranslationType> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(HeaderTranslationType headerTranslationTypeConfigObject) {
            sourceHeaders = headerTranslationTypeConfigObject.getHeader();
            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }
}
