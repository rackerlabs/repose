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
package org.openrepose.filters.cnorm;

import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.filters.cnorm.normalizer.HeaderNormalizer;
import org.openrepose.filters.cnorm.normalizer.MediaTypeNormalizer;
import org.openrepose.core.filter.logic.FilterAction;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler;
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Dan Daley
 */
public class ContentNormalizationHandler extends AbstractFilterLogicHandler {
    private HeaderNormalizer headerNormalizer;
    private MediaTypeNormalizer mediaTypeNormalizer;

    public ContentNormalizationHandler(HeaderNormalizer headerNormalizer, MediaTypeNormalizer mediaTypeNormalizer) {
        this.headerNormalizer = headerNormalizer;
        this.mediaTypeNormalizer = mediaTypeNormalizer;
    }

    @Override
    public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
        final FilterDirector myDirector = new FilterDirectorImpl();
        myDirector.setFilterAction(FilterAction.PASS);
        if(headerNormalizer != null) {
            headerNormalizer.normalizeHeaders(request, myDirector);
        }
        if(mediaTypeNormalizer != null) {
            mediaTypeNormalizer.normalizeContentMediaType(request, myDirector);
        }
        return myDirector;
    }

}
