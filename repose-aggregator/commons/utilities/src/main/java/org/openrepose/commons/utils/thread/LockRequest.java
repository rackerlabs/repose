package org.openrepose.commons.utils.thread;

public class LockRequest {

   private final Thread threadReference;
   private final Object lockKey;

   public LockRequest(Thread threadReference, Object threadKey) {
      this.threadReference = threadReference;
      this.lockKey = threadKey;
   }

   public Object getLockKey() {
      return lockKey;
   }

   public Thread getThreadReference() {
      return threadReference;
   }
}
