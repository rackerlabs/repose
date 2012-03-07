package com.rackspace.papi.commons.util.logging.apache;

import com.rackspace.papi.commons.util.StringUtilities;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogArgumentGroupExtractor {

   private static final String LIFECYCLE_MODIFIER_EXTRACTOR = "([<>])?";           // Group 1
   private static final String STATUS_CODE_EXTRACTOR = "([!]?([0-9]{3}[,]?)*)?";   // Group 2, 3 (ignore)
   private static final String VARIABLE_EXTRACTOR = "(\\{([\\-a-zA-Z0-9]*)\\})?";  // Group 4 (ignore), 5
   private static final String ENTITY_EXTRACTOR = "([%a-zA-Z])";                   // Group 6
   public static final Pattern PATTERN = Pattern.compile("%" + LIFECYCLE_MODIFIER_EXTRACTOR + STATUS_CODE_EXTRACTOR + VARIABLE_EXTRACTOR + ENTITY_EXTRACTOR);
   private static final int LIFECYCLE_GROUP_INDEX = 1;
   private static final int STATUS_CODE_INDEX = 2;
   private static final int VARIABLE_INDEX = 5;
   private static final int ENTITY_INDEX = 6;
   private String lifeCycleModifier;
   private String statusCodes;
   private String variable;
   private String entity;

   private LogArgumentGroupExtractor(String lifeCycleModifier, String statusCodes, String variable, String entity) {
      this.lifeCycleModifier = lifeCycleModifier;
      this.statusCodes = statusCodes;
      this.variable = variable;
      this.entity = entity;
   }

   public static LogArgumentGroupExtractor instance(String lifeCycleModifier, String statusCodes, String variable, String entity) {
      return new LogArgumentGroupExtractor(lifeCycleModifier, statusCodes, variable, entity);
   }

   public LogArgumentGroupExtractor(Matcher m) {
      lifeCycleModifier = getGroupValue(m, LIFECYCLE_GROUP_INDEX);
      statusCodes = getGroupValue(m, STATUS_CODE_INDEX);
      variable = getGroupValue(m, VARIABLE_INDEX);
      entity = getGroupValue(m, ENTITY_INDEX);
   }
   
   private String getGroupValue(Matcher m, int index) {
      String value = m.group(index);
      
      return value != null? value: "";
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

   @Override
   public boolean equals(Object o) {
      boolean result = false;

      if (o instanceof LogArgumentGroupExtractor) {
         LogArgumentGroupExtractor other = (LogArgumentGroupExtractor) o;

         result = StringUtilities.nullSafeEquals(other.entity, entity)
                 && StringUtilities.nullSafeEquals(other.lifeCycleModifier, lifeCycleModifier)
                 && StringUtilities.nullSafeEquals(other.statusCodes, statusCodes)
                 && StringUtilities.nullSafeEquals(other.variable, variable);

      }

      return result;
   }

   @Override
   public int hashCode() {
      int hash = 3;
      hash = 11 * hash + StringUtilities.getValue(lifeCycleModifier, "").hashCode();
      hash = 11 * hash + StringUtilities.getValue(statusCodes, "").hashCode();
      hash = 11 * hash + StringUtilities.getValue(variable, "").hashCode();
      hash = 11 * hash + StringUtilities.getValue(entity, "").hashCode();
      return hash;
   }
}
