package com.rackspace.papi.mocks.auth.rs11.wrappers;

import com.rackspacecloud.docs.auth.api.v1.*;

public class JaxbElementWrapper implements ResponseWrapper {
   private final ObjectFactory objectFactory;
   
   public JaxbElementWrapper() {
      objectFactory = new ObjectFactory();
   }
   
   @Override
   public Object wrapElement(GroupsList groups) {
      return objectFactory.createGroups(groups);
   }

   @Override
   public Object wrapElement(FullToken token) {
      return objectFactory.createToken(token);
   }

   @Override
   public Object wrapElement(UnauthorizedFault fault) {
      return objectFactory.createUnauthorized(fault);
   }

   @Override
   public Object wrapElement(ItemNotFoundFault fault) {
      return objectFactory.createItemNotFound(fault);
   }

   
}
