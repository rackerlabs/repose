package org.openrepose.components.apivalidator.filter;

import com.rackspace.com.papi.components.checker.Validator;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.OpenStackServiceHeader;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.http.header.HeaderValueImpl;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import java.util.List;
import java.util.ArrayList;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiValidatorHandler extends AbstractFilterLogicHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiValidatorHandler.class);
    private final List<ValidatorInfo> validators;
    private final ValidatorInfo defaultValidator;
    private FilterChain chain;

    public ApiValidatorHandler(ValidatorInfo defaultValidator, List<ValidatorInfo> validators) {
        this.validators = new ArrayList<ValidatorInfo>(validators.size());
        if (validators != null) {
            this.validators.addAll(validators);
        }
        this.defaultValidator = defaultValidator;

    }

    public void setFilterChain(FilterChain chain) {
        this.chain = chain;
    }

    protected ValidatorInfo getValidatorForRole(List<? extends HeaderValue> roles) {

        if (validators != null) {
            for (ValidatorInfo validator : validators) {
                for (HeaderValue role : roles) {
                   if (validator.getRole().equals(role.getValue())) {
                      return validator;
                    }
                }
            }
        }

        return defaultValidator;
    }

    @Override
    public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
        final FilterDirector myDirector = new FilterDirectorImpl();
        myDirector.setFilterAction(FilterAction.PASS);
        MutableHttpServletRequest mutableRequest = MutableHttpServletRequest.wrap(request);

        List<HeaderValue> roles = mutableRequest.getPreferredHeaderValues(OpenStackServiceHeader.ROLES.toString(), new HeaderValueImpl(""));
        ValidatorInfo validatorInfo = getValidatorForRole(roles);

        if (validatorInfo != null) {
            myDirector.setFilterAction(FilterAction.RETURN);

            Validator validator = validatorInfo.getValidator();
            if (validator == null) {
                LOG.warn("Validator not available for request:", validatorInfo.getUri());
                myDirector.setResponseStatus(HttpStatusCode.BAD_GATEWAY);
            } else {
                try {
                    validator.validate(request, response, chain);
                    myDirector.setResponseStatusCode(response.getStatus());
                } catch (Throwable t) {
                    LOG.error("Some error", t);
                    myDirector.setResponseStatus(HttpStatusCode.BAD_GATEWAY);
                }
            }
        }

        return myDirector;
    }
}
