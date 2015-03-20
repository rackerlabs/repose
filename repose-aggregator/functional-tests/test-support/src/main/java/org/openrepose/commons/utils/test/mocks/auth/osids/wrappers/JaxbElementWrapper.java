/*
 * #%L
 * Repose
 * %%
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.openrepose.commons.utils.test.mocks.auth.osids.wrappers;

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
