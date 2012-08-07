package org.openrepose.components.apivalidator.filter;

import com.rackspace.com.papi.components.checker.Validator;
import com.rackspace.com.papi.components.checker.step.Result;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiValidatorHandler extends AbstractFilterLogicHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiValidatorHandler.class);
    private final Map<String, ValidatorInfo> validators;
    private final ValidatorInfo defaultValidator;
    private FilterChain chain;

    public ApiValidatorHandler(ValidatorInfo defaultValidator, Map<String, ValidatorInfo> validators) {
        this.validators = new HashMap<String, ValidatorInfo>();
        if (validators != null) {
            this.validators.putAll(validators);
        }
        this.defaultValidator = defaultValidator;

    }

    public void setFilterChain(FilterChain chain) {
        this.chain = chain;
    }

    protected ValidatorInfo getValidatorForRole(List<? extends HeaderValue> roles) {

        if (validators != null) {
            for (String validatorRole : validators.keySet()) {
                for (HeaderValue role : roles) {
                    if (validatorRole.contains(role.getValue())) {
                        return validators.get(validatorRole);
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
        ValidatorInfo validator = getValidatorForRole(roles);

        if (validator != null) {
            myDirector.setFilterAction(FilterAction.RETURN);

            Validator v = validator.getValidator();
            if (v == null) {
                LOG.warn("Validator not available for request:", validator.getUri());
                myDirector.setResponseStatus(HttpStatusCode.BAD_GATEWAY);
            } else {
                try {
                    Result validate = v.validate(request, response, chain);
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
