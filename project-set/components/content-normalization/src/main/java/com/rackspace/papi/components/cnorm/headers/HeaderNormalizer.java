package com.rackspace.papi.components.cnorm.headers;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author jhopper
 */
public class HeaderNormalizer {

    private final List<String> headers;
    private final boolean isBlacklist;

    public HeaderNormalizer(List<String> headers, boolean isBlacklist) {
        this.headers = headers;
        this.isBlacklist = isBlacklist;
    }

    public FilterDirector normalizeHeaders(HttpServletRequest request, FilterDirector currentDirector) {
        final FilterDirector myDirector = new FilterDirectorImpl(currentDirector);

        normalizeContentType(request, myDirector);
        filterHeaders(request, myDirector);

        return myDirector;
    }

    // TODO: Need to do some analysis of this code when we get ready to do Content Normalization
    private void normalizeContentType(HttpServletRequest request, FilterDirector myDirector) {
//        final String acceptsHeader = request.getHeader(CommonHttpHeader.ACCEPT.headerKey());
//
//        final ContentTypeExtractionStrategy variantExtractor = MediaRangeParser.fromVariant(request.getRequestURI());
//        MediaType normalizedContentType = variantExtractor.isWellFormed() ? variantExtractor.toContentType() : MediaType.UNKNOWN;
//
//        if (!StringUtilities.isBlank(acceptsHeader)) {
//            final ContentTypeExtractionStrategy acceptHeaderExtractor = MediaRangeParser.fromVariant(request.getRequestURI());
//
//            if (acceptHeaderExtractor.isWellFormed()) {
//                //Add the version if it exists
//                if (acceptHeaderExtractor.hasVersion()) {
//                    myDirector.requestHeaderManager().putHeader(CommonHttpHeader.ACCEPT.headerKey(), acceptHeaderExtractor.getVersion());
//                }
//
//                normalizedContentType = acceptHeaderExtractor.toContentType();
//            }
//        }
//
//        if (normalizedContentType != MediaType.UNKNOWN){
//            myDirector.requestHeaderManager().putHeader(CommonHttpHeader.ACCEPT.headerKey(), normalizedContentType.toString());
//        } else {
//            myDirector.setFilterAction(FilterAction.RETURN);
//            myDirector.setResponseStatus(HttpStatusCode.BAD_REQUEST);
//        }
    }

    private void filterHeaders(HttpServletRequest request, FilterDirector myDirector) {
        for (String headerName : headers) {
            final String headerValue = request.getHeader(headerName);

            if (!StringUtilities.isBlank(headerValue)) {
                if (isBlacklist) {
                    myDirector.requestHeaderManager().removeHeader(headerName);
                }
            } else if (!isBlacklist) {
                myDirector.requestHeaderManager().removeHeader(headerName);
            }
        }
    }
}
