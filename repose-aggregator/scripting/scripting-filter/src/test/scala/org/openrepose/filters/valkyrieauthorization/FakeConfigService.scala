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
package org.openrepose.filters.valkyrieauthorization

import java.net.URL

import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.config.parser.common.ConfigurationParser
import org.openrepose.commons.config.resource.ConfigurationResourceResolver
import org.openrepose.core.services.config.ConfigurationService
import org.slf4j.LoggerFactory

import scala.collection.mutable


class FakeConfigService extends ConfigurationService {

  val log = LoggerFactory.getLogger(this.getClass)
  val stupidListener: mutable.Map[String, AnyRef] = mutable.Map.empty[String, AnyRef]
  private val lock = new Object()

  def getListener[T](key: String): UpdateListener[T] = {

    //Have to block on an entry
    while (lock.synchronized {
      !stupidListener.keySet.contains(key)
    }) {
      //Set up to block for when we want to get ahold of a listener by a key
      //THis guy blocks forever, it doesn't have any timeout things :(
      Thread.sleep(10)
    }

    stupidListener(key).asInstanceOf[UpdateListener[T]]
  }

  override def subscribeTo[T](configurationName: String, listener: UpdateListener[T], configurationClass: Class[T]): Unit = ???

  override def subscribeTo[T](filterName: String, configurationName: String, listener: UpdateListener[T], configurationClass: Class[T]): Unit = ???

  //This is the only one we use I think
  override def subscribeTo[T](configurationName: String, xsdStreamSource: URL, listener: UpdateListener[T], configurationClass: Class[T]): Unit = {
    log.info(s"Subscribing to ${configurationName}")
    lock.synchronized {
      stupidListener(configurationName) = listener
    }
  }

  override def subscribeTo[T](filterName: String, configurationName: String, xsdStreamSource: URL, listener: UpdateListener[T], configurationClass: Class[T]): Unit = ???

  override def subscribeTo[T](filterName: String, configurationName: String, listener: UpdateListener[T], customParser: ConfigurationParser[T]): Unit = ???

  override def subscribeTo[T](filterName: String, configurationName: String, listener: UpdateListener[T], customParser: ConfigurationParser[T], sendNotificationNow: Boolean): Unit = ???

  override def unsubscribeFrom(configurationName: String, plistener: UpdateListener[_]): Unit = {
    stupidListener.remove(configurationName) //Drop it from our stuff
  }

  override def getResourceResolver: ConfigurationResourceResolver = ???

  override def destroy(): Unit = ???
}