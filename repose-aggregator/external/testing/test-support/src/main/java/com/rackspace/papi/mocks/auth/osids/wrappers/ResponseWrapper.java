package com.rackspace.papi.mocks.auth.osids.wrappers;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.EndpointList;
import org.openstack.docs.identity.api.v2.ItemNotFoundFault;
import org.openstack.docs.identity.api.v2.UnauthorizedFault;

public interface ResponseWrapper {

   Object wrapElement(UnauthorizedFault fault);

   Object wrapElement(ItemNotFoundFault fault);

   Object wrapElement(Groups groups);

   Object wrapElement(AuthenticateResponse response);
   
   Object wrapElement(EndpointList endpointList);
}
