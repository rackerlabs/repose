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
package org.openrepose.core.services.datastore.hazelcast

import com.hazelcast.config.{AliasedDiscoveryConfig, Config, DomConfigHelper}
import org.openrepose.core.services.datastore.hazelcast.config.SimplifiedConfig
import org.w3c.dom.Node

import scala.collection.JavaConverters._

object HazelcastConfig {

  private final val ConnectionTimeoutKey = "connection-timeout-seconds"

  def from(simplifiedConfig: SimplifiedConfig): Config = {
    val hazelcastConfig = new Config()
    val networkConfig = hazelcastConfig.getNetworkConfig
    val joinConfig = networkConfig.getJoin

    val port = simplifiedConfig.getPort
    val join = simplifiedConfig.getJoin
    val multicast = join.getMulticast
    val tcpIp = join.getTcpIp
    val aws = join.getAws
    val kubernetes = join.getKubernetes

    // Configure ports
    networkConfig.setPort(port.getValue)
    networkConfig.setPortCount(port.getPortCount)
    networkConfig.setPortAutoIncrement(port.isAutoIncrement)

    // Explicitly disable the default discovery mechanisms
    joinConfig.getMulticastConfig.setEnabled(false)
    joinConfig.getAwsConfig.setEnabled(false)

    // Configure the discovery mechanism
    if (Option(multicast).isDefined) {
      joinConfig.getMulticastConfig
        .setMulticastGroup(multicast.getMulticastGroup)
        .setMulticastPort(multicast.getMulticastPort)
        .setMulticastTimeoutSeconds(multicast.getMulticastTimeoutSeconds)
        .setMulticastTimeToLive(multicast.getMulticastTimeToLive)
        .setEnabled(true)
    } else if (Option(tcpIp).isDefined) {
      joinConfig.getTcpIpConfig
        .setMembers(tcpIp.getMember)
        .setConnectionTimeoutSeconds(tcpIp.getConnectionTimeoutSeconds)
        .setEnabled(true)
    } else if (Option(aws).isDefined) {
      val awsConfig = joinConfig.getAwsConfig
      awsConfig
        .setProperty(ConnectionTimeoutKey, String.valueOf(aws.getConnectionTimeoutSeconds))
        .setEnabled(true)
      aws.getAny.asScala.foreach(setProperty(awsConfig))
    } else if (Option(kubernetes).isDefined) {
      val kubernetesConfig = joinConfig.getKubernetesConfig
      kubernetesConfig
        .setProperty(ConnectionTimeoutKey, String.valueOf(kubernetes.getConnectionTimeoutSeconds))
        .setEnabled(true)
      kubernetes.getAny.asScala.foreach(setProperty(kubernetesConfig))
    }

    hazelcastConfig
  }

  private def setProperty(config: AliasedDiscoveryConfig[_])(node: Node): Unit = {
    val key = DomConfigHelper.cleanNodeName(node)
    val value = Option(node.getTextContent).map(_.trim).getOrElse("")
    config.setProperty(key, value)
  }
}
