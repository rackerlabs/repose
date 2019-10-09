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

import com.hazelcast.core.Hazelcast
import com.hazelcast.instance.HazelcastInstanceFactory
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class HazelcastDatastoreManagerTest
  extends FunSpec with BeforeAndAfterEach with Matchers {

  var hazelcastDatastoreManager: HazelcastDatastoreManager = _

  override def beforeEach(): Unit = {
    HazelcastInstanceFactory.terminateAll()

    hazelcastDatastoreManager = new HazelcastDatastoreManager(null)
  }

  describe("constructor") {
    it("should create a new Hazelcast instance") {
      Hazelcast.getAllHazelcastInstances should have size 1
    }
  }

  describe("getDatastore") {
    it("should return a Hazelcast Datastore") {
      hazelcastDatastoreManager.getDatastore shouldBe a[HazelcastDatastore]
    }
  }

  describe("isDistributed") {
    it("should return true") {
      hazelcastDatastoreManager.isDistributed() shouldBe true
    }

    ignore("should return false if less than or equal to one Hazelcast instance is a member of the same cluster") {
      hazelcastDatastoreManager.isDistributed shouldBe false
    }

    ignore("should return true if more than one Hazelcast instance is a member of the same cluster") {
      Hazelcast.newHazelcastInstance()

      hazelcastDatastoreManager.isDistributed shouldBe true
    }
  }

  describe("destroy") {
    it("should shutdown the Hazelcast instance created by this manager") {
      hazelcastDatastoreManager.destroy()

      Hazelcast.getAllHazelcastInstances shouldBe empty
    }

    it("should not shutdown a Hazelcast instance not created by this manager") {
      val externalHazelcast = Hazelcast.newHazelcastInstance()

      hazelcastDatastoreManager.destroy()

      Hazelcast.getAllHazelcastInstances should contain only externalHazelcast
    }
  }
}
