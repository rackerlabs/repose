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
package org.openrepose.filters.rackspaceauthuser

import java.util
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.filter.logic.AbstractConfiguredFilterHandlerFactory

class RackspaceAuthUserHandlerFactory extends AbstractConfiguredFilterHandlerFactory[RackspaceAuthUserHandler] {

  private val handlerReference = new AtomicReference[RackspaceAuthUserHandler]()

  override protected def buildHandler(): RackspaceAuthUserHandler = {
    if (isInitialized) {
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
