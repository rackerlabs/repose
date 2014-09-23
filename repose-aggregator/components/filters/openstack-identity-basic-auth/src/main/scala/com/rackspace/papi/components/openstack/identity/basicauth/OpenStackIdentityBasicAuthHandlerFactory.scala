package com.rackspace.papi.components.openstack.identity.basicauth

import java.util

import com.rackspace.papi.commons.config.manager.UpdateListener
import com.rackspace.papi.components.openstack.identity.basicauth.config.OpenStackIdentityBasicAuthConfig
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory
import com.rackspace.papi.service.datastore.DatastoreService
import com.rackspace.papi.service.serviceclient.akka.AkkaServiceClient

class OpenStackIdentityBasicAuthHandlerFactory(akkaServiceClient: AkkaServiceClient, datastoreService: DatastoreService)
  extends AbstractConfiguredFilterHandlerFactory[OpenStackIdentityBasicAuthHandler] {

  private var openStackIdentityBasicAuthHandler: OpenStackIdentityBasicAuthHandler = _

  override def buildHandler: OpenStackIdentityBasicAuthHandler = {
    if (isInitialized) openStackIdentityBasicAuthHandler
    else null
  }

  override def getListeners: java.util.Map[Class[_], UpdateListener[_]] = {
    val listenerMap = new util.HashMap[Class[_], UpdateListener[_]]()
    listenerMap.put(classOf[OpenStackIdentityBasicAuthConfig], new OpenStackIdentityBasicAuthConfigurationListener())
    listenerMap
  }

  private class OpenStackIdentityBasicAuthConfigurationListener extends UpdateListener[OpenStackIdentityBasicAuthConfig] {
    private var initialized = false

    def configurationUpdated(config: OpenStackIdentityBasicAuthConfig) {
      openStackIdentityBasicAuthHandler = new OpenStackIdentityBasicAuthHandler(config, akkaServiceClient, datastoreService)
      initialized = true
    }

    def isInitialized = {
      initialized
    }
  }

}
