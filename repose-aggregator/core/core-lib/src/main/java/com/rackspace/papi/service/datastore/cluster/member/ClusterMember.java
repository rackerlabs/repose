package com.rackspace.papi.service.datastore.cluster.member;

import java.net.InetSocketAddress;

public class ClusterMember {

   private static final int REQUIRED_VALIDATION_PASSES = 4;
   private final InetSocketAddress memberAddress;
   private final int droppedMemberRestTime, requiredValidationPasses;
   private long droppedTime, restPeriod;
   private int validationPass;
   private boolean online;

   public ClusterMember(InetSocketAddress memberAddress, int droppedMemberRestTime) {
      this(REQUIRED_VALIDATION_PASSES, memberAddress, droppedMemberRestTime);
   }

   public ClusterMember(int requiredValidationPasses, InetSocketAddress memberAddress, int droppedMemberRestTime) {
      this.memberAddress = memberAddress;
      this.droppedMemberRestTime = droppedMemberRestTime;
      this.requiredValidationPasses = requiredValidationPasses;

      online = true;
      validationPass = 0;
   }

   private static long nowInMilliseconds() {
      return System.currentTimeMillis();
   }

   public InetSocketAddress getMemberAddress() {
      return memberAddress;
   }

   public boolean shouldRetry() {
      final long nowInMilliseconds = nowInMilliseconds();
      final boolean retry = nowInMilliseconds - droppedTime > restPeriod;

      if (retry) {
         logMemberRetry(nowInMilliseconds);
      }

      return retry;
   }

   private void logMemberRetry(long nowInMilliseconds) {
      if (validationPass++ < requiredValidationPasses) {
         restPeriod = droppedMemberRestTime / validationPass;
         droppedTime = nowInMilliseconds;
      } else {
         validationPass = 0;
         droppedTime = 0;

         online = true;
      }
   }

   public void setOffline() {
      droppedTime = nowInMilliseconds();
      restPeriod = droppedMemberRestTime;
      validationPass = 0;

      online = false;
   }

   public boolean isOnline() {
      return online;
   }

   public boolean isOffline() {
      return !online;
   }
}
