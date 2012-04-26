package com.rackspace.papi.components.hnorm;

import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspacecloud.api.docs.powerapi.header_normalization.v1.HttpHeader;
import com.rackspacecloud.api.docs.powerapi.header_normalization.v1.HeaderFilterList;

import java.util.Enumeration;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author jhopper
 */
public class HeaderNormalizer {

    private final List<HttpHeader> headers;
    private final boolean isBlacklist;

    public HeaderNormalizer(HeaderFilterList headerFilterList, boolean isBlacklist) {
       // TODO: refactor to account for uri and http-method matching
        this.headers = null; //isBlacklist ? headerFilterList.getBlacklist().getHeader() : headerFilterList.getWhitelist().getHeader();
        this.isBlacklist = isBlacklist;
    }

    public void normalizeHeaders(HttpServletRequest request, FilterDirector currentDirector) {
        filterHeaders(request, currentDirector);
    }

    private void filterHeaders(HttpServletRequest request, FilterDirector myDirector) {
        final Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            final String requestHeaderName = headerNames.nextElement();

            boolean found = false;

            for (HttpHeader configuredHeader : headers) {
                if (configuredHeader.getId().equalsIgnoreCase(requestHeaderName)) {
                    found = true;
                    break;
                }
            }

            if (found && isBlacklist || !found && !isBlacklist) {
                myDirector.requestHeaderManager().removeHeader(requestHeaderName);
            }
        }
    }
}
