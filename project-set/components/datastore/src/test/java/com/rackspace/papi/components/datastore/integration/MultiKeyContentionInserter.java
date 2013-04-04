package com.rackspace.papi.components.datastore.integration;

import com.rackspace.papi.commons.util.io.ObjectSerializer;
import com.rackspace.papi.components.datastore.hash.HashRingDatastoreManager;
import com.rackspace.papi.domain.Port;
import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.domain.ServicePorts;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import com.rackspace.papi.service.datastore.cluster.ThreadSafeClusterView;
import com.rackspace.papi.service.datastore.encoding.UUIDEncodingProvider;
import com.rackspace.papi.service.datastore.hash.MD5MessageDigestFactory;
import com.rackspace.papi.service.datastore.impl.ehcache.EHCacheDatastoreManager;
import com.rackspace.papi.service.proxy.jersey.RequestProxyServiceImpl;
import net.sf.ehcache.CacheManager;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class MultiKeyContentionInserter {

   public static final int MAX_KEYS = 50;
   public static final AtomicIntegerArray counterArray = new AtomicIntegerArray(MAX_KEYS);

   public static abstract class CacheRunnable implements Runnable {

      protected final int sleepDuration, keySpreadMax;
      protected final Datastore datastore;
      private boolean shouldContinue;
      private int key;

      public CacheRunnable(int sleepDuration, int keySpreadMax, Datastore datastore) {
         this.sleepDuration = sleepDuration;
         this.datastore = datastore;
         this.keySpreadMax = keySpreadMax;

         shouldContinue = true;
         key = keySpreadMax;
      }

      protected int nextKey() {
         if (++key >= keySpreadMax) {
            key = 0;
         }

         return key;
      }

      public synchronized void halt() {
         shouldContinue = false;
      }

      public synchronized boolean shouldContinue() {
         return shouldContinue;
      }
   }

   public static class CacheInserter extends CacheRunnable {

      public CacheInserter(int sleepDuration, int keySpreadMax, Datastore datastore) {
         super(sleepDuration, keySpreadMax, datastore);
      }

      @Override
      public void run() {
         while (shouldContinue()) {
            try {
               Thread.sleep(sleepDuration);

               final int nextKey = nextKey();
               final String nextCacheKey = String.valueOf(nextKey);
               final CacheableValue myValue = datastore.get(nextCacheKey).elementAs(CacheableValue.class);
               final CacheableValue putValue = myValue != null ? myValue : new CacheableValue(0, false);

               datastore.put(nextCacheKey, ObjectSerializer.instance().writeObject(putValue.next()), 300, TimeUnit.SECONDS);
               counterArray.incrementAndGet(nextKey);
            } catch (Exception ex) {
               ex.printStackTrace();
               break;
            }
         }
      }
   }

   public static class CacheReader extends CacheRunnable {

      public CacheReader(int sleepDuration, int keySpreadMax, Datastore datastore) {
         super(sleepDuration, keySpreadMax, datastore);
      }

      @Override
      public void run() {
         double averageDriftRatio = 1;

         while (shouldContinue()) {
            try {
               final int nextKey = nextKey();

               if (nextKey == 0) {
                  System.out.println("Average cache value drift ratio is: " + averageDriftRatio);
                  Thread.sleep(sleepDuration);
               }

               final String nextCacheKey = String.valueOf(nextKey);
               final CacheableValue myValue = datastore.get(nextCacheKey).elementAs(CacheableValue.class);
               final Integer expectedValue = counterArray.get(nextKey);

               if (myValue != null && expectedValue != null && myValue.getValue() > 0 && expectedValue > 0) {
                  final double keyDrift = ((double) myValue.getValue() / expectedValue);
                  averageDriftRatio = (averageDriftRatio + keyDrift) / 2;

//                  System.out.println("Anticipated cache value drift ratio for key \"" + nextCacheKey + "\" is " + keyDrift + " with actual of: " + myValue.getValue() + " and an expected of: " + expectedValue);
               }
            } catch (Exception ex) {
               ex.printStackTrace();
               break;
            }
         }
      }
   }

   private static ServicePorts getHttpPortList(int port) {
      ServicePorts ports = new ServicePorts();
      ports.add(new Port("http", port));
      return ports;
   }

   public static void main(String[] args) throws Exception {
      final ReposeInstanceInfo instanceInfo = new ReposeInstanceInfo("MultiKeyContentionInserter", "node");
      final MutableClusterView view = new ThreadSafeClusterView(getHttpPortList(20000));
      final EHCacheDatastoreManager localManager = new EHCacheDatastoreManager(new CacheManager());
      final HashRingDatastoreManager remoteManager = new HashRingDatastoreManager(new RequestProxyServiceImpl(instanceInfo), "", UUIDEncodingProvider.getInstance(), MD5MessageDigestFactory.getInstance(), view, localManager.getDatastore());
      final Datastore datastore = remoteManager.getDatastore();

      view.updateMembers(new InetSocketAddress[]{
                 new InetSocketAddress(InetAddress.getLocalHost(), 2101),
                 new InetSocketAddress(InetAddress.getLocalHost(), 2102),
                 new InetSocketAddress(InetAddress.getLocalHost(), 2103),
                 new InetSocketAddress(InetAddress.getLocalHost(), 2104)});


      for (int i = 0; i < MAX_KEYS; i++) {
         counterArray.set(i, 0);
         datastore.put(String.valueOf(i), ObjectSerializer.instance().writeObject(new CacheableValue(0, false)), 300, TimeUnit.SECONDS);
      }

      final CacheInserter cinserter1 = new CacheInserter(47, MAX_KEYS, datastore),
              cinserter2 = new CacheInserter(73, MAX_KEYS, datastore),
              cinserter3 = new CacheInserter(99, MAX_KEYS, datastore),
              cinserter4 = new CacheInserter(67, MAX_KEYS, datastore);

      final Thread inserter1 = new Thread(cinserter1),
              inserter2 = new Thread(cinserter2),
              inserter3 = new Thread(cinserter3),
              inserter4 = new Thread(cinserter4);

      final CacheReader creader = new CacheReader(5000, MAX_KEYS, datastore);
      final Thread reader = new Thread(creader);

      reader.start();

      inserter1.start();
      Thread.sleep(1300);
      inserter2.start();
      Thread.sleep(1300);
      inserter3.start();
      Thread.sleep(1300);
      inserter4.start();

      Thread.sleep(75000);
      cinserter1.halt();
      cinserter2.halt();
      cinserter3.halt();
      cinserter4.halt();

      Thread.sleep(5000);
      creader.halt();

      Thread.sleep(5000);

      System.out.println("Exiting...");
      System.exit(0);
   }
}
