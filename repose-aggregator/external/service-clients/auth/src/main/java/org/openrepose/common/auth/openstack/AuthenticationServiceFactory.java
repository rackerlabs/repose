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
package org.openrepose.common.auth.openstack;

import org.openrepose.common.auth.AuthServiceException;
import org.openrepose.common.auth.ResponseUnmarshaller;
import org.openrepose.commons.utils.transform.jaxb.JaxbEntityToXml;
import org.openrepose.core.services.httpclient.HttpClientService;
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

public class AuthenticationServiceFactory {
   private static final Logger LOG = LoggerFactory.getLogger(AuthenticationServiceFactory.class);

   public AuthenticationService build(String targetHostUri, String username, String password, String tenantId,
                                      String connectionPoolId,HttpClientService httpClientService,
                                      AkkaServiceClient akkaServiceClient) throws AuthServiceException {

      JAXBContext coreJaxbContext;
      JAXBContext groupJaxbContext;
      try {
         coreJaxbContext = JAXBContext.newInstance(
                 org.openstack.docs.identity.api.v2.ObjectFactory.class,
                 com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory.class);
         groupJaxbContext = JAXBContext.newInstance(com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory.class);
      } catch (JAXBException e) {
         LOG.error("Problem creating the jaxb context for the OpenStack Auth objects.", e);
         throw new AuthServiceException("Possible deployment problem! Unable to build JAXB Context for OpenStack Auth schema types. Reason: "
                 + e.getMessage(), e);
      }

      return new AuthenticationServiceClient(targetHostUri, username, password, tenantId,
              new ResponseUnmarshaller(coreJaxbContext),
              new ResponseUnmarshaller(groupJaxbContext),
              new JaxbEntityToXml(coreJaxbContext),
              akkaServiceClient);
   }
}
