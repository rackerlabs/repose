/*
 *  Copyright (c) 2015 Rackspace US, Inc.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.openrepose.commons.utils.test.mocks.auth.osids.wrappers;

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
