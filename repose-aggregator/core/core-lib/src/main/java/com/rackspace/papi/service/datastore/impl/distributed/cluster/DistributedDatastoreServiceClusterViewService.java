package com.rackspace.papi.service.datastore.impl.distributed.cluster;

import com.rackspace.papi.service.datastore.cluster.ClusterView;
import com.rackspace.papi.service.datastore.impl.distributed.DatastoreAccessControl;
import java.net.InetSocketAddress;
import java.util.List;


public interface DistributedDatastoreServiceClusterViewService {
   
   void updateClusterView(List<InetSocketAddress> cacheSiblings);
   void updateAccessList(DatastoreAccessControl accessControl);
   ClusterView getClusterView();
   DatastoreAccessControl getAccessControl();
   void initialize(ClusterView clusterView, DatastoreAccessControl datastoreAccessControl);
}
