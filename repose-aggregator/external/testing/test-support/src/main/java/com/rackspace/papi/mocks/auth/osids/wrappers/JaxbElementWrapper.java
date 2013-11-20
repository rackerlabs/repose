package com.rackspace.papi.mocks.auth.osids.wrappers;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import org.openstack.docs.identity.api.v2.*;

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
   
   @Override
   public Object wrapElement(EndpointList endpointList){
       return objectFactory.createEndpoints(endpointList);
   }
   
}
