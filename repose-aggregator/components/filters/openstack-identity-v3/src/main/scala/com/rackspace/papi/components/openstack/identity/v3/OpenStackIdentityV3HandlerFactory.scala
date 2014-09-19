package com.rackspace.papi.components.openstack.identity.v3

import java.util

import com.rackspace.papi.commons.config.manager.UpdateListener
import com.rackspace.papi.components.openstack.identity.v3.config.OpenstackIdentityV3Config
import com.rackspace.papi.components.openstack.identity.v3.utilities.OpenStackIdentityV3API
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory
import com.rackspace.papi.service.datastore.DatastoreService
import com.rackspace.papi.service.serviceclient.akka.AkkaServiceClient

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
