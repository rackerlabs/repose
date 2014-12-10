package org.openrepose.filters.addheader

import java.util

import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.filter.logic.AbstractConfiguredFilterHandlerFactory
import org.openrepose.filters.addheader.config.AddHeadersType

import scala.collection.JavaConverters._

class AddHeaderHandlerFactory extends AbstractConfiguredFilterHandlerFactory[AddHeaderHandler] {

  private var addHeaderHandler: AddHeaderHandler = _

  override protected def buildHandler: AddHeaderHandler = {
    if (isInitialized) addHeaderHandler
    else null
  }

  override protected def getListeners: util.Map[Class[_], UpdateListener[_]] = {
    val listenerMap = new util.HashMap[Class[_], UpdateListener[_]]()

    listenerMap.put(classOf[AddHeadersType], new AddHeaderConfigurationListener())

    listenerMap
  }

  private class AddHeaderConfigurationListener extends UpdateListener[AddHeadersType] {
    private var initialized = false

    def configurationUpdated(addHeaderTypeConfigObject: AddHeadersType) {
      addHeaderHandler = new AddHeaderHandler(addHeaderTypeConfigObject.getHeader.asScala.toList)
      initialized = true
    }

    override def isInitialized: Boolean = {
      initialized
    }
  }

}
