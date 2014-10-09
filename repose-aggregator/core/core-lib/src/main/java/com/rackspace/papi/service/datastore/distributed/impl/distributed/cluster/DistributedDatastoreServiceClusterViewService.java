package com.rackspace.papi.service.datastore.distributed.impl.distributed.cluster;

import org.openrepose.services.datastore.api.distributed.ClusterView;
import org.openrepose.services.datastore.api.DatastoreAccessControl;
import java.net.InetSocketAddress;
import java.util.List;


public interface DistributedDatastoreServiceClusterViewService {
   
   void updateClusterView(List<InetSocketAddress> cacheSiblings);
   void updateAccessList(DatastoreAccessControl accessControl);
   ClusterView getClusterView();
   DatastoreAccessControl getAccessControl();
   void initialize(ClusterView clusterView, DatastoreAccessControl datastoreAccessControl);
}
