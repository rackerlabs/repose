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
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

import static org.openrepose.commons.utils.logging.apache.LogConstants.*;

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
        lifeCycleModifier = getGroupValue(m, LIFECYCLE_GROUP_INDEX);
        statusCodes = getGroupValue(m, STATUS_CODE_INDEX);
        variable = getGroupValue(m, VARIABLE_INDEX);
        variableArgumentSeparator = getGroupValue(m, VAR_ARG_SEPARATOR_INDEX);
        arguments = parseArguments(getGroupValue(m, ARGUMENTS_INDEX));
        entity = getGroupValue(m, ENTITY_INDEX);
    }

    public static LogArgumentGroupExtractor instance(String lifeCycleModifier, String statusCodes, String variable, String arguments, String entity) {
        return new LogArgumentGroupExtractor(lifeCycleModifier, statusCodes, variable, "", arguments, entity);
    }

    public static LogArgumentGroupExtractor stringEntity(String variable) {
        return new LogArgumentGroupExtractor("", "", variable, "", "", LogFormatArgument.STRING);
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

            result = StringUtils.equals(other.entity, entity)
                    && StringUtils.equals(other.lifeCycleModifier, lifeCycleModifier)
                    && StringUtils.equals(other.statusCodes, statusCodes)
                    && other.arguments.equals(arguments)
                    && StringUtils.equals(other.variable, variable);
        }

        return result;
    }

    @Override
    public int hashCode() {
        int hash = HASH_BASE;
        hash = HASH_PRIME * hash + StringUtils.defaultIfEmpty(lifeCycleModifier, "").hashCode();
        hash = HASH_PRIME * hash + StringUtils.defaultIfEmpty(statusCodes, "").hashCode();
        hash = HASH_PRIME * hash + StringUtils.defaultIfEmpty(variable, "").hashCode();
        hash = HASH_PRIME * hash + arguments.hashCode();
        hash = HASH_PRIME * hash + StringUtils.defaultIfEmpty(entity, "").hashCode();
        return hash;
    }
}
