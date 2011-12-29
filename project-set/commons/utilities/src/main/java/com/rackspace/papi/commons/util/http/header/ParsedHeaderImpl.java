package com.rackspace.papi.commons.util.http.header;

import com.rackspace.papi.commons.util.StringUtilities;
import java.util.Collections;
import java.util.Map;

/**
 *
 * @author zinic
 */
public class ParsedHeaderImpl implements HeaderValue {

   public static final String QUALITY_FACTOR_PARAM_NAME = "q";
   
   private final Map<String, String> parameters;
   private final String value;

   public ParsedHeaderImpl(String value, Map<String, String> parameters) {
      this.parameters = parameters;
      this.value = value;
   }

   @Override
   public String getValue() {
      return value;
   }
   
   @Override
   public Map<String, String> getParameters() {
      return Collections.unmodifiableMap(parameters);
   }

   public boolean hasQualityFactor() {
      return parameters.containsKey(QUALITY_FACTOR_PARAM_NAME);
   }

   @Override
   public double getQualityFactor() {
      double qualityFactor = -1;

      final String qualityFactorString = parameters.get(QUALITY_FACTOR_PARAM_NAME);

      if (StringUtilities.isNotBlank(qualityFactorString)) {
         try {
            qualityFactor = Double.parseDouble(qualityFactorString);
         } catch (NumberFormatException nfe) {
            // TODO:Implement - Handle this exception
         }
      }

      return qualityFactor;
   }
}
