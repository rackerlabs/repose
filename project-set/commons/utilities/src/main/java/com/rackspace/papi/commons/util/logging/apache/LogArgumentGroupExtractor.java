package com.rackspace.papi.commons.util.logging.apache;

import com.rackspace.papi.commons.util.StringUtilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogArgumentGroupExtractor {

    public interface LOG_CONSTANTS {

        // Group 1
        String LIFECYCLE_MODIFIER_EXTRACTOR = "([<>])?";
        // Group 2, 3 (ignore)
        String STATUS_CODE_EXTRACTOR = "([!]?([0-9]{3}[,]?)*)?";   
        // Group 4 (ignore), 5, 6 
        String VARIABLE_EXTRACTOR = "(\\{([\\-a-zA-Z0-9]*)[ ,]?([_\\-a-zA-Z0-9 ,]*)\\})?";  
        // Group 7
        String ENTITY_EXTRACTOR = "([%a-zA-Z])";                   
        Pattern PATTERN = Pattern.compile("%" + LIFECYCLE_MODIFIER_EXTRACTOR + STATUS_CODE_EXTRACTOR + VARIABLE_EXTRACTOR + ENTITY_EXTRACTOR);
        int LIFECYCLE_GROUP_INDEX = 1;
        int STATUS_CODE_INDEX = 2;
        int VARIABLE_INDEX = 5;
        int ARGUMENTS_INDEX = 6;
        int ENTITY_INDEX = 7;
    }
    private String lifeCycleModifier;
    private String statusCodes;
    private String variable;
    private String entity;
    private List<String> arguments;

    private LogArgumentGroupExtractor(String lifeCycleModifier, String statusCodes, String variable, String arguments, String entity) {
        this.lifeCycleModifier = lifeCycleModifier;
        this.statusCodes = statusCodes;
        this.variable = variable;
        this.arguments = parseArguments(arguments);
        this.entity = entity;
    }

    public static LogArgumentGroupExtractor instance(String lifeCycleModifier, String statusCodes, String variable, String arguments, String entity) {
        return new LogArgumentGroupExtractor(lifeCycleModifier, statusCodes, variable, arguments, entity);
    }

    public static LogArgumentGroupExtractor stringEntity(String variable) {
        return new LogArgumentGroupExtractor("", "", variable, "", LogFormatArgument.STRING.name());
    }

    public LogArgumentGroupExtractor(Matcher m) {
        lifeCycleModifier = getGroupValue(m, LOG_CONSTANTS.LIFECYCLE_GROUP_INDEX);
        statusCodes = getGroupValue(m, LOG_CONSTANTS.STATUS_CODE_INDEX);
        variable = getGroupValue(m, LOG_CONSTANTS.VARIABLE_INDEX);
        arguments = parseArguments(getGroupValue(m, LOG_CONSTANTS.ARGUMENTS_INDEX));
        entity = getGroupValue(m, LOG_CONSTANTS.ENTITY_INDEX);
    }

    private List<String> parseArguments(String arguments) {
        List<String> result = new ArrayList<String>();

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
    private static final int HASH_BASE = 3;
    private static final int HASH_PRIME = 11;

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
}
