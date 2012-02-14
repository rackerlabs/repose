package com.rackspace.papi.components.identity.header.extractor;

import com.rackspace.papi.components.identity.header.config.HttpHeader;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import com.rackspace.papi.commons.util.regex.ExtractorResult;

public class HeaderValueExtractor {

    private HttpServletRequest request;
    public static final String DEFAULT_QUALITY = "0.1";

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
                String quality = determineQuality(header);
                user += quality;
                group = header.getId() + quality;
                break;
            }
        }

        return new ExtractorResult<String>(user, group);
    }

    private String determineQuality(HttpHeader header) {
        String q = DEFAULT_QUALITY;

        if (header.getQuality() != null && !header.getQuality().trim().isEmpty()) {
            q = header.getQuality().trim();
        }

        return ";q=" + q;
    }
}
