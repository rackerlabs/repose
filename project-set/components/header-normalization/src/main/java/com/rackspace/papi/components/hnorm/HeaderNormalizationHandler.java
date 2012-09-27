package com.rackspace.papi.components.hnorm;

import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.hnorm.util.CompiledRegexAndList;
import com.rackspace.papi.components.hnorm.util.HeaderNormalizer;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import com.rackspacecloud.api.docs.repose.header_normalization.v1.HttpMethod;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

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
            if (target.getPattern().matcher(uri).matches() && (target.getMethodList().contains(method) || target.getMethodList().contains(HttpMethod.ALL))) {
                myDirector.requestHeaderManager().headersToRemove().addAll(HeaderNormalizer.getHeadersToRemove(request, target));
                break;

            }
        }

        return myDirector;
    }
}
