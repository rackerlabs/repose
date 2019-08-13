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
package org.openrepose.core.services.datastore.hazelcast.tasks

import java.io
import java.util.concurrent.{Callable, TimeUnit}

import com.hazelcast.core.{HazelcastInstance, HazelcastInstanceAware}
import org.openrepose.core.services.datastore.Patch

class MapPatchTask[T <: io.Serializable](mapName: String,
                                         key: String,
                                         patch: Patch[T],
                                         ttl: Int,
                                         timeUnit: TimeUnit)
  extends Callable[T] with HazelcastInstanceAware with Serializable {

  @transient
  private var hazelcast: HazelcastInstance = _

  override def setHazelcastInstance(hazelcastInstance: HazelcastInstance): Unit = {
    this.hazelcast = hazelcastInstance
  }

  override def call(): T = {
    val data = hazelcast.getMap[String, io.Serializable](mapName)

    data.lock(key)
    try {
      val newValue = Option(data.get(key))
        .map(_.asInstanceOf[T])
        .map(patch.applyPatch)
        .getOrElse(patch.newFromPatch)
      data.set(key, newValue, ttl, timeUnit)
      newValue
    } finally {
      data.unlock(key)
    }
  }
}
