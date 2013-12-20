package com.rackspace.papi.commons.util.thread;

import com.rackspace.papi.commons.util.Destroyable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DestroyableThreadWrapper implements Destroyable {

   private static final Logger LOG = LoggerFactory.getLogger(DestroyableThreadWrapper.class);
   private static final int WAIT_TIME = 15;

   public static <T extends Destroyable & Runnable> DestroyableThreadWrapper newThread(T threadLogic) {
      return new DestroyableThreadWrapper(new Thread(threadLogic), threadLogic);
   }
   private final Thread threadReference;
   private final Destroyable threadLogic;

   public DestroyableThreadWrapper(Thread threadReference, Destroyable threadLogic) {
      if (threadReference == null || threadLogic == null) {
         throw new IllegalArgumentException("References for creating a destroyable thread reference must not be null."
                 + "Thread Reference: " + threadReference + " - Thread Logic: " + threadLogic);
      }

      this.threadReference = threadReference;
      this.threadLogic = threadLogic;
   }

   public void start() {
      // Was it started?
      if (threadReference.getState() != Thread.State.NEW) {
         throw new IllegalStateException("Thread already started. Thread object: " + threadReference);
      }

      threadReference.start();
   }

   @Override
   public synchronized void destroy() {
      threadLogic.destroy();

      // Was it started?
      if (threadReference.getState() != Thread.State.NEW) {
         threadReference.interrupt();

         while (threadReference.getState() != Thread.State.TERMINATED) {
            try {
               wait(WAIT_TIME);
            } catch (InterruptedException ie) {
               LOG.error("Caught an interrupted exception while waiting for thread death.", ie);
               break;
            }
         }
      }
   }
}
