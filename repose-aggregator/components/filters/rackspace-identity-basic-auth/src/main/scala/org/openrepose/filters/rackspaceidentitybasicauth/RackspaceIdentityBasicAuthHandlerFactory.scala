package com.rackspace.papi.components.rackspace.identity.basicauth

import java.util

import org.openrepose.commons.config.manager.UpdateListener
import com.rackspace.papi.components.rackspace.identity.basicauth.config.RackspaceIdentityBasicAuthConfig
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory
import com.rackspace.papi.service.serviceclient.akka.AkkaServiceClient
import org.openrepose.services.datastore.api.DatastoreService

class RackspaceIdentityBasicAuthHandlerFactory(akkaServiceClient: AkkaServiceClient, datastoreService: DatastoreService)
  extends AbstractConfiguredFilterHandlerFactory[RackspaceIdentityBasicAuthHandler] {

  private var rackspaceIdentityBasicAuthHandler: RackspaceIdentityBasicAuthHandler = _

  override def buildHandler: RackspaceIdentityBasicAuthHandler = {
    if (isInitialized) rackspaceIdentityBasicAuthHandler
    else null
  }

  override def getListeners: java.util.Map[Class[_], UpdateListener[_]] = {
    val listenerMap = new util.HashMap[Class[_], UpdateListener[_]]()
    listenerMap.put(classOf[RackspaceIdentityBasicAuthConfig], new RackspaceIdentityBasicAuthConfigurationListener())
    listenerMap
  }

  private class RackspaceIdentityBasicAuthConfigurationListener extends UpdateListener[RackspaceIdentityBasicAuthConfig] {
    private var initialized = false

    def configurationUpdated(config: RackspaceIdentityBasicAuthConfig) {
      rackspaceIdentityBasicAuthHandler = new RackspaceIdentityBasicAuthHandler(config, akkaServiceClient, datastoreService)
      initialized = true
    }

    def isInitialized = {
      initialized
    }
  }

}
