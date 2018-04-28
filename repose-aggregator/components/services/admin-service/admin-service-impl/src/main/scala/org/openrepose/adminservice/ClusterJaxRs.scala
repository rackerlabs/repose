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
package org.openrepose.adminservice

import java.util

import com.fasterxml.jackson.databind.ObjectMapper
import javax.annotation.{PostConstruct, PreDestroy}
import javax.inject.{Inject, Named}
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.serviceclient.akka.{AkkaServiceClient, AkkaServiceClientFactory}
import org.openrepose.core.systemmodel.config.SystemModel
import org.springframework.beans.factory.annotation.Value

import scala.collection.JavaConverters._
import scala.io.Source

@Named
class ClusterJaxRs @Inject() (configurationService: ConfigurationService, akkaServiceClientFactory: AkkaServiceClientFactory, @Value("${server.port}") port: String) extends AdminWebInterface {
  private var nodes: Map[String, String] = _
  private var akkaServiceClient: AkkaServiceClient = _

  @PostConstruct
  def init(): Unit = {
    akkaServiceClient = akkaServiceClientFactory.newAkkaServiceClient()

    configurationService.subscribeTo(
      "system-model.cfg.xml",
      SystemModelConfigurationListener,
      classOf[SystemModel]
    )
  }

  @PreDestroy
  def destroy(): Unit = {
    configurationService.unsubscribeFrom("system-model.cfg.xml", SystemModelConfigurationListener)

    akkaServiceClient.destroy()
  }

  override def healthCheck(): util.Map[String, Object] = {
    val mapper = new ObjectMapper()

    val responses = nodes.mapValues { host => akkaServiceClient.get(s"admin-$host", s"http://$host:$port/node/health", Map[String, String]().asJava) }

    responses.mapValues({ response =>
      response.getStatus match {
        case 200 => mapper.readValue(Source.fromInputStream(response.getData).mkString, classOf[util.Map[String, String]])
        case statusCode => Map("unknown_error" -> s"$statusCode").asJava
      }
    }).asJava.asInstanceOf[util.Map[String, Object]]
//    Map("greeting" -> "Hello from the cluster").asJava
  }

  private object SystemModelConfigurationListener extends UpdateListener[SystemModel] {
    private var initialized = false

    override def configurationUpdated(configurationObject: SystemModel): Unit = {

      initialized = true

      nodes = configurationObject.getReposeCluster.asScala.flatMap({ cluster => cluster.getNodes.getNode.asScala}).map(node => (node.getId -> node.getHostname)).toMap
    }

    override def isInitialized: Boolean = initialized
  }
}
