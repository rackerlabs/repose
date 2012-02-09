package com.rackspace.papi.components.identity.header.extractor;

import com.rackspace.papi.components.identity.header.config.HttpHeader;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import com.rackspace.papi.commons.util.regex.ExtractorResult;

public class HeaderValueExtractor {

    private HttpServletRequest request;

    public HeaderValueExtractor(HttpServletRequest request) {
        this.request = request;
    }

    protected String extractHeader(String name) {
        // Header value may be a comma separated list of values.
        // The left most value in the list will be extracted.
        String header = request.getHeader(name);
        return header != null ? header.split(",")[0].trim() : "";
    }
    
    public ExtractorResult<String> extractUserGroup(List<HttpHeader> headerNames) {
        String user = "";
        String group = "";

        for (HttpHeader header : headerNames) {
            user = extractHeader(header.getId());
            if (!user.isEmpty()) {
                group = header.getId();
                break;
            }
        }

        return new ExtractorResult<String>(user, group);
    }
    
    
}
