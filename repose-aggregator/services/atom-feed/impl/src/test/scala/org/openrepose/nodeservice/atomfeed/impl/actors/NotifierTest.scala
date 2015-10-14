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
package org.openrepose.nodeservice.atomfeed.impl.actors

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit}
import org.junit.runner.RunWith
import org.mockito.Mockito.{reset, verify}
import org.openrepose.nodeservice.atomfeed.AtomFeedListener
import org.openrepose.nodeservice.atomfeed.impl.actors.Notifier.NotifyListeners
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSuiteLike}

import scala.collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class NotifierTest extends TestKit(ActorSystem("TestNotifier")) with FunSuiteLike with MockitoSugar with BeforeAndAfter {

  val mockAtomFeedListener = mock[AtomFeedListener]
  val atomFeedListeners = Set(mockAtomFeedListener)
  val actorRef = TestActorRef(new Notifier(atomFeedListeners))

  before {
    reset(mockAtomFeedListener)
  }

  test("a registered listener is notified of new entries exactly once") {
    val entries = List("test")

    actorRef ! NotifyListeners(entries)

    verify(mockAtomFeedListener).onNewAtomEntry(entries)
  }
}
