package com.rackspace.papi.service.datastore.impl.distributed.cluster;

import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import com.rackspace.papi.service.datastore.cluster.ThreadSafeClusterView;
import com.rackspace.papi.service.datastore.impl.distributed.DatastoreAccessControl;
import java.net.InetSocketAddress;
import java.util.List;
import org.springframework.stereotype.Component;

@Component("clusterViewService")
public class DistributedDatastoreServiceClusterViewServiceImpl implements DistributedDatastoreServiceClusterViewService {
   
   private MutableClusterView clusterView;
   private DatastoreAccessControl accessControl;

   
   public DistributedDatastoreServiceClusterViewServiceImpl(){
   }
   
   @Override
   public void initialize(MutableClusterView clusterView, DatastoreAccessControl accessControl){
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
   public  MutableClusterView getClusterView() {
      return clusterView;
   }

   @Override
   public DatastoreAccessControl getAccessControl() {
      return accessControl;
   }
   
   
   
}
