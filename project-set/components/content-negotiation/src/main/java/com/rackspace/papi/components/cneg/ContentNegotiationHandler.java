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
package com.rackspace.papi.components.cneg;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.config.cneg.ContentNegotiation;
import com.rackspace.papi.filter.logic.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterDirector;

/**
 *
 * @author jhopper
 */
public class ContentNegotiationHandler extends AbstractFilterLogicHandler {

    public ContentNegotiationHandler() {
    }

    private final UpdateListener<ContentNegotiation> contentNegotiationConfigurationListener = new UpdateListener<ContentNegotiation>() {

        @Override
        public void configurationUpdated(ContentNegotiation modifiedConfig) {
            throw new UnsupportedOperationException("Not supported");
        }
    };

    public UpdateListener<ContentNegotiation> getContentNegotiationConfigurationListener() {
        return contentNegotiationConfigurationListener;
    }

    public FilterDirector handleRequest(MutableHttpServletRequest request, MutableHttpServletResponse response) {
        // TODO: Add logic for content negotiation
        return null;
    }
}
