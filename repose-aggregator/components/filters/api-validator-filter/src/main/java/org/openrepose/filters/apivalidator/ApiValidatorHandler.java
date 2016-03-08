/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.filters.apivalidator;

import com.rackspace.com.papi.components.checker.Validator;
import com.rackspace.com.papi.components.checker.step.results.ErrorResult;
import com.rackspace.com.papi.components.checker.step.results.Result;
import org.openrepose.commons.utils.http.OpenStackServiceHeader;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.openrepose.core.filters.ApiValidator;
import org.openrepose.core.services.reporting.metrics.MetricsService;
import org.openrepose.core.services.reporting.metrics.MeterByCategorySum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ApiValidatorHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiValidatorHandler.class);
    private final List<ValidatorInfo> validators;
    private final ValidatorInfo defaultValidator;
    private final MetricsService metricsService;
    private Set<String> matchedRoles;
    private FilterChain chain;
    private boolean multiRoleMatch = false;
    private boolean delegatingMode;
    private MeterByCategorySum mbcsInvalidRequests;

    public ApiValidatorHandler(ValidatorInfo defaultValidator, List<ValidatorInfo> validators, boolean multiRoleMatch,
                               boolean delegatingMode, MetricsService metricsService) {
        this.validators = new ArrayList<>(validators.size());
        this.matchedRoles = new HashSet<>();
        this.validators.addAll(validators);
        this.multiRoleMatch = multiRoleMatch;
        this.defaultValidator = defaultValidator;
        this.delegatingMode = delegatingMode;
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

    protected List<ValidatorInfo> getValidatorsForRole(List<String> listRoles) {
        List<ValidatorInfo> validatorList = new ArrayList<>();
        Set<String> roles = new HashSet<>(listRoles);

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

    // Until API Validator is updated to not throw the generic Throwable, this method will need to catch it.
    private void sendMultiMatchErrorResponse(Result result, HttpServletResponse response) {
        try {
            ErrorResult error = getErrorResult(result);
            if (error != null && !delegatingMode) {
                response.setStatus(error.code());
                response.sendError(error.code(), error.message());
            }
        } catch (Throwable t) {
            LOG.error("Some error", t);
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
        }
    }

    // Until API Validator is updated to not throw the generic Throwable, this method will need to catch it.
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) {
        HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(request);
        List<String> roles = wrappedRequest.getPreferredSplittableHeaders(OpenStackServiceHeader.ROLES.toString());
        if (roles.isEmpty()) {
            roles = Collections.singletonList("");
        }
        Result lastValidatorResult = null;
        boolean isValid = false;

        try {
            matchedRoles.clear();
            List<ValidatorInfo> matchedValidators = getValidatorsForRole(roles);
            if (!matchedValidators.isEmpty()) {
                for (ValidatorInfo validatorInfo : matchedValidators) {

                    Validator validator = validatorInfo.getValidator();
                    if (validator == null) {
                        LOG.warn("Validator not available for request: {}", validatorInfo.getUri());
                        response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
                    } else {
                        lastValidatorResult = validator.validate(wrappedRequest, response, chain);
                        isValid = lastValidatorResult.valid();
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
                        sendMultiMatchErrorResponse(lastValidatorResult, response);
                    }
                }
            } else {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
            //TODO: Look back into this to see if we can avoid catching throwable
        } catch (Throwable t) {
            LOG.error("Error processing validation", t);
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
        }
    }
}
