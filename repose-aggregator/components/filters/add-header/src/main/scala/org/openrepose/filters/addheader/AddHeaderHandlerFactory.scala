/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.filters.addheader

import java.util

import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.filter.logic.AbstractConfiguredFilterHandlerFactory
import org.openrepose.filters.addheader.config.AddHeadersConfig

class AddHeaderHandlerFactory extends AbstractConfiguredFilterHandlerFactory[AddHeaderHandler] {

  private var addHeaderHandler: AddHeaderHandler = _

  override protected def buildHandler: AddHeaderHandler = {
    if (isInitialized) addHeaderHandler
    else null
  }

  override protected def getListeners: util.Map[Class[_], UpdateListener[_]] = {
    val listenerMap = new util.HashMap[Class[_], UpdateListener[_]]()

    listenerMap.put(classOf[AddHeadersConfig], new AddHeaderConfigurationListener())

    listenerMap
  }

  private class AddHeaderConfigurationListener extends UpdateListener[AddHeadersConfig] {
    private var initialized = false

    def configurationUpdated(addHeaderTypeConfigObject: AddHeadersConfig) {
      addHeaderHandler = new AddHeaderHandler(addHeaderTypeConfigObject)
      initialized = true
    }

    override def isInitialized: Boolean = {
      initialized
    }
  }

}
