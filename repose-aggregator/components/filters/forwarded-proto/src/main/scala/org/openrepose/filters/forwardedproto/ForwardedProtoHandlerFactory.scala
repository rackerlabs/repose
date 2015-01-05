package org.openrepose.filters.forwardedproto

import org.openrepose.core.filter.logic.AbstractConfiguredFilterHandlerFactory
import org.openrepose.commons.config.manager.UpdateListener

/**
 * Created by eric7500 on 12/30/14.
 */
class ForwardedProtoHandlerFactory()
  extends AbstractConfiguredFilterHandlerFactory[ForwardedProtoHandler] {

  override def buildHandler: ForwardedProtoHandler = {
    val handler = new ForwardedProtoHandler
    handler
  }

  override def getListeners: java.util.Map[Class[_], UpdateListener[_]] = {
    val listenerMap = new java.util.HashMap[Class[_], UpdateListener[_]]()
    listenerMap
  }

}