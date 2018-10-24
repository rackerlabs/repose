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
package org.openrepose.commons.utils.logging.apache.format.stock;

import org.openrepose.commons.utils.logging.apache.format.FormatterLogic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.DecimalFormat;


public class ResponseTimeHandler implements FormatterLogic {

    public static final String START_TIME_ATTRIBUTE = "org.openrepose.repose.logging.start.time";
    private final DecimalFormat decimalFormat = new DecimalFormat("#.##");
    private double multiplier;

    public ResponseTimeHandler(double multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public String handle(HttpServletRequest request, HttpServletResponse response) {
        Object startTime = request.getAttribute(START_TIME_ATTRIBUTE);
        String responseTime = "";

        if (startTime != null) {
            responseTime = decimalFormat.format((System.currentTimeMillis() - (Long) startTime) * multiplier);
        }

        return responseTime;
    }
}
