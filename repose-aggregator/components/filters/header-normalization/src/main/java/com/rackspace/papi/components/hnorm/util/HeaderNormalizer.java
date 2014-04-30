package com.rackspace.papi.components.hnorm.util;

import com.rackspace.papi.commons.util.http.header.HeaderNameStringWrapper;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;


public final class HeaderNormalizer {
    
    private HeaderNormalizer(){}
    
    public static Set<HeaderNameStringWrapper> getHeadersToRemove(HttpServletRequest request, CompiledRegexAndList target) {

        final Enumeration<String> headerNames = request.getHeaderNames();
        Set<HeaderNameStringWrapper> headersToRemove = new HashSet<HeaderNameStringWrapper>();
        Set<HeaderNameStringWrapper> filterList = target.getFilterList();
        String header;        
        
        while(headerNames.hasMoreElements()){
            header = headerNames.nextElement();
            headersToRemove.add(new HeaderNameStringWrapper(header));
        }

        if(!target.isBlackList()){
            headersToRemove.removeAll(filterList);
        }else{
            headersToRemove.retainAll(filterList);
        }
        return headersToRemove;
    }
}
