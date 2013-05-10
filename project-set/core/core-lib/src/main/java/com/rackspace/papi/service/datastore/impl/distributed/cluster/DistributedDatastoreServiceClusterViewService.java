package com.rackspace.papi.service.datastore.impl.distributed.cluster;

import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import com.rackspace.papi.service.datastore.impl.distributed.DatastoreAccessControl;
import java.net.InetSocketAddress;
import java.util.List;


public interface DistributedDatastoreServiceClusterViewService {
   
   public void updateClusterView(List<InetSocketAddress> cacheSiblings);
   
   public void updateAccessList(DatastoreAccessControl accessControl);
   
   public MutableClusterView getClusterView();
   
   public DatastoreAccessControl getAccessControl();
   
   public void initialize(MutableClusterView clusterView, DatastoreAccessControl datastoreAccessControl);
}
