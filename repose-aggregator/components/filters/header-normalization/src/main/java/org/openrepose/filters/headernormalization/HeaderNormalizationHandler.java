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
package org.openrepose.filters.headernormalization;

import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.filters.headernormalization.util.CompiledRegexAndList;
import org.openrepose.filters.headernormalization.util.HeaderNormalizer;
import org.openrepose.core.filter.logic.FilterAction;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler;
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl;
import org.openrepose.core.filters.HeaderNormalization;
import org.openrepose.core.services.reporting.metrics.MetricsService;
import org.openrepose.core.services.reporting.metrics.impl.MeterByCategorySum;
import org.openrepose.filters.headernormalization.config.HttpMethod;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class HeaderNormalizationHandler extends AbstractFilterLogicHandler {
    private final MetricsService metricsService;
    private List<CompiledRegexAndList> compiledTargets;
    private MeterByCategorySum mbcsNormalizations;

    HeaderNormalizationHandler(List<CompiledRegexAndList> compiledTargets, MetricsService metricsService) {
        this.compiledTargets = compiledTargets;
        this.metricsService = metricsService;

        // TODO replace "header-normalization" with filter-id or name-number in sys-model
        if (metricsService != null) {
            mbcsNormalizations = metricsService.newMeterByCategorySum(HeaderNormalization.class,
                    "header-normalization", "Normalization", TimeUnit.SECONDS);
        }
    }

    @Override
    public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
        final FilterDirector myDirector = new FilterDirectorImpl();
        myDirector.setFilterAction(FilterAction.PASS);

        String uri = request.getRequestURI();
        HttpMethod method = HttpMethod.valueOf(request.getMethod());

        for (CompiledRegexAndList target : compiledTargets) {
            if (target.getPattern().matcher(uri).matches() && (target.getMethodList().contains(method) || target.getMethodList().contains(HttpMethod.ALL))) {
                myDirector.requestHeaderManager().headersToRemove().addAll(HeaderNormalizer.getHeadersToRemove(request, target));
                if (mbcsNormalizations != null) {
                    mbcsNormalizations.mark(target.getPattern().toString() + "_" + method.value());
                }
                break;
            }
        }

        return myDirector;
    }
}
