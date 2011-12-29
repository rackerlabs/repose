package com.rackspace.papi.commons.util.http.header;

import com.rackspace.papi.commons.util.StringUtilities;
import java.util.Collections;
import java.util.Map;

/**
 *
 * @author zinic
 */
public class HeaderValueImpl implements HeaderValue {

   public static final String QUALITY_FACTOR_PARAM_NAME = "q";
   private final Map<String, String> parameters;
   private final String value;

   public HeaderValueImpl(String value, Map<String, String> parameters) {
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

   @Override
   public boolean equals(Object obj) {
      if (obj == null) {
         return false;
      }
      
      if (getClass() != obj.getClass()) {
         return false;
      }
      
      final HeaderValueImpl other = (HeaderValueImpl) obj;
      
      if (this.parameters != other.parameters && (this.parameters == null || !this.parameters.equals(other.parameters))) {
         return false;
      }
      
      if ((this.value == null) ? (other.value != null) : !this.value.equals(other.value)) {
         return false;
      }
      
      return true;
   }

   @Override
   public int hashCode() {
      int hash = 5;
      hash = 43 * hash + (this.parameters != null ? this.parameters.hashCode() : 0);
      hash = 43 * hash + (this.value != null ? this.value.hashCode() : 0);
      
      return hash;
   }
}
