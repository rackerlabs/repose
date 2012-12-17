package com.rackspace.repose.service.ratelimit.cache;

import java.util.Date;

/**
 * @author jhopper
 */
public class NextAvailableResponse {

   private final boolean hasRequests;
   private final Date resetTime;
   private final int currentLimitAmount;


   public NextAvailableResponse(boolean hasRequests, Date resetTime, int currentLimitAmount) {
      this.hasRequests = hasRequests;
      this.resetTime = resetTime;
      this.currentLimitAmount = currentLimitAmount;
   }

   public Date getResetTime() {
      return (Date)resetTime.clone();
   }

   public boolean hasRequestsRemaining() {
      return hasRequests;
   }

   public int getCurrentLimitAmount() {
      return currentLimitAmount;
   }
}
