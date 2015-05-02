package org.openrepose.valve.jmx

object ValvePortMXBean {
  final val OBJECT_NAME = "org.openrepose.valve.jmx:type=ValvePort"
}

trait ValvePortMXBean {
  def getPort(clusterId: String, nodeId: String): Int

  def getName():String
}
