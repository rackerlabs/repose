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
package org.openrepose.commons.utils.logging.apache.constraint;

import javax.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class StatusCodeConstraint {

    private static final Pattern STATUS_CODE_RX = Pattern.compile(",");
    private final Set<Integer> statusCodes;
    private final boolean isInclusivePass;

    public StatusCodeConstraint(boolean isExclusivePass) {
        this.isInclusivePass = isExclusivePass;

        statusCodes = new HashSet<>();
    }

    public StatusCodeConstraint(String codes) {
        this.isInclusivePass = !codes.startsWith("!");

        statusCodes = new HashSet<>();
        for (String st : STATUS_CODE_RX.split(removeNegation(codes))) {
            statusCodes.add(Integer.parseInt(st));
        }
    }

    private String removeNegation(String codes) {
        return codes.startsWith("!") ? codes.substring(1) : codes;
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
