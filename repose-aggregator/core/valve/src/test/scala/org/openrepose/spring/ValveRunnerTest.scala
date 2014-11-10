package org.openrepose.spring

import java.net.URL

import org.junit.runner.RunWith
import org.openrepose.commons.config.manager.{UpdateListener, ConfigurationUpdateManager}
import org.openrepose.commons.config.parser.common.ConfigurationParser
import org.openrepose.commons.config.resource.ConfigurationResourceResolver
import org.openrepose.core.jmx.ConfigurationInformation
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.valve.spring.ValveRunner
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}

import scala.collection.mutable


@RunWith(classOf[JUnitRunner])
class ValveRunnerTest extends FunSpec with Matchers {

  val fakeConfigService = new ConfigurationService {

    val stupidListener: mutable.Map[String,AnyRef] = mutable.Map.empty[String, AnyRef]

    def getListener[T](key:String): UpdateListener[T] = {
      stupidListener(key).asInstanceOf[UpdateListener[T]]
    }

    override def subscribeTo[T](configurationName: String, listener: UpdateListener[T], configurationClass: Class[T]): Unit = ???

    override def subscribeTo[T](filterName: String, configurationName: String, listener: UpdateListener[T], configurationClass: Class[T]): Unit = ???

    //This is the only one we use I think
    override def subscribeTo[T](configurationName: String, xsdStreamSource: URL, listener: UpdateListener[T], configurationClass: Class[T]): Unit = {
      stupidListener(configurationName) = listener
    }

    override def subscribeTo[T](filterName: String, configurationName: String, xsdStreamSource: URL, listener: UpdateListener[T], configurationClass: Class[T]): Unit = ???

    override def subscribeTo[T](filterName: String, configurationName: String, listener: UpdateListener[T], customParser: ConfigurationParser[T]): Unit = ???

    override def subscribeTo[T](filterName: String, configurationName: String, listener: UpdateListener[T], customParser: ConfigurationParser[T], sendNotificationNow: Boolean): Unit = ???

    override def setConfigurationInformation(configurationInformation: ConfigurationInformation): Unit = ???

    override def setResourceResolver(resourceResolver: ConfigurationResourceResolver): Unit = ???

    override def unsubscribeFrom(configurationName: String, plistener: UpdateListener[_]): Unit = ???

    override def getResourceResolver: ConfigurationResourceResolver = ???

    override def getConfigurationInformation: ConfigurationInformation = ???

    override def setUpdateManager(updateManager: ConfigurationUpdateManager): Unit = ???

    override def destroy(): Unit = ???
  }


  it("restarts all nodes when a change to the container.cfg.xml happens") {
    val runner = new ValveRunner(fakeConfigService)


  }
  describe("When updating the system-model") {
    it("restarts only the changed nodes") {
      pending
    }
    it("Stops removed nodes") {
      pending
    }
    it("starts new nodes") {
      pending
    }
    it("will not do anything if the nodes are the same") {
      pending
    }
  }
}
