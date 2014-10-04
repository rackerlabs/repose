package com.rackspace.identity.repose.authidentity

import java.util
import java.util.concurrent.atomic.{AtomicReference, AtomicBoolean}

import com.rackspace.papi.commons.config.manager.UpdateListener
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory

class RackspaceAuthIdentityHandlerFactory extends AbstractConfiguredFilterHandlerFactory[RackspaceAuthIdentityHandler] {

  private val handlerReference = new AtomicReference[RackspaceAuthIdentityHandler]()

  override protected def buildHandler(): RackspaceAuthIdentityHandler = {
    if(isInitialized) {
      handlerReference.get
    } else {
      null //EW
    }
  }

  override protected def getListeners: util.Map[Class[_], UpdateListener[_]] = {
    val listenerMap = new util.HashMap[Class[_], UpdateListener[_]]()

    listenerMap.put(classOf[RackspaceAuthIdentityConfig], new RackspaceAuthIdentityConfigListener())

    listenerMap
  }

  private class RackspaceAuthIdentityConfigListener extends UpdateListener[RackspaceAuthIdentityConfig] {
    private val initialized = new AtomicBoolean()

    override def configurationUpdated(config: RackspaceAuthIdentityConfig): Unit = {
      val handler = new RackspaceAuthIdentityHandler(config)
      handlerReference.set(handler)
      initialized.set(true)
    }

    override def isInitialized: Boolean = {
      initialized.get()
    }
  }
}
