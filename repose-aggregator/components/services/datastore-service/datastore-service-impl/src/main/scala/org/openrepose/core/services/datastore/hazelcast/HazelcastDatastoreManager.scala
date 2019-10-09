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

import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import com.typesafe.scalalogging.slf4j.StrictLogging
import org.openrepose.core.services.datastore.{Datastore, DatastoreManager}

/**
  * Manages the lifecycle of a [[com.hazelcast.core.HazelcastInstance]] and
  * acts as a bridge between the [[DatastoreManager]] API and the
  * [[com.hazelcast.core.HazelcastInstance]] API.
  *
  * TODO: Add client/server mode support using HazelcastClient
  */
class HazelcastDatastoreManager(config: Config)
  extends DatastoreManager with StrictLogging {

  // TODO: OpenTracing: https://github.com/opentracing-contrib/java-hazelcast
  logger.trace("Creating Hazelcast Datastore")
  private val hazelcast = Hazelcast.newHazelcastInstance(config)
  private val hazelcastDatastore = new HazelcastDatastore(hazelcast)

  logger.debug("Created Hazelcast Datastore {}", hazelcast)

  override def getDatastore: Datastore = {
    hazelcastDatastore
  }

  override def isDistributed: Boolean = {
    // TODO: For an accurate determination at some moment in time, use:
    // TODO: hazelcast.getCluster.getMembers.size > 1
    // TODO: Unfortunately, the Datastore service currently only checks once on creation.
    // TODO: Since Hazelcast may not have connected this member to the cluster yet,
    // TODO: we return true to indicate that Hazelcast data may be distributed.
    true
  }

  override def destroy(): Unit = {
    logger.debug("Destroying Hazelcast Datastore {}", hazelcast)
    hazelcast.shutdown()
  }
}
