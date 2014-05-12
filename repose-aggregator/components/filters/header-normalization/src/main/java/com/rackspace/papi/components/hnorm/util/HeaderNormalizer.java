package com.rackspace.papi.components.hnorm.util;

import com.rackspace.papi.commons.util.http.header.HeaderName;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;


public final class HeaderNormalizer {
    
    private HeaderNormalizer(){}
    
    public static Set<HeaderName> getHeadersToRemove(HttpServletRequest request, CompiledRegexAndList target) {

        final Enumeration<String> headerNames = request.getHeaderNames();
        Set<HeaderName> headersToRemove = new HashSet<HeaderName>();
        Set<HeaderName> filterList = target.getFilterList();
        String header;        
        
        while(headerNames.hasMoreElements()){
            header = headerNames.nextElement();
            headersToRemove.add(new HeaderName(header));
        }

        if(!target.isBlackList()){
            headersToRemove.removeAll(filterList);
        }else{
            headersToRemove.retainAll(filterList);
        }
        return headersToRemove;
    }
}
