package com.rackspace.papi.commons.util.logging.apache.constraint;

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

      statusCodes = new HashSet<Integer>();
   }

   public StatusCodeConstraint(String codes) {
      this.isInclusivePass = !codes.startsWith("!");

      statusCodes = new HashSet<Integer>();
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
