package com.rackspace.papi.commons.util.pooling;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GenericBlockingResourcePool<R> implements Pool<R> {

   private final ConstructionStrategy<R> constructor;
   private final Queue<R> pool;
   private final Condition poolHasResources;
   private final Lock poolLock;
   private int maxPoolSize;
   private int checkoutCounter;

   public GenericBlockingResourcePool(ConstructionStrategy<R> constructor) {
      this(constructor, DEFAULT_MIN_POOL_SIZE, DEFAULT_MAX_POOL_SIZE);
   }

   public GenericBlockingResourcePool(ConstructionStrategy<R> constructor, int minPoolSize, int maxPoolSize) {
      this.constructor = constructor;

      checkoutCounter = 0;

      pool = new LinkedList<R>();
      poolLock = new ReentrantLock(true);
      poolHasResources = poolLock.newCondition();

      resizeMinimum(minPoolSize);
      resizeMaximum(maxPoolSize);
   }

   @Override
   public int size() {
      try {
         poolLock.lock();

         return checkoutCounter + pool.size();
      } finally {
         poolLock.unlock();
      }
   }

   @Override
   public void setMaximumPoolSize(int newSize) {
      resizeMaximum(newSize);
   }

   @Override
   public void setMinimumPoolSize(int newSize) {
      resizeMinimum(newSize);
   }

   @Override
   public <T> T use(ResourceContext<R, T> newContext) {
      final R resource = checkout();

      try {
         return newContext.perform(resource);
      } finally {
         checkin(resource);
      }
   }

   @Override
   public void use(SimpleResourceContext<R> newContext) {
      final R resource = checkout();

      try {
         newContext.perform(resource);
      } finally {
         checkin(resource);
      }
   }

   private void resizeMaximum(int newSize) {
      try {
         poolLock.lock();

         maxPoolSize = newSize;

         while (pool.size() + checkoutCounter > maxPoolSize && !pool.isEmpty()) {
            pool.poll();
         }
      } finally {
         poolLock.unlock();
      }
   }

   private void resizeMinimum(final int newMinPoolSize) {
      try {
         poolLock.lock();

         while (checkoutCounter + pool.size() < newMinPoolSize) {
            pool.add(constructor.construct());
         }
      } finally {
         poolLock.unlock();
      }
   }

   private void checkin(R resource) {
      try {
         poolLock.lock();

         if (pool.size() + checkoutCounter < maxPoolSize) {
            pool.add(resource);
            poolHasResources.signal();
         }

         checkoutCounter--;
      } finally {
         poolLock.unlock();
      }
   }

   private R checkout() {
      try {
         poolLock.lock();

         R resource;

         if (pool.isEmpty() && checkoutCounter != maxPoolSize) {
            resource = constructor.construct();
         } else {
            while (pool.isEmpty()) {
               poolHasResources.await();
            }

            resource = pool.poll();
         }

         checkoutCounter++;
         return resource;
      } catch (InterruptedException ie) {
         throw new ResourceAccessException("Interrupted while waiting for a resource to be checked in", ie);
      } finally {
         poolLock.unlock();
      }
   }
}
