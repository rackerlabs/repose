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
package org.openrepose.core.services.httpclient

import com.typesafe.scalalogging.StrictLogging
import javax.annotation.PreDestroy
import javax.inject.Named
import org.apache.http.impl.client.CloseableHttpClient
import org.springframework.scheduling.annotation.Scheduled

/**
  * Handles decommissioning of HTTP clients. Decommissioning is a process
  * in which client usage is tracked and when a client is no longer in use
  * connections managed by that client are closed and cleaned up. The client
  * itself is also cleaned up, if necessary.
  * <p>
  * This system manages its own background processing since it must only
  * decommission clients when they are no longer in use. For the sake of
  * consistency and concurrency, synchronization is used.
  */
@Named
class HttpClientDecommissioner extends HttpClientUserManager with StrictLogging {

  logger.debug("Starting HttpClientDecommissioner")

  private var clientUsers: Map[String, Set[String]] = Map.empty
  private var clientsToDecom: Seq[InternalHttpClient] = Seq.empty

  @PreDestroy
  def destroy(): Unit = {
    logger.debug("Stopping HttpClientDecommissioner")
  }

  override def registerUser(clientInstanceId: String, userId: String): Unit = synchronized {
    logger.debug("Registering user {} for client {}", userId, clientInstanceId)
    clientUsers += (clientInstanceId -> (clientUsers.getOrElse(clientInstanceId, Set.empty) + userId))
  }

  override def deregisterUser(clientInstanceId: String, userId: String): Unit = synchronized {
    logger.debug("Unregistering user {} for client {}", userId, clientInstanceId)
    val newUsers = clientUsers.getOrElse(clientInstanceId, Set.empty) - userId
    if (newUsers.nonEmpty) {
      clientUsers += (clientInstanceId -> newUsers)
    } else {
      clientUsers -= clientInstanceId
    }
  }

  def decommissionClient(client: InternalHttpClient): Unit = synchronized {
    logger.debug("Scheduling client {} to be decommissioned", client.getInstanceId)
    clientsToDecom :+= client
  }

  @Scheduled(fixedDelay = 5000)
  def run(): Unit = synchronized {
    logger.trace("Checking for HTTP clients to decommission")

    clientsToDecom = clientsToDecom.filter { client =>
      clientUsers.get(client.getInstanceId) match {
        case Some(users) if users.nonEmpty =>
          logger.warn("Failed to decommission HTTP client {} as it is still in use", client.getInstanceId)
          true
        case _ =>
          // Closing the client will also shutdown the connection manager, releasing connections
          client.getClient.close()
          logger.info("Successfully decommissioned HTTP client {}", client.getInstanceId)
          false
      }
    }
  }
}
