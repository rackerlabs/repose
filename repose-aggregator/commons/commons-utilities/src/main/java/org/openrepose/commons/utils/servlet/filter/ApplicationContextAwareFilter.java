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
package org.openrepose.commons.utils.servlet.filter;

import org.openrepose.commons.utils.servlet.InitParameter;
import org.openrepose.commons.utils.servlet.context.ApplicationContextAdapter;
import org.openrepose.commons.utils.servlet.context.exceptions.ContextAdapterResolutionException;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

/**
 * This class serves as an abstraction parent to any number of IOC Containers herefore referenced as the application
 * context. This is not a naming context in the JNDI sense nor is it the Servlet context.
 */
public abstract class ApplicationContextAwareFilter implements Filter {

    private ApplicationContextAdapter applicationContextAdapter;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

        final String adapterClassName = filterConfig.getInitParameter(InitParameter.APP_CONTEXT_ADAPTER_CLASS.getParameterName());

        if (adapterClassName != null && !"".equals(adapterClassName)) {
            try {
                final Object freshAdapter = Class.forName(adapterClassName).newInstance();

                if (freshAdapter instanceof ApplicationContextAdapter) {
                    applicationContextAdapter = (ApplicationContextAdapter) freshAdapter;
                    applicationContextAdapter.usingServletContext(filterConfig.getServletContext());
                } else {
                    throw new ContextAdapterResolutionException("Unknown application context adapter class: " + adapterClassName);
                }
            } catch (Exception ex) {
                throw new ContextAdapterResolutionException("Failure on ApplicationContextAwareFilter.init(...). Reason: " + ex.getMessage(), ex);
            }
        }
    }

    public ApplicationContextAdapter getAppContext() {
        return applicationContextAdapter;
    }
}
