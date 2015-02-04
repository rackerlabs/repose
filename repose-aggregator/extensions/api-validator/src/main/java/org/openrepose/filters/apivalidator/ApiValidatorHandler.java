package org.openrepose.filters.apivalidator;

import com.rackspace.com.papi.components.checker.Validator;
import com.rackspace.com.papi.components.checker.step.ErrorResult;
import com.rackspace.com.papi.components.checker.step.Result;
import org.openrepose.commons.utils.http.OpenStackServiceHeader;
import org.openrepose.commons.utils.http.header.HeaderValue;
import org.openrepose.commons.utils.http.header.HeaderValueImpl;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.core.filter.logic.FilterAction;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler;
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl;
import org.openrepose.core.filters.ApiValidator;
import org.openrepose.core.services.reporting.metrics.MetricsService;
import org.openrepose.core.services.reporting.metrics.impl.MeterByCategorySum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ApiValidatorHandler extends AbstractFilterLogicHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiValidatorHandler.class);
    private final List<ValidatorInfo> validators;
    private final ValidatorInfo defaultValidator;
    private final MetricsService metricsService;
    private Set<String> matchedRoles;
    private FilterChain chain;
    private boolean multiRoleMatch = false;
    private MeterByCategorySum mbcsInvalidRequests;

    public ApiValidatorHandler(ValidatorInfo defaultValidator, List<ValidatorInfo> validators, boolean multiRoleMatch,
                               MetricsService metricsService) {
        this.validators = new ArrayList<ValidatorInfo>(validators.size());
        this.matchedRoles = new HashSet<String>();
        this.validators.addAll(validators);
        this.multiRoleMatch = multiRoleMatch;
        this.defaultValidator = defaultValidator;
        this.metricsService = metricsService;

        // TODO replace "api-validator" with filter-id or name-number in sys-model
        if (metricsService != null) {
            mbcsInvalidRequests = metricsService.newMeterByCategorySum(ApiValidator.class,
                    "api-validator", "InvalidRequest", TimeUnit.SECONDS);
        }
    }

    public void setFilterChain(FilterChain chain) {
        this.chain = chain;
    }

    private Set<String> getRolesAsSet(List<? extends HeaderValue> listRoles) {
        Set<String> roles = new HashSet();

        for (HeaderValue role : listRoles) {
            roles.add(role.getValue());
        }

        return roles;
    }

    private boolean appendDefaultValidator(List<ValidatorInfo> validatorList) {
        if (defaultValidator != null) {
            if (!multiRoleMatch) {
                validatorList.add(defaultValidator);
            }

            if (multiRoleMatch && !validatorList.contains(defaultValidator)) {
                validatorList.add(0, defaultValidator);
            }

            return true;
        }
        return false;
    }

    protected List<ValidatorInfo> getValidatorsForRole(List<? extends HeaderValue> listRoles) {
        List<ValidatorInfo> validatorList = new ArrayList<ValidatorInfo>();
        Set<String> roles = getRolesAsSet(listRoles);

        for (ValidatorInfo validator : validators) {
            for (String validatorRoles : validator.getRoles()) {
                if (roles.contains(validatorRoles)) {
                    validatorList.add(validator); // TODO Can the same validator be added multiple times?
                    matchedRoles.add(validatorRoles);
                }
            }
        }

        if (appendDefaultValidator(validatorList)) {
            matchedRoles.addAll(roles);
        }

        return !multiRoleMatch && !validatorList.isEmpty() ? validatorList.subList(0, 1) : validatorList;
    }

    private ErrorResult getErrorResult(Result lastResult) {
        if (lastResult instanceof ErrorResult) {
            return (ErrorResult) lastResult;
        }

        return null;
    }

    //The exceptions thrown by the validator are all custom exceptions which extend throwable
    @SuppressWarnings("PMD.AvoidCatchingThrowable")
    private void sendMultiMatchErrorResponse(Result result, final FilterDirector myDirector, HttpServletResponse response) {
        try {
            ErrorResult error = getErrorResult(result);
            if (error != null) {
                myDirector.setResponseStatusCode(error.code());
                response.sendError(error.code(), error.message());
            }
        } catch (Throwable t) {

            LOG.error("Some error", t);
            myDirector.setResponseStatusCode(HttpServletResponse.SC_BAD_GATEWAY);
        }
    }

    //The exceptions thrown by the validator are all custom exceptions which extend throwable
    @SuppressWarnings("PMD.AvoidCatchingThrowable")
    @Override
    public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
        final FilterDirector myDirector = new FilterDirectorImpl();
        myDirector.setFilterAction(FilterAction.PASS);
        MutableHttpServletRequest mutableRequest = MutableHttpServletRequest.wrap(request);
        List<HeaderValue> roles = mutableRequest.getPreferredHeaderValues(OpenStackServiceHeader.ROLES.toString(), new HeaderValueImpl(""));
        Result lastValidatorResult = null;
        boolean isValid = false;
        myDirector.setFilterAction(FilterAction.RETURN);

        try {
            matchedRoles.clear();
            List<ValidatorInfo> matchedValidators = getValidatorsForRole(roles);
            if (!matchedValidators.isEmpty()) {
                for (ValidatorInfo validatorInfo : matchedValidators) {

                    Validator validator = validatorInfo.getValidator();
                    if (validator == null) {
                        LOG.warn("Validator not available for request:", validatorInfo.getUri());
                        myDirector.setResponseStatusCode(HttpServletResponse.SC_BAD_GATEWAY);
                    } else {
                        lastValidatorResult = validator.validate(request, response, chain);
                        isValid = lastValidatorResult.valid();
                        myDirector.setResponseStatusCode(response.getStatus());
                        if (isValid) {
                            break;
                        }
                    }
                }

                if (!isValid) {
                    if (mbcsInvalidRequests != null) {
                        for (String s : matchedRoles) {
                            mbcsInvalidRequests.mark(s);
                        }
                    }
                    if (multiRoleMatch) {
                        sendMultiMatchErrorResponse(lastValidatorResult, myDirector, response);
                    }
                }
            } else {
                myDirector.setResponseStatusCode(HttpServletResponse.SC_FORBIDDEN);
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
            //TODO: Look back into this to see if we can avoid catching throwable
        } catch (Throwable t) {
            LOG.error("Error processing validation", t);
            myDirector.setResponseStatusCode(HttpServletResponse.SC_BAD_GATEWAY);
        }

        return myDirector;
    }
}
