package org.openrepose.spring

import java.net.URL
import java.util.concurrent.LinkedBlockingQueue

import org.openrepose.commons.config.manager.{ConfigurationUpdateManager, UpdateListener}
import org.openrepose.commons.config.parser.common.ConfigurationParser
import org.openrepose.commons.config.resource.ConfigurationResourceResolver
import org.openrepose.core.jmx.ConfigurationInformation
import org.openrepose.core.services.config.ConfigurationService
import org.slf4j.LoggerFactory

import scala.collection.mutable


class FakeConfigService extends ConfigurationService {

  val log = LoggerFactory.getLogger(this.getClass)

  private val lock = new Object()

  val stupidListener: mutable.Map[String, AnyRef] = mutable.Map.empty[String, AnyRef]

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

  override def setConfigurationInformation(configurationInformation: ConfigurationInformation): Unit = ???

  override def setResourceResolver(resourceResolver: ConfigurationResourceResolver): Unit = ???

  override def unsubscribeFrom(configurationName: String, plistener: UpdateListener[_]): Unit = {
    stupidListener.remove(configurationName) //Drop it from our stuff
  }

  override def getResourceResolver: ConfigurationResourceResolver = ???

  override def getConfigurationInformation: ConfigurationInformation = ???

  override def setUpdateManager(updateManager: ConfigurationUpdateManager): Unit = ???

  override def destroy(): Unit = ???
}