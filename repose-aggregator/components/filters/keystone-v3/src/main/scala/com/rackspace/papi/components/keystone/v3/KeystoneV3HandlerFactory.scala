package com.rackspace.papi.components.keystone.v3

import java.util

import com.rackspace.papi.commons.config.manager.UpdateListener
import com.rackspace.papi.components.keystone.v3.config.KeystoneV3Config
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory
import com.rackspace.papi.service.datastore.DatastoreService
import com.rackspace.papi.service.httpclient.HttpClientService
import com.rackspace.papi.service.serviceclient.akka.AkkaServiceClient

// TODO: Don't extend AbstractConfiguredFilterHandlerFactory (or the FilterDirector)
class KeystoneV3HandlerFactory(akkaServiceClient: AkkaServiceClient, datastoreService: DatastoreService)
  extends AbstractConfiguredFilterHandlerFactory[KeystoneV3Handler] {

  private var keystoneHandler: KeystoneV3Handler = _

  override def buildHandler: KeystoneV3Handler = {
    if (isInitialized) keystoneHandler
    else null
  }

  override def getListeners: java.util.Map[Class[_], UpdateListener[_]] = {
    val listenerMap = new util.HashMap[Class[_], UpdateListener[_]]()

    listenerMap.put(classOf[KeystoneV3Config], new KeystoneV3ConfigurationListener())

    listenerMap
  }

  private class KeystoneV3ConfigurationListener extends UpdateListener[KeystoneV3Config] {
    private var initialized = false

    def configurationUpdated(config: KeystoneV3Config) {
      keystoneHandler = new KeystoneV3Handler(config, akkaServiceClient, datastoreService)
      initialized = true
    }

    def isInitialized = {
      initialized
    }
  }

}
