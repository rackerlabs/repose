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

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class SingleKeyContentionInserter {

   private static class CacheInserterRunnable implements Runnable {

      private final int sleepDuration, finishTotal;
      private final Datastore datastore;
      private final String myKey;

      public CacheInserterRunnable(int sleepDuration, int finishTotal, Datastore datastore, String myKey) {
         this.sleepDuration = sleepDuration;
         this.finishTotal = finishTotal;
         this.datastore = datastore;
         this.myKey = myKey;
      }

      @Override
      public void run() {
         try {
            while (total < finishTotal) {
               try {
                  Thread.sleep(sleepDuration);
               } catch (InterruptedException ie) {
                  break;
               }

               final CacheableValue myValue = datastore.get(myKey).elementAs(CacheableValue.class);
               datastore.put(myKey, ObjectSerializer.instance().writeObject(myValue.next()), 300, TimeUnit.SECONDS);

               total++;
            }
         } catch (Exception ex) {
            ex.printStackTrace();
         }
      }
   }
   protected static volatile long total, beginTimestamp;

   private static ServicePorts getHttpPortList(int port) {
      ServicePorts ports = new ServicePorts();
      ports.add(new Port("http", port));
      return ports;
   }

   public static void main(String[] args) throws Exception {
      final ReposeInstanceInfo instanceInfo = new ReposeInstanceInfo("SingleKeyContentionInserter", "node");
      final MutableClusterView view = new ThreadSafeClusterView(getHttpPortList(20000));

      final EHCacheDatastoreManager localManager = new EHCacheDatastoreManager(new CacheManager());
      final HashRingDatastoreManager remoteManager = new HashRingDatastoreManager(new RequestProxyServiceImpl(instanceInfo), "", UUIDEncodingProvider.getInstance(), MD5MessageDigestFactory.getInstance(), view, localManager.getDatastore());
      final Datastore datastore = remoteManager.getDatastore();

      view.updateMembers(new InetSocketAddress[]{
                 new InetSocketAddress(InetAddress.getLocalHost(), 2101),
                 new InetSocketAddress(InetAddress.getLocalHost(), 2102),
                 new InetSocketAddress(InetAddress.getLocalHost(), 2103),
                 new InetSocketAddress(InetAddress.getLocalHost(), 2104)});

      final String myKey = "mykey";
      final int finishTotal = 9700,
              sleep1 = 100,
              sleep2 = 200,
              sleep3 = 1200,
              sleep4 = 300;

      total = 0;

      final Thread inserter1 = new Thread(new CacheInserterRunnable(sleep1, finishTotal, datastore, myKey)),
              inserter2 = new Thread(new CacheInserterRunnable(sleep2, finishTotal, datastore, myKey)),
              inserter3 = new Thread(new CacheInserterRunnable(sleep3, finishTotal, datastore, myKey)),
              inserter4 = new Thread(new CacheInserterRunnable(sleep4, finishTotal, datastore, myKey));

      final Thread reader = new Thread(new Runnable() {

         @Override
         public void run() {
            try {
               while (true) {
                  try {
                     Thread.sleep(400);
                  } catch (InterruptedException ie) {
                     break;
                  }

                  final CacheableValue myValue = datastore.get(myKey).elementAs(CacheableValue.class);
                  System.out.println("Acquired: " + myValue.getValue() + " out of " + total
                          + "\t\t\t\t- Potential Drift Ratio: " + ((double) myValue.getValue() / (double) total)
                          + "\t\t- Elapsed time: " + (System.currentTimeMillis() - beginTimestamp) + "ms");
               }
            } catch (Exception ex) {
               ex.printStackTrace();
            }
         }
      }, "Reader 1");

      Thread.sleep(4000);

      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final ObjectOutputStream oos = new ObjectOutputStream(baos);

      oos.writeObject(new CacheableValue(0, true));
      oos.close();

      beginTimestamp = System.currentTimeMillis();

      datastore.put(myKey, baos.toByteArray(), 200, TimeUnit.SECONDS);

      reader.start();
      inserter1.start();
      inserter2.start();
      inserter3.start();
      inserter4.start();

      Thread.sleep(200000);

      System.exit(0);
   }
}
