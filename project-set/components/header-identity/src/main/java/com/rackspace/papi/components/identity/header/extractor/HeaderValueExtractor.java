package com.rackspace.papi.components.identity.header.extractor;

import com.rackspace.papi.components.identity.header.config.HttpHeader;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import com.rackspace.papi.commons.util.regex.ExtractorResult;

// NOTE: This is a sun class we use for IP validation.  If a customer eventually wants to run Repose on a JVM
// that does not include the IPAddressUtil class, we can look in our git history and resurrect the copy we had
// included in our project or perhaps find another way at that time to validate IP address in a JVM independent
// fashion.
public class HeaderValueExtractor {

    private HttpServletRequest request;

    public HeaderValueExtractor(HttpServletRequest request) {
        this.request = request;
    }

    protected String extractHeader(String name) {
        // Header value may be a comma separated list of IP addresses.
        // The client IP should be the left most IP address in the list.
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
