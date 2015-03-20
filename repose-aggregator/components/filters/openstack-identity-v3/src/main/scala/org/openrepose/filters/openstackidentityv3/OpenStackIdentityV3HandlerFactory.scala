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
package org.openrepose.filters.openstackidentityv3

import java.util

import org.openrepose.core.filter.logic.AbstractConfiguredFilterHandlerFactory
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.services.datastore.DatastoreService
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient
import org.openrepose.filters.openstackidentityv3.config.OpenstackIdentityV3Config
import org.openrepose.filters.openstackidentityv3.utilities.OpenStackIdentityV3API

class OpenStackIdentityV3HandlerFactory(akkaServiceClient: AkkaServiceClient, datastoreService: DatastoreService)
  extends AbstractConfiguredFilterHandlerFactory[OpenStackIdentityV3Handler] {

  private var keystoneHandler: OpenStackIdentityV3Handler = _

  override def buildHandler: OpenStackIdentityV3Handler = {
    if (isInitialized) keystoneHandler
    else null
  }

  override def getListeners: java.util.Map[Class[_], UpdateListener[_]] = {
    val listenerMap = new util.HashMap[Class[_], UpdateListener[_]]()

    listenerMap.put(classOf[OpenstackIdentityV3Config], new KeystoneV3ConfigurationListener())

    listenerMap
  }

  private class KeystoneV3ConfigurationListener extends UpdateListener[OpenstackIdentityV3Config] {
    private var initialized = false

    def configurationUpdated(config: OpenstackIdentityV3Config) {
      val identityAPI = new OpenStackIdentityV3API(config, datastoreService.getDefaultDatastore, akkaServiceClient)
      keystoneHandler = new OpenStackIdentityV3Handler(config, identityAPI)
      initialized = true
    }

    def isInitialized = {
      initialized
    }
  }

}
