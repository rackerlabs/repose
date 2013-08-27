package com.rackspace.papi.components.hnorm;

import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.hnorm.util.CompiledRegexAndList;
import com.rackspace.papi.components.hnorm.util.HeaderNormalizer;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import com.rackspace.papi.filters.HeaderNormalization;
import com.rackspace.papi.service.reporting.metrics.MetricsService;
import com.rackspace.papi.service.reporting.metrics.impl.MeterByCategorySum;
import com.rackspacecloud.api.docs.repose.header_normalization.v1.HttpMethod;

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
