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

import java.io
import java.util.concurrent.TimeUnit

import com.hazelcast.core.HazelcastInstance
import com.typesafe.scalalogging.slf4j.StrictLogging
import org.openrepose.core.services.datastore.hazelcast.HazelcastDatastore._
import org.openrepose.core.services.datastore.hazelcast.tasks.MapPatchTask
import org.openrepose.core.services.datastore.{Datastore, DatastoreOperationException, Patch}

import scala.language.implicitConversions
import scala.util.control.NonFatal

/**
  * Bridges the [[Datastore]] Service API with the [[HazelcastInstance]] API.
  *
  * Note that while not advised, usage of this class can be circumvented
  * by directly retrieving a [[HazelcastInstance]] from [[com.hazelcast.core.Hazelcast]].
  */
class HazelcastDatastore(hazelcast: HazelcastInstance)
  extends Datastore with StrictLogging {

  private val data = hazelcast.getMap[String, io.Serializable](HazelcastMapName)
  private val executorService = hazelcast.getExecutorService(HazelcastExecutorServiceName)

  override def get(key: String): io.Serializable = wrapExceptions {
    logger.trace("Getting data for {}", key)
    data.get(key)
  }

  override def put(key: String, value: io.Serializable): Unit = {
    // We call through two reasons:
    // 1. To reduce redundancy.
    // 2. To explicitly disable TTL, since otherwise Hazelcast will update the entry's access time but keep the existing TTL.
    this.put(key, value, DisabledTtl, TimeUnit.MILLISECONDS)
  }

  override def put(key: String, value: io.Serializable, ttl: Int, timeUnit: TimeUnit): Unit = wrapExceptions {
    logger.trace("Putting data for {}", key)
    data.set(key, value, ttl, timeUnit)
  }

  override def patch[T <: io.Serializable](key: String, patch: Patch[T]): T = {
    // We call through two reasons:
    // 1. To reduce redundancy.
    // 2. To explicitly disable TTL, since otherwise Hazelcast will update the entry's access time but keep the existing TTL.
    this.patch[T](key, patch, DisabledTtl, TimeUnit.MILLISECONDS)
  }

  override def patch[T <: io.Serializable](key: String, patch: Patch[T], ttl: Int, timeUnit: TimeUnit): T = wrapExceptions {
    logger.trace("Patching data for {}", key)

    // Forward our task to the key-owning member of the Hazelcast cluster.
    // Doing so prevents multiple network hops and reduces the amount of time
    // a key remains locked.
    // The net result is much better performance.
    // An ExecutorService was used rather than an EntryProcessor so that TTL
    // can be set.
    val patchTask = new MapPatchTask(HazelcastMapName, key, patch, ttl, timeUnit)
    val future = executorService.submitToKeyOwner(patchTask, key)
    future.get()
  }

  override def remove(key: String): Boolean = wrapExceptions {
    logger.trace("Removing data for {}", key)
    Option(data.remove(key)).isDefined
  }

  override def removeAll(): Unit = wrapExceptions {
    logger.trace("Removing all data")
    data.clear()
  }

  override def getName: String = {
    Name
  }
}

object HazelcastDatastore {
  final val Name = "hazelcast"

  private final val HazelcastMapName = "hazelcast-datastore-map"
  private final val HazelcastExecutorServiceName = "hazelcast-datastore-executor-service"
  private final val DisabledTtl = -1

  private def wrapExceptions[T](f: => T): T = {
    try {
      f
    } catch {
      case NonFatal(t) => throw new DatastoreOperationException(t)
    }
  }
}
