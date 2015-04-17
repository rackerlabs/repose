package org.openrepose.filters.mergeheader

import java.util
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.filter.logic.AbstractConfiguredFilterHandlerFactory

class MergeHeaderHandlerFactory extends AbstractConfiguredFilterHandlerFactory[MergeHeaderHandler] {
  private val handlerReference = new AtomicReference[MergeHeaderHandler]()

  override protected def buildHandler(): MergeHeaderHandler = {
    if (isInitialized) {
      handlerReference.get
    } else {
      null //EW
    }
  }

  override protected def getListeners: util.Map[Class[_], UpdateListener[_]] = {
    val listenerMap = new util.HashMap[Class[_], UpdateListener[_]]()

    listenerMap.put(classOf[MergeHeaderConfig], new RackspaceAuthIdentityConfigListener())

    listenerMap
  }

  private class RackspaceAuthIdentityConfigListener extends UpdateListener[MergeHeaderConfig] {
    private val initialized = new AtomicBoolean()

    override def configurationUpdated(config: MergeHeaderConfig): Unit = {
      val handler = new MergeHeaderHandler(config)
      handlerReference.set(handler)
      initialized.set(true)
    }

    override def isInitialized: Boolean = {
      initialized.get()
    }
  }

}
