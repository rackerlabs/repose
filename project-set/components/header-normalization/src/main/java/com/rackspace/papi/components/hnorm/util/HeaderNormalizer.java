
package com.rackspace.papi.components.hnorm.util;

import com.rackspacecloud.api.docs.powerapi.header_normalization.v1.HttpHeader;
import javax.servlet.http.HttpServletRequest;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;


public final class HeaderNormalizer {
    
    private HeaderNormalizer(){}
    
    public static Set<String> getHeadersToRemove(HttpServletRequest request, CompiledRegexAndList target) {

        final Enumeration<String> headerNames = request.getHeaderNames();
        Set<String> headersToRemove = new HashSet<String>();
        String header;
        while (headerNames.hasMoreElements()) {
            header = headerNames.nextElement();
            boolean found = false;
            
            for (HttpHeader headerFilter : target.getHeaderList()) {
                if (headerFilter.getId().equalsIgnoreCase(header)) {
                    found = true;
                    break;
                }
            }

            if (found && target.isBlackList() || !found && !target.isBlackList()) {
                headersToRemove.add(header);
            }
        }
        
        return headersToRemove;
    }
}
