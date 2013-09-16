package com.rackspace.papi.service.datastore.impl.distributed.cluster;

import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import com.rackspace.papi.service.datastore.impl.distributed.DatastoreAccessControl;
import java.net.InetSocketAddress;
import java.util.List;


public interface DistributedDatastoreServiceClusterViewService {
   
   void updateClusterView(List<InetSocketAddress> cacheSiblings);
   void updateAccessList(DatastoreAccessControl accessControl);
   MutableClusterView getClusterView();
   DatastoreAccessControl getAccessControl();
   void initialize(MutableClusterView clusterView, DatastoreAccessControl datastoreAccessControl);
}
