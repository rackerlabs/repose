/*
 *  Copyright 2010 Rackspace.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package com.rackspace.papi.commons.util.logging.apache.constraint;

import javax.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * 
 */
public class StatusCodeConstraint {
    private final Set<Integer> statusCodes;
    private final boolean isInclusivePass;

    public StatusCodeConstraint(boolean isExclusivePass) {
        this.isInclusivePass = isExclusivePass;

        statusCodes = new HashSet<Integer>();
    }

    public void addStatusCode(Integer statusCode) {
        statusCodes.add(statusCode);
    }

    public boolean pass(HttpServletResponse response) {

        int responseStatusCode = response.getStatus();

        return pass(isInclusivePass, responseStatusCode);
    }

    private boolean pass(boolean passedByDefault, int responseStatusCode) {
        boolean passed = !passedByDefault;

        for (int targetStatusCode : statusCodes) {
            if (responseStatusCode == targetStatusCode) {
                passed = !passed;
                break;
            }
        }

        return passed;
    }
}
