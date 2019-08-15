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
package org.openrepose.core.services.datastore.hazelcast.processors

import java.io
import java.util.concurrent.TimeUnit

import com.hazelcast.core.{HazelcastInstance, HazelcastInstanceAware}
import com.hazelcast.map.{EntryBackupProcessor, EntryProcessor}
import org.openrepose.core.services.datastore.Patch

/**
  * Applies a [[Patch]] to an entry in a Hazelcast map.
  */
class MapPatchProcessor[T <: java.io.Serializable](mapName: String,
                                                   patch: Patch[T],
                                                   ttl: Int,
                                                   timeUnit: TimeUnit)
  extends EntryProcessor[String, java.io.Serializable] with HazelcastInstanceAware {

  @transient
  private var hazelcastInstance: HazelcastInstance = _

  override def setHazelcastInstance(hazelcastInstance: HazelcastInstance): Unit = {
    this.hazelcastInstance = hazelcastInstance
  }

  override def process(entry: java.util.Map.Entry[String, java.io.Serializable]): AnyRef = {
    val map = hazelcastInstance.getMap[String, java.io.Serializable](mapName)

    val newValue = Option(entry.getValue)
      .map(_.asInstanceOf[T])
      .map(patch.applyPatch)
      .getOrElse(patch.newFromPatch)

    // Note: If consistency problems arise, start here.
    // Note: This should be safe since it occurs in an EntryProcessor,
    // Note: but I am wary of using this rather than entry.setValue(...).
    // Note: Unfortunately, entry.setValue(...) does not allow setting the TTL.
    map.set(entry.getKey, newValue, ttl, timeUnit)

    newValue.asInstanceOf[AnyRef]
  }

  /**
    * Return null since we do not need to update backups.
    * Updating backups is handled by the use of `map.set(...)` rather than `entry.setValue(...)`.
    */
  override def getBackupProcessor: EntryBackupProcessor[String, io.Serializable] = {
    null
  }
}
