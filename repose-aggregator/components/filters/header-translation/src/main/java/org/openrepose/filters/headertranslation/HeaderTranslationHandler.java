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
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.core.filter.logic.FilterAction;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.HeaderManager;
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler;
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

public class HeaderTranslationHandler extends AbstractFilterLogicHandler {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(HeaderTranslationHandler.class);
    private final List<Header> sourceHeaders;

    public HeaderTranslationHandler(List<Header> sourceHeaders) {
        this.sourceHeaders = sourceHeaders;
    }

    @Override
    public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {

        final FilterDirector filterDirector = new FilterDirectorImpl();
        final HeaderManager headerManager = filterDirector.requestHeaderManager();
        filterDirector.setFilterAction(FilterAction.PASS);

        for (Header sourceHeader : sourceHeaders) {
            final String originalName = sourceHeader.getOriginalName();

            if (StringUtilities.isNotBlank(request.getHeader(originalName))) {

                final List<String> originalHeaderValue = Collections.list(request.getHeaders(originalName));

                for (String newname : sourceHeader.getNewName()) {
                    headerManager.appendHeader(newname, originalHeaderValue.toArray(new String[originalHeaderValue.size()]));
                    LOG.trace("Header added: " + newname);
                }

                if (sourceHeader.isRemoveOriginal()) {
                    headerManager.removeHeader(originalName);
                    LOG.trace("Header removed: " + originalName);
                }
            } else {
                LOG.trace("Header for translation not found: " + originalName);
            }
        }

        return filterDirector;
    }
}
