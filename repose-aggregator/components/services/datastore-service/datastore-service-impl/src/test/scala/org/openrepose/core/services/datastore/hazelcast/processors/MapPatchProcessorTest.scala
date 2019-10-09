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

import com.hazelcast.core.{HazelcastInstance, IMap}
import org.junit.runner.RunWith
import org.mockito.Matchers.{any, anyString}
import org.mockito.Mockito.{verify, when}
import org.openrepose.core.services.datastore.Patch
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class MapPatchProcessorTest
  extends FunSpec with BeforeAndAfterEach with MockitoSugar with Matchers {

  final val MapName = "test-map"
  final val KeyName = "test-key"
  final val Ttl = 10
  final val TtlTimeUnit = TimeUnit.SECONDS

  var patch: Patch[java.io.Serializable] = _
  var hazelcastInstance: HazelcastInstance = _
  var hazelcastMap: IMap[String, java.io.Serializable] = _
  var entry: java.util.Map.Entry[String, io.Serializable] = _
  var mapPatchProcessor: MapPatchProcessor[java.io.Serializable] = _

  override protected def beforeEach(): Unit = {
    patch = mock[Patch[java.io.Serializable]]
    hazelcastInstance = mock[HazelcastInstance]
    hazelcastMap = mock[IMap[String, java.io.Serializable]]
    entry = mock[java.util.Map.Entry[String, java.io.Serializable]]

    when(hazelcastInstance.getMap[String, java.io.Serializable](anyString)).thenReturn(hazelcastMap)
    when(entry.getKey).thenReturn(KeyName)

    mapPatchProcessor = new MapPatchProcessor(MapName, patch, Ttl, TtlTimeUnit)

    mapPatchProcessor.setHazelcastInstance(hazelcastInstance)
  }

  describe("process") {
    it("should patch an existing value") {
      val existingValue = 2
      val patchedValue = 3

      when(entry.getValue).thenReturn(existingValue, Nil: _*)
      when(patch.applyPatch(any[java.io.Serializable])).thenReturn(patchedValue, Nil: _*)

      mapPatchProcessor.process(entry) shouldEqual patchedValue

      verify(hazelcastInstance).getMap(MapName)
      verify(patch).applyPatch(existingValue)
      verify(hazelcastMap).set(KeyName, patchedValue, Ttl, TtlTimeUnit)
    }

    it("should set a new value") {
      val newValue = 1

      when(patch.newFromPatch).thenReturn(newValue, Nil: _*)

      mapPatchProcessor.process(entry) shouldEqual newValue

      verify(hazelcastInstance).getMap(MapName)
      verify(patch).newFromPatch()
      verify(hazelcastMap).set(KeyName, newValue, Ttl, TtlTimeUnit)
    }
  }

  describe("getBackupProcess") {
    it("should return null") {
      mapPatchProcessor.getBackupProcessor shouldBe null
    }
  }
}
