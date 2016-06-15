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
package org.openrepose.filters.openstackidentityv3.utilities

import java.io.Serializable
import java.util.concurrent.TimeUnit

import org.openrepose.core.services.datastore.{Datastore, Patch}

import scala.util.Random

object Cache {
  final val AdminTokenKey = "IDENTITY:V3:ADMIN_TOKEN"
  final val TokenKeyPrefix = "IDENTITY:V3:TOKEN:"
  final val GroupsKeyPrefix = "IDENTITY:V3:GROUPS:"
  final val UserIdKeyPrefix = "IDENTITY:V3:USER_ID:"

  def getTokenKey(token: String): String = TokenKeyPrefix + token

  def getGroupsKey(token: String): String = GroupsKeyPrefix + token

  def getUserIdKey(userId: String): String = UserIdKeyPrefix + userId

  def offsetTtl(exactTtl: Int, offset: Int): Int = {
    if (offset == 0 || exactTtl == 0) exactTtl
    else safeLongToInt(exactTtl.toLong + (Random.nextInt(offset * 2) - offset))
  }

  def safeLongToInt(long: Long): Int = {
    if (long < 0) math.max(long, Int.MinValue).toInt
    else math.min(long, Int.MaxValue).toInt
  }
}

class Cache(datastore: Datastore) extends Datastore {
  def safeGet[T <: Serializable](key: String, cls: Class[T]): Option[T] = {
    try {
      Option(get(key)).map(cls.cast)
    } catch {
      case cce: ClassCastException => None
    }
  }

  override def get(key: String): Serializable = datastore.get(key)

  override def removeAll(): Unit = datastore.removeAll()

  override def getName: String = datastore.getName

  override def put(key: String, value: Serializable): Unit = datastore.put(key, value)

  override def put(key: String, value: Serializable, ttl: Int, timeUnit: TimeUnit): Unit =
    datastore.put(key, value, ttl, timeUnit)

  override def remove(key: String): Boolean = datastore.remove(key)

  override def patch(key: String, patch: Patch[_]): Serializable = datastore.patch(key, patch)

  override def patch(key: String, patch: Patch[_], ttl: Int, timeUnit: TimeUnit): Serializable =
    datastore.patch(key, patch, ttl, timeUnit)
}
