package com.rackspace.papi.commons.util.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class KeyedStackLock {

   private static final Logger LOG = LoggerFactory.getLogger(KeyedStackLock.class);
   private final Set<Thread> threadsHoldingLock;
   private boolean locked;
   private int waitDepth;
   private Object currentKey;

   public KeyedStackLock() {
      waitDepth = 0;
      locked = false;
      currentKey = null;

      threadsHoldingLock = new HashSet<Thread>();
   }

   public boolean isLocked() {
      return locked;
   }

   public void lock(Object key) {
      try {
         lockInterruptibly(key);
      } catch (InterruptedException ie) {
         String keyString = (key != null ? key.toString() : "UNDEFINED");
         LOG.warn("failed lock attempt using key: " + keyString, ie);

         Thread.currentThread().interrupt();
      }
   }

   public synchronized boolean tryLock(Object key) {
      if (!locked || currentKey.equals(key)) {
         registerThread(new LockRequest(Thread.currentThread(), key));

         return true;
      }

      return false;
   }

   public synchronized void lockInterruptibly(Object key) throws InterruptedException {
      final LockRequest qlr = new LockRequest(Thread.currentThread(), key);

      if (waitDepth > 0 || (locked && !currentKey.equals(key))) {
         unsafeWaitOnLock(qlr);
      }

      registerThread(qlr);
   }

   /**
    * WARNING! WARNING! WARNING!
    *
    * This method expects that the object monitor is already captured by the caller's thread.
    *
    * @param lockRequest lockRequest
    * @throws InterruptedException InterruptedException
    */
   private void unsafeWaitOnLock(LockRequest lockRequest) throws InterruptedException {
      do {
         waitDepth++;
         wait();
         waitDepth--;
      } while (locked && currentKey != lockRequest.getLockKey());
   }

   private void clearLockStatus() {
      currentKey = null;
      locked = false;
   }

   private void registerThread(LockRequest qlr) {
      LOG.debug("Registering thread: " + qlr.getThreadReference().toString());

      if (!threadsHoldingLock.add(qlr.getThreadReference())) {
         LOG.warn("failed thread registration [lockKey: " + qlr.getLockKey() + "]");
      }

      if (!locked) {
         locked = true;
         currentKey = qlr.getLockKey();
      }
   }

   public synchronized void unlock(Object key) {
      if (!locked) {
         throw new IllegalStateException("Keyed lock is not currently locked");
      }

      if (!currentKey.equals(key)) {
         throw new IllegalArgumentException("Key does not match the key used to hold the lock");
      }

      final Thread currentThreadReference = Thread.currentThread();

      if (!threadsHoldingLock.remove(currentThreadReference)) {
         throw new IllegalMonitorStateException("Thread reference: "
                 + currentThreadReference.getName()
                 + " does not have a keyed lock on this lock");
      }

      if (threadsHoldingLock.isEmpty()) {
         clearLockStatus();
      }

      if (waitDepth > 0) {
         notify();
      }
   }
}
