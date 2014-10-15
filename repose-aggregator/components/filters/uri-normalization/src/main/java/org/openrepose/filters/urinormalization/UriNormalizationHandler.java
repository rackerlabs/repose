package org.openrepose.filters.urinormalization;

import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.filters.urinormalization.normalizer.MediaTypeNormalizer;
import org.openrepose.core.filter.logic.FilterAction;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler;
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl;
import org.openrepose.core.filters.UriNormalization;
import org.openrepose.core.services.reporting.metrics.MetricsService;
import org.openrepose.core.services.reporting.metrics.impl.MeterByCategorySum;

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
