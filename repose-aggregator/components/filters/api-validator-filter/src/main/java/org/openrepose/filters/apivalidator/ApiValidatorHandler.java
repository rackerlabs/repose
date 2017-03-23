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

import com.codahale.metrics.MetricRegistry;
import com.rackspace.com.papi.components.checker.Validator;
import com.rackspace.com.papi.components.checker.ValidatorException;
import com.rackspace.com.papi.components.checker.step.results.ErrorResult;
import com.rackspace.com.papi.components.checker.step.results.Result;
import com.rackspace.com.papi.components.checker.wadl.WADLException;
import org.openrepose.commons.utils.http.OpenStackServiceHeader;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.openrepose.core.services.reporting.metrics.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

public class ApiValidatorHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiValidatorHandler.class);
    private final List<ValidatorInfo> validators;
    private final ValidatorInfo defaultValidator;
    private Set<String> matchedRoles;
    private boolean multiRoleMatch = false;
    private boolean delegatingMode;
    private final Optional<MetricRegistry> metricRegistryOpt;

    public ApiValidatorHandler(
            ValidatorInfo defaultValidator,
            List<ValidatorInfo> validators,
            boolean multiRoleMatch,
            boolean delegatingMode,
            Optional<MetricsService> metricsService) {
        this.validators = new ArrayList<>(validators.size());
        this.matchedRoles = new HashSet<>();
        this.validators.addAll(validators);
        this.multiRoleMatch = multiRoleMatch;
        this.defaultValidator = defaultValidator;
        this.delegatingMode = delegatingMode;
        this.metricRegistryOpt = metricsService.map(MetricsService::getRegistry);
    }

    private boolean appendDefaultValidator(List<ValidatorInfo> validatorList) {
        if (defaultValidator != null) {
            if (!multiRoleMatch) {
                validatorList.add(defaultValidator);
            } else if (!validatorList.contains(defaultValidator)) {
                validatorList.add(0, defaultValidator);
            }

            return true;
        }

        return false;
    }

    protected List<ValidatorInfo> getValidatorsForRoles(List<String> listRoles) {
        Set<ValidatorInfo> validatorSet = new LinkedHashSet<>();
        Set<String> roles = new HashSet<>(listRoles);

        for (ValidatorInfo validator : validators) {
            for (String validatorRoles : validator.getRoles()) {
                if (roles.contains(validatorRoles)) {
                    validatorSet.add(validator);
                    matchedRoles.add(validatorRoles);
                }
            }
        }

        List<ValidatorInfo> validatorList = new ArrayList<>(validatorSet);
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

    private void sendMultiMatchErrorResponse(Result result, HttpServletResponse response) {
        try {
            ErrorResult error = getErrorResult(result);
            if (error != null && !delegatingMode) {
                response.setStatus(error.code());
                response.sendError(error.code(), error.message());
            }
        } catch (ValidatorException | WADLException | IOException e) {
            LOG.error("Some error", e);
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
        }
    }

    public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
        HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(request);
        List<String> roles = wrappedRequest.getPreferredSplittableHeaders(OpenStackServiceHeader.ROLES);
        if (roles.isEmpty()) {
            roles = Collections.singletonList("");
        }
        Result lastValidatorResult = null;
        boolean isValid = false;

        try {
            matchedRoles.clear();
            List<ValidatorInfo> matchedValidators = getValidatorsForRoles(roles);
            if (!matchedValidators.isEmpty()) {
                for (ValidatorInfo validatorInfo : matchedValidators) {

                    Validator validator = validatorInfo.getValidator();
                    if (validator == null) {
                        LOG.warn("Validator not available for request: {}", validatorInfo.getUri());
                        response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
                    } else {
                        lastValidatorResult = validator.validate(wrappedRequest, response, chain);
                        isValid = lastValidatorResult != null && lastValidatorResult.valid();
                        if (isValid) {
                            break;
                        }
                    }
                }

                if (!isValid) {
                    metricRegistryOpt.ifPresent(metricRegistry ->
                        matchedRoles.forEach(role ->
                            metricRegistry
                                .meter(MetricRegistry.name(
                                    ApiValidatorHandler.class,
                                    "invalid-request",
                                    role))
                                .mark()
                        )
                    );
                    if (multiRoleMatch) {
                        sendMultiMatchErrorResponse(lastValidatorResult, response);
                    }
                }
            } else {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
        } catch (ValidatorException | WADLException | IOException e) {
            LOG.error("Error processing validation", e);
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
        }
    }
}
