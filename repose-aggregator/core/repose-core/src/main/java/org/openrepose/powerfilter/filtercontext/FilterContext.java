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
package org.openrepose.powerfilter.filtercontext;

import org.openrepose.commons.utils.Destroyable;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.openrepose.core.systemmodel.config.FilterCriterion;
import org.springframework.context.support.AbstractApplicationContext;

import javax.servlet.Filter;

/**
 * Holds information about a filter, the filter itself and the filter's application context.
 */
// @TODO: This class is OBE'd with REP-7231
@Deprecated
public class FilterContext implements Destroyable {

    private final Filter filter;
    private final org.openrepose.core.systemmodel.config.Filter filterConfig;
    private final String name;
    private final FilterCriterion filterCriterion;
    private final AbstractApplicationContext filterAppContext;

    public FilterContext(Filter filter, AbstractApplicationContext filterAppContext, org.openrepose.core.systemmodel.config.Filter filterConfig) {
        this.filter = filter;
        this.filterAppContext = filterAppContext;
        this.filterConfig = filterConfig;
        this.name = filterConfig.getName();
        filterCriterion = filterConfig.getFilterCriterion();
    }

    public Filter getFilter() {
        return filter;
    }

    public org.openrepose.core.systemmodel.config.Filter getFilterConfig() {
        return filterConfig;
    }

    public boolean shouldRun(HttpServletRequestWrapper request) {
        return filterCriterion.evaluate(request);
    }

    public String getName() {
        return name;
    }

    public boolean isFilterAvailable() {
        return filter != null;
    }

    public AbstractApplicationContext getFilterAppContext() {
        return filterAppContext;
    }

    @Override
    public void destroy() {
        if (filter != null) {
            filter.destroy();
        }
        if (filterAppContext != null) {
            filterAppContext.close();
        }
    }
}
