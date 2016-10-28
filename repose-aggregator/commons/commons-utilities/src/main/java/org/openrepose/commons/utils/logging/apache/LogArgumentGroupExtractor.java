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
package org.openrepose.commons.utils.logging.apache;

import org.apache.commons.lang3.StringUtils;
import org.openrepose.commons.utils.StringUtilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogArgumentGroupExtractor {

    private static final int HASH_BASE = 3;
    private static final int HASH_PRIME = 11;
    private String lifeCycleModifier;
    private String statusCodes;
    private String variable;
    private String variableArgumentSeparator;
    private String entity;
    private List<String> arguments;

    private LogArgumentGroupExtractor(String lifeCycleModifier, String statusCodes, String variable, String variableArgumentSeparator, String arguments, String entity) {
        this.lifeCycleModifier = lifeCycleModifier;
        this.statusCodes = statusCodes;
        this.variable = variable;
        this.variableArgumentSeparator = variableArgumentSeparator;
        this.arguments = parseArguments(arguments);
        this.entity = entity;
    }

    public LogArgumentGroupExtractor(Matcher m) {
        lifeCycleModifier = getGroupValue(m, LOG_CONSTANTS.LIFECYCLE_GROUP_INDEX);
        statusCodes = getGroupValue(m, LOG_CONSTANTS.STATUS_CODE_INDEX);
        variable = getGroupValue(m, LOG_CONSTANTS.VARIABLE_INDEX);
        variableArgumentSeparator = getGroupValue(m, LOG_CONSTANTS.VAR_ARG_SEPARATOR_INDEX);
        arguments = parseArguments(getGroupValue(m, LOG_CONSTANTS.ARGUMENTS_INDEX));
        entity = getGroupValue(m, LOG_CONSTANTS.ENTITY_INDEX);
    }

    public static LogArgumentGroupExtractor instance(String lifeCycleModifier, String statusCodes, String variable, String arguments, String entity) {
        return new LogArgumentGroupExtractor(lifeCycleModifier, statusCodes, variable, "", arguments, entity);
    }

    public static LogArgumentGroupExtractor stringEntity(String variable) {
        return new LogArgumentGroupExtractor("", "", variable, "", "", LogFormatArgument.STRING.name());
    }

    private List<String> parseArguments(String arguments) {
        List<String> result = new ArrayList<>();

        if (arguments != null) {
            Collections.addAll(result, arguments.split("[, ]"));
        }

        return result;
    }

    private String getGroupValue(Matcher m, int index) {
        String value = m.group(index);

        return value != null ? value : "";
    }

    public String getLifeCycleModifier() {
        return lifeCycleModifier;
    }

    public String getStatusCodes() {
        return statusCodes;
    }

    public String getVariable() {
        return variable;
    }

    public String getEntity() {
        return entity;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public String getFormat() {
        // everything that was inside the braces
        return variable + variableArgumentSeparator + StringUtils.join(arguments, " ");
    }

    @Override
    public boolean equals(Object o) {
        boolean result = false;

        if (o instanceof LogArgumentGroupExtractor) {
            LogArgumentGroupExtractor other = (LogArgumentGroupExtractor) o;

            result = StringUtilities.nullSafeEquals(other.entity, entity)
                    && StringUtilities.nullSafeEquals(other.lifeCycleModifier, lifeCycleModifier)
                    && StringUtilities.nullSafeEquals(other.statusCodes, statusCodes)
                    && other.arguments.equals(arguments)
                    && StringUtilities.nullSafeEquals(other.variable, variable);
        }

        return result;
    }

    @Override
    public int hashCode() {
        int hash = HASH_BASE;
        hash = HASH_PRIME * hash + StringUtilities.getValue(lifeCycleModifier, "").hashCode();
        hash = HASH_PRIME * hash + StringUtilities.getValue(statusCodes, "").hashCode();
        hash = HASH_PRIME * hash + StringUtilities.getValue(variable, "").hashCode();
        hash = HASH_PRIME * hash + arguments.hashCode();
        hash = HASH_PRIME * hash + StringUtilities.getValue(entity, "").hashCode();
        return hash;
    }

    public interface LOG_CONSTANTS {

        // Group 1
        String LIFECYCLE_MODIFIER_EXTRACTOR = "([<>])?";
        // Group 2, 3 (ignore)
        String STATUS_CODE_EXTRACTOR = "([!]?([0-9]{3}[,]?)*)?";
        // Group 4 (ignore), 5, 6, 7
        String VARIABLE_EXTRACTOR = "(\\{([\\-a-zA-Z0-9:.]*)([ ,]?)([_\\-a-zA-Z0-9 ,:.]*)\\})?";
        // Group 8
        String ENTITY_EXTRACTOR = "([%a-zA-Z])";
        Pattern PATTERN = Pattern.compile("%" + LIFECYCLE_MODIFIER_EXTRACTOR + STATUS_CODE_EXTRACTOR + VARIABLE_EXTRACTOR + ENTITY_EXTRACTOR);
        int LIFECYCLE_GROUP_INDEX = 1;
        int STATUS_CODE_INDEX = 2;
        int VARIABLE_INDEX = 5;
        int VAR_ARG_SEPARATOR_INDEX = 6;
        int ARGUMENTS_INDEX = 7;
        int ENTITY_INDEX = 8;
    }
}
