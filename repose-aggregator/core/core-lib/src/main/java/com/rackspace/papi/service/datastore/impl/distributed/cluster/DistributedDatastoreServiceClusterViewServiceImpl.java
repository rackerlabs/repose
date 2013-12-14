package com.rackspace.papi.service.datastore.impl.distributed.cluster;

import com.rackspace.papi.service.datastore.cluster.ClusterView;
import com.rackspace.papi.service.datastore.impl.distributed.DatastoreAccessControl;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.List;

@Component("clusterViewService")
public class DistributedDatastoreServiceClusterViewServiceImpl implements DistributedDatastoreServiceClusterViewService {
   
   private ClusterView clusterView;
   private DatastoreAccessControl accessControl;

   
   public DistributedDatastoreServiceClusterViewServiceImpl(){
   }
   
   @Override
   public void initialize(ClusterView clusterView, DatastoreAccessControl accessControl){
      this.clusterView = clusterView;
      this.accessControl = accessControl;
   }
   
   
   @Override
   public void updateClusterView(List<InetSocketAddress> cacheSiblings) {
      clusterView.updateMembers(cacheSiblings.toArray(new InetSocketAddress[cacheSiblings.size()]));
   }

   @Override
   public void updateAccessList(DatastoreAccessControl accessControl) {
      this.accessControl = accessControl;
   }

   @Override
   public ClusterView getClusterView() {
      return clusterView;
   }

   @Override
   public DatastoreAccessControl getAccessControl() {
      return accessControl;
   }
   
   
   
}
