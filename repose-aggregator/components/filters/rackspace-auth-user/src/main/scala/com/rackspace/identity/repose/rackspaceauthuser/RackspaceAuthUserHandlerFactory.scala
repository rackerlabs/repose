package com.rackspace.identity.repose.rackspaceauthuser

import java.util
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import com.rackspace.papi.commons.config.manager.UpdateListener
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory

class RackspaceAuthUserHandlerFactory extends AbstractConfiguredFilterHandlerFactory[RackspaceAuthUserHandler] {

  private val handlerReference = new AtomicReference[RackspaceAuthUserHandler]()

  override protected def buildHandler(): RackspaceAuthUserHandler = {
    if(isInitialized) {
      handlerReference.get
    } else {
      null //EW
    }
  }

  override protected def getListeners: util.Map[Class[_], UpdateListener[_]] = {
    val listenerMap = new util.HashMap[Class[_], UpdateListener[_]]()

    listenerMap.put(classOf[RackspaceAuthUserConfig], new RackspaceAuthIdentityConfigListener())

    listenerMap
  }

  private class RackspaceAuthIdentityConfigListener extends UpdateListener[RackspaceAuthUserConfig] {
    private val initialized = new AtomicBoolean()

    override def configurationUpdated(config: RackspaceAuthUserConfig): Unit = {
      val handler = new RackspaceAuthUserHandler(config)
      handlerReference.set(handler)
      initialized.set(true)
    }

    override def isInitialized: Boolean = {
      initialized.get()
    }
  }
}
