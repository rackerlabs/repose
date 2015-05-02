package org.openrepose.valve.jmx

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Named

import org.springframework.jmx.export.annotation.{ManagedAttribute, ManagedResource}


@Named
@ManagedResource(
  objectName = "org.openrepose.valve.jmx:type=ValvePort",
  description = "Get the ports from valve in test mode"
)
class ValvePortMXBeanImpl extends ValvePortMXBean {

  val nodePorts = new ConcurrentHashMap[String, Int]()

  def key(clusterId: String, nodeId: String): String = {
    clusterId + "-" + nodeId
  }

  @ManagedAttribute(description = "Returns the port of the selected node, or zero if it isn't set")
  override def getPort(clusterId: String, nodeId: String): Int = {
    nodePorts.get(key(clusterId, nodeId))
  }

  def setPort(clusterId: String, nodeId: String, port: Int): Unit = {
    nodePorts.put(key(clusterId, nodeId), port)
  }

  override def getName(): String = ValvePortMXBean.OBJECT_NAME
}