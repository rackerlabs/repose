package com.rackspace.papi.service.datastore.distributed.impl.distributed.cluster;

import com.rackspace.papi.components.datastore.distributed.ClusterView;
import com.rackspace.papi.service.datastore.DatastoreAccessControl;
import java.net.InetSocketAddress;
import java.util.List;


public interface DistributedDatastoreServiceClusterViewService {
   
   void updateClusterView(List<InetSocketAddress> cacheSiblings);
   void updateAccessList(DatastoreAccessControl accessControl);
   ClusterView getClusterView();
   DatastoreAccessControl getAccessControl();
}
