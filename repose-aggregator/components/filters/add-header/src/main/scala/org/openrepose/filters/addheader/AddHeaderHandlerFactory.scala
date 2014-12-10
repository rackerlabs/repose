package org.openrepose.filters.addheader

import java.util
import java.util.concurrent.atomic.AtomicBoolean

import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.filter.logic.AbstractConfiguredFilterHandlerFactory
import org.openrepose.filters.addheader.config.AddHeaderType
import org.openrepose.filters.addheader.config.Header


/**
 * Created by dimi5963 on 12/4/14.
 */
class AddHeaderHandlerFactory(sourceHeaders: List[Header] = List[Header]()) extends AbstractConfiguredFilterHandlerFactory[AddHeaderHandler]{

  override protected def buildHandler: AddHeaderHandler = {
    if (!this.isInitialized) {
      return null
    }
    return new AddHeaderHandler(sourceHeaders)
  }

  override protected def getListeners: util.Map[Class[_], UpdateListener[_]] = {
    val listenerMap = new util.HashMap[Class[_], UpdateListener[_]]()

    listenerMap.put(classOf[AddHeaderType], new AddHeaderConfigurationListener())

    listenerMap
  }

  private class AddHeaderConfigurationListener extends UpdateListener[AddHeaderType] {
    def configurationUpdated(addHeaderTypeConfigObject: AddHeaderType) {
      sourceHeaders :+ addHeaderTypeConfigObject.getHeader
      initialized.set(true)
    }

    override def isInitialized: Boolean = {
      initialized.get()
    }

    private val initialized = new AtomicBoolean()
  }
}
