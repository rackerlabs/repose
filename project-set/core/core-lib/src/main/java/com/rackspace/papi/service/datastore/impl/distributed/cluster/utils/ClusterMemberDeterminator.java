/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.papi.service.datastore.impl.distributed.cluster.utils;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.ReposeCluster;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.datastore.impl.distributed.config.DistributedDatastoreConfiguration;
import com.rackspace.papi.service.datastore.impl.distributed.config.Port;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterMemberDeterminator {

   private static final Logger LOG = LoggerFactory.getLogger(ClusterMemberDeterminator.class);

   public static List<InetSocketAddress> getClusterMembers(SystemModel config, DistributedDatastoreConfiguration ddConfig, String clusterId) {

      final List<InetSocketAddress> cacheSiblings = new LinkedList<InetSocketAddress>();
      ReposeCluster cluster = getCurrentCluster(config.getReposeCluster(), clusterId);

      try {
         if (cluster != null) {
            for (Node node : cluster.getNodes().getNode()) {

               final InetAddress hostAddress = InetAddress.getByName(node.getHostname());
               final int port = getNodeDDPort(ddConfig, cluster.getId(), node.getId());
               final InetSocketAddress hostSocketAddress = new InetSocketAddress(hostAddress, port);
               cacheSiblings.add(hostSocketAddress);
            }
         }
      } catch (UnknownHostException ex) {
         LOG.error(ex.getMessage(), ex);
      }



      return cacheSiblings;

   }

   public static int getNodeDDPort(DistributedDatastoreConfiguration config, String clusterId, String nodeId) {

      int port = getDefaultDDPort(config, clusterId);
      for (Port curPort : config.getPortConfig().getPort()) {
         if (curPort.getCluster().equalsIgnoreCase(clusterId) && curPort.getNode().equalsIgnoreCase(nodeId)) {
            port = curPort.getPort();
            break;

         }
      }
      return port;
   }

   public static int getDefaultDDPort(DistributedDatastoreConfiguration config, String clusterId) {

      int port = -1;
      for (Port curPort : config.getPortConfig().getPort()) {
         if (curPort.getCluster().equalsIgnoreCase(clusterId) && curPort.getNode().equals("-1")) {
            port = curPort.getPort();
         }
      }
      return port;
   }

   public static ReposeCluster getCurrentCluster(List<ReposeCluster> clusters, String clusterId) {

      for (ReposeCluster cluster : clusters) {

         if (StringUtilities.nullSafeEquals(clusterId, cluster.getId())) {
            return cluster;
         }
      }

      return null;

   }
}
