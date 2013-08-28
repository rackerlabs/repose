package com.rackspace.papi.components.unorm;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.unorm.normalizer.MediaTypeNormalizer;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import com.rackspace.papi.filters.UriNormalization;
import com.rackspace.papi.service.reporting.metrics.MetricsService;
import com.rackspace.papi.service.reporting.metrics.impl.MeterByCategorySum;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Dan Daley
 */
public class UriNormalizationHandler extends AbstractFilterLogicHandler {
    private final Collection<QueryParameterNormalizer> queryStringNormalizers;
    private final MediaTypeNormalizer mediaTypeNormalizer;
    private final MetricsService metricsService;
    private MeterByCategorySum mbcsUriNormalizations;

    public UriNormalizationHandler(Collection<QueryParameterNormalizer> queryStringNormalizers, MediaTypeNormalizer mediaTypeNormalizer, MetricsService metricsService) {
        this.queryStringNormalizers = queryStringNormalizers;
        this.mediaTypeNormalizer = mediaTypeNormalizer;
        this.metricsService = metricsService;

        // TODO replace "uri-normalization" with filter-id or name-number in sys-model
        if (metricsService != null) {
            mbcsUriNormalizations = metricsService.newMeterByCategorySum(UriNormalization.class,
                    "uri-normalization", "Normalization", TimeUnit.SECONDS);
        }
    }

    @Override
    public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
        final FilterDirector myDirector = new FilterDirectorImpl();
        myDirector.setFilterAction(FilterAction.PASS);

        mediaTypeNormalizer.normalizeContentMediaType(request, myDirector);

        // TODO: Refactor this into a object like the above method call?
        if (!StringUtilities.isEmpty(request.getQueryString())) {
            normalizeUriQuery(request, myDirector);
        }

        return myDirector;
    }

    private void normalizeUriQuery(HttpServletRequest request, FilterDirector myDirector) {
        for (QueryParameterNormalizer queryParameterNormalizer : queryStringNormalizers) {
            if (queryParameterNormalizer.normalize(request, myDirector)) {
                if (mbcsUriNormalizations != null) {
                    mbcsUriNormalizations.mark(queryParameterNormalizer.getLastMatch().toString() + "_" + request.getMethod());
                }
                break;
            }
        }
    }
}
