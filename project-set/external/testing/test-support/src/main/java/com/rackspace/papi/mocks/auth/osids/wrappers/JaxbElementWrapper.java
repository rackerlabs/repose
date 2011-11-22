package com.rackspace.papi.mocks.auth.osids.wrappers;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.ItemNotFoundFault;
import org.openstack.docs.identity.api.v2.ObjectFactory;
import org.openstack.docs.identity.api.v2.UnauthorizedFault;

public class JaxbElementWrapper implements ResponseWrapper {
   private final ObjectFactory objectFactory;
   private final com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory groupsObjectFactory;
   
   public JaxbElementWrapper() {
      objectFactory = new ObjectFactory();
      groupsObjectFactory = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory();
      
   }
   
   @Override
   public Object wrapElement(UnauthorizedFault fault) {
      return objectFactory.createUnauthorized(fault);
   }
   
   @Override
   public Object wrapElement(ItemNotFoundFault fault) {
      return objectFactory.createItemNotFound(fault);
   }
   
   @Override
   public Object wrapElement(Groups groups) {
      return groupsObjectFactory.createGroups(groups);
   }
   
   @Override
   public Object wrapElement(AuthenticateResponse response) {
      return objectFactory.createAccess(response);
   }
   
}
