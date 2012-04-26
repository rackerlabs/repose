package com.rackspace.papi.components.hnorm;

import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.hnorm.util.CompiledRegexAndList;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;

import com.rackspacecloud.api.docs.powerapi.header_normalization.v1.HttpHeader;
import com.rackspacecloud.api.docs.powerapi.header_normalization.v1.HttpMethod;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public class HeaderNormalizationHandler extends AbstractFilterLogicHandler {

    private List<CompiledRegexAndList> compiledTargets;

    HeaderNormalizationHandler(List<CompiledRegexAndList> compiledTargets) {
        this.compiledTargets = compiledTargets;
    }

    @Override
    public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
        final FilterDirector myDirector = new FilterDirectorImpl();
        myDirector.setFilterAction(FilterAction.PASS);

        String uri = request.getRequestURI();
        HttpMethod method = HttpMethod.valueOf(request.getMethod());

        for (CompiledRegexAndList target : compiledTargets) {
            if (target.getPattern().matcher(uri).matches()) {
                if (target.getMethodList().contains(method) || target.getMethodList().contains(HttpMethod.ALL)) {
                    myDirector.requestHeaderManager().headersToRemove().addAll(getHeadersToRemove(request, target));
                    break;
                }

            }
        }

        return myDirector;
    }

    private Set<String> getHeadersToRemove(HttpServletRequest request, CompiledRegexAndList target) {

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
