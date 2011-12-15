package com.rackspace.papi.commons.util.thread;

import com.rackspace.papi.commons.util.Destroyable;

/**
 *
 * Because I don't like Java Timer =/
 *
 * @author zinic
 */
public class Poller implements Runnable, Destroyable {

   private volatile boolean shouldContinue;
   private final long interval;
   private final RecurringTask task;
   private Thread taskThread;

   public Poller(RecurringTask task, long interval) {
      this.interval = interval;
      this.task = task;
      
      shouldContinue = true;
   }

   @Override
   public void run() {
      taskThread = Thread.currentThread();
      
      while (shouldContinue && !taskThread.isInterrupted()) {
         try {
            task.run();

            // Lock on our monitor
            synchronized (this) {
               wait(interval);
            }
         } catch (InterruptedException ie) {
            shouldContinue = false;
         }
      }
   }

   @Override
   public synchronized void destroy() {
      shouldContinue = false;
      
      // Notify and interrupt the task thread
      notify();
      taskThread.interrupt();
   }
}
