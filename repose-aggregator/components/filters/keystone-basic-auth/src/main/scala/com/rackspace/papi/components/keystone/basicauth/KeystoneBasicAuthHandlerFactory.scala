package com.rackspace.papi.components.keystone.basicauth

import java.util

import com.rackspace.papi.commons.config.manager.UpdateListener
import com.rackspace.papi.components.keystone.basicauth.config.KeystoneBasicAuthConfig
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory
import com.rackspace.papi.service.datastore.DatastoreService
import com.rackspace.papi.service.serviceclient.akka.AkkaServiceClient

class KeystoneBasicAuthHandlerFactory(akkaServiceClient: AkkaServiceClient, datastoreService: DatastoreService)
  extends AbstractConfiguredFilterHandlerFactory[KeystoneBasicAuthHandler] {

  private var keystoneHandler: KeystoneBasicAuthHandler = _
  private var todoAttribute: Boolean = false
  private var todoElement: Boolean = false

  override def buildHandler: KeystoneBasicAuthHandler = {
    if (isInitialized) keystoneHandler
    else null
  }

  override def getListeners: java.util.Map[Class[_], UpdateListener[_]] = {
    val listenerMap = new util.HashMap[Class[_], UpdateListener[_]]()
    listenerMap.put(classOf[KeystoneBasicAuthConfig], new KeystoneBasicAuthConfigurationListener())
    listenerMap
  }

  private class KeystoneBasicAuthConfigurationListener extends UpdateListener[KeystoneBasicAuthConfig] {
    private var initialized = false

    def configurationUpdated(config: KeystoneBasicAuthConfig) {
      keystoneHandler = new KeystoneBasicAuthHandler(config, akkaServiceClient, datastoreService)
      initialized = true
    }

    def isInitialized = {
      initialized
    }
  }

  private class KeystoneBasicAuthFilterConfigListener extends UpdateListener[KeystoneBasicAuthConfig] {
    private var initialized = false

    override def configurationUpdated(config: KeystoneBasicAuthConfig) {
      initialized = false
      todoAttribute = config.isTodoAttribute
      todoElement = config.isTodoElement
      initialized = true
    }

    override def isInitialized = {
      initialized
    }
  }

}
