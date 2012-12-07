package com.rackspace.repose.service.ratelimit.exception;

import java.util.Date;

public class OverLimitException extends Exception {

   private final String user;
   private final Date nextAvailableTime;
   private final int currentLimitAmount;
   private final String configuredLimit;


   public OverLimitException(String msg, String user, Date nextAvailableTime, int currentLimitAmount, String configuredLimit) {
      super(msg);
      this.user = user;
      this.nextAvailableTime = (Date)nextAvailableTime.clone();
      this.currentLimitAmount = currentLimitAmount;
      this.configuredLimit = configuredLimit;
   }

   public String getUser() {
      return user;
   }

   public Date getNextAvailableTime() {
      return (Date)nextAvailableTime.clone();
   }

   public int getCurrentLimitAmount() {
      return currentLimitAmount;
   }

   public String getConfiguredLimit() {
      return configuredLimit;
   }
}
