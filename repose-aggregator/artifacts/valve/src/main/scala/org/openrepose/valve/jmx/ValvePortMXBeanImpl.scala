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
package org.openrepose.valve.jmx

import java.util.concurrent.ConcurrentHashMap

import javax.inject.Named
import org.openrepose.valve.jetty.ReposeJettyServer
import org.springframework.jmx.export.annotation.{ManagedAttribute, ManagedResource}

@Named
@ManagedResource(
  objectName = "org.openrepose.valve.jmx:type=ValvePort",
  description = "Get the ports from valve in test mode"
)
class ValvePortMXBeanImpl extends ValvePortMXBean {

  val nodes = new ConcurrentHashMap[String, ReposeJettyServer]()

  @ManagedAttribute(description = "Returns the port of the selected node, or zero if it isn't set")
  override def getPort(nodeId: String): Int = {
    Option(nodes.get(nodeId)).map { node =>
      node.runningHttpPort
    } getOrElse 0
  }

  @ManagedAttribute(description = "Returns the SSL port of the selected node, or zero if it isn't set")
  override def getSslPort(nodeId: String): Int = {
    Option(nodes.get(nodeId)).map { node =>
      node.runningHttpsPort
    } getOrElse 0
  }

  def replaceNode(nodeId: String, node: ReposeJettyServer): Unit = {
    nodes.replace(nodeId, node)
  }

  def removeNode(nodeId: String): Unit = {
    nodes.remove(nodeId)
  }

  def addNode(nodeId: String, jettyNode: ReposeJettyServer): Unit = {
    nodes.put(nodeId, jettyNode)
  }
}
