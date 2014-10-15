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
