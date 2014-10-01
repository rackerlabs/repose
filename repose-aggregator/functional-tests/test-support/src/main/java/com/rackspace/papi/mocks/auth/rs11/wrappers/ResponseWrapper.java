package com.rackspace.papi.mocks.auth.rs11.wrappers;

import com.rackspacecloud.docs.auth.api.v1.FullToken;
import com.rackspacecloud.docs.auth.api.v1.GroupsList;
import com.rackspacecloud.docs.auth.api.v1.ItemNotFoundFault;
import com.rackspacecloud.docs.auth.api.v1.UnauthorizedFault;

public interface ResponseWrapper {

   Object wrapElement(GroupsList groups);

   Object wrapElement(FullToken token);

   Object wrapElement(UnauthorizedFault fault);

   Object wrapElement(ItemNotFoundFault fault);
   
}
