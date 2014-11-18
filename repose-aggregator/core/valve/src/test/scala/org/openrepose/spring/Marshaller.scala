package org.openrepose.spring

import java.net.URL

import org.openrepose.commons.config.parser.ConfigurationParserFactory
import org.openrepose.commons.config.resource.impl.BufferedURLConfigurationResource
import org.openrepose.core.container.config.ContainerConfiguration
import org.openrepose.core.systemmodel.SystemModel

import scala.reflect.ClassTag

object Marshaller {

  val systemModelXSD = getClass.getResource("/META-INF/schema/system-model/system-model.xsd")
  val containerConfigXSD = getClass.getResource("/META-INF/schema/container/container-configuration.xsd")


  def systemModel(resource: String): SystemModel = {
    configResource[SystemModel](resource, systemModelXSD)
  }

  def containerConfig(resource: String): ContainerConfiguration = {
    configResource[ContainerConfiguration](resource, containerConfigXSD)
  }

  def configResource[T:ClassTag](resource: String, xsdURL: URL): T = {
    import scala.reflect._
    val ct:ClassTag[T] = classTag[T]
    val parser = ConfigurationParserFactory.getXmlConfigurationParser(
      ct.runtimeClass.asInstanceOf[Class[T]],
      xsdURL)

    val configResource = new BufferedURLConfigurationResource(this.getClass.getResource(resource))

    parser.read(configResource)
  }

}
