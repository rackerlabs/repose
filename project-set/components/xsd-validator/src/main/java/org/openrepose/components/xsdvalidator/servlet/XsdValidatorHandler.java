package org.openrepose.components.xsdvalidator.servlet;

import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import com.rackspace.com.papi.components.checker.Validator;

public class XsdValidatorHandler extends AbstractFilterLogicHandler {

    private final List<Validator> validators;
    
    public XsdValidatorHandler(List<Validator> validators) {
        this.validators = validators;
    }

    @Override
    public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
        final FilterDirector myDirector = new FilterDirectorImpl();
        myDirector.setFilterAction(FilterAction.PASS);

        return myDirector;
    }
}
