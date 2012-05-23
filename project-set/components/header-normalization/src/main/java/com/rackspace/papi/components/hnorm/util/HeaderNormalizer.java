package com.rackspace.papi.components.hnorm.util;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;


public final class HeaderNormalizer {
    
    private HeaderNormalizer(){}
    
    public static Set<String> getHeadersToRemove(HttpServletRequest request, CompiledRegexAndList target) {

        final Enumeration<String> headerNames = request.getHeaderNames();
        Set<String> headersToRemove = new HashSet<String>();
        Set<String> filterList = target.getFilterList();
        String header;        
        
        while(headerNames.hasMoreElements()){
            header = headerNames.nextElement();
            headersToRemove.add(header.toLowerCase());
        }

        if(!target.isBlackList()){
            headersToRemove.removeAll(filterList);
        }else{
            headersToRemove.retainAll(filterList);
        }
        return headersToRemove;
    }
}
