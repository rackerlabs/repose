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
import org.openrepose.core.services.datastore.hazelcast.processors.MapPatchProcessor
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

    // Uses an EntryProcessor so that we do not have to worry about data
    // consistency or where our code is executed; we can rely on Hazelcast
    // to execute our code on the member which owns the data in a consistent
    // manner.
    // This approach is more performant than locking ourselves, even when an
    // ExecutorService is used to run our code on the data-owner node.
    data.executeOnKey(key, new MapPatchProcessor(HazelcastMapName, patch, ttl, timeUnit)).asInstanceOf[T]
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
  private final val DisabledTtl = -1

  private def wrapExceptions[T](f: => T): T = {
    try {
      f
    } catch {
      case NonFatal(t) => throw new DatastoreOperationException(t)
    }
  }
}
