package org.openrepose.nodeservice.distributed.cluster.utils;

import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.core.systemmodel.Node;
import org.openrepose.core.systemmodel.ReposeCluster;
import org.openrepose.core.systemmodel.SystemModel;
import org.openrepose.core.services.datastore.distributed.config.DistributedDatastoreConfiguration;
import org.openrepose.core.services.datastore.distributed.config.Port;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

/**
 * A handful of convienience methods around determining the members of a cluster for a Distributed Datastore
 */
public class ClusterMemberDeterminator {

   private static final Logger LOG = LoggerFactory.getLogger(ClusterMemberDeterminator.class);

   /**
    * Get a list of all the cluster members for a specified cluster ID
    * You'll get the Port and inet address for those hosts.
    * @param config
    * @param ddConfig
    * @param clusterId
    * @return
    */
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

   /**
    * Get the DD port that this node is going to use.
    * @param config
    * @param clusterId
    * @param nodeId
    * @return
    */
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

   /**
    * The "default" dd port is always -1, because it is a required configuration. The port should never end up
    * being -1. I'm not sure why this is even here ...
    * @param config
    * @param clusterId
    * @return
    */
   public static int getDefaultDDPort(DistributedDatastoreConfiguration config, String clusterId) {
      int port = -1;
      for (Port curPort : config.getPortConfig().getPort()) {
         if (curPort.getCluster().equalsIgnoreCase(clusterId) && "-1".equals(curPort.getNode())) {
            port = curPort.getPort();
         }
      }
      return port;
   }

   /**
    * Just gets the cluster object by cluster name without having to have a nodeID
    * @param clusters
    * @param clusterId
    * @return
    */
   public static ReposeCluster getCurrentCluster(List<ReposeCluster> clusters, String clusterId) {
      for (ReposeCluster cluster : clusters) {
         if (StringUtilities.nullSafeEquals(clusterId, cluster.getId())) {
            return cluster;
         }
      }
      return null;
   }
}
