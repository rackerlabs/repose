package com.rackspace.papi.components.keystone.v3

import com.rackspace.papi.commons.config.manager.UpdateListener
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory

class KeystoneV3HandlerFactory extends AbstractConfiguredFilterHandlerFactory[KeystoneV3Handler] {
  override def buildHandler: KeystoneV3Handler = {

  }

  override def getListeners: Map[Class[_], UpdateListener[_]] = {

  }
}
