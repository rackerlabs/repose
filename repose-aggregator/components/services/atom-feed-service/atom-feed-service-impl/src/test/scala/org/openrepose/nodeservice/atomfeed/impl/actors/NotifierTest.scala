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
import org.openrepose.nodeservice.atomfeed.impl.actors.Notifier._
import org.openrepose.nodeservice.atomfeed.{AtomFeedListener, LifecycleEvents}
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSuiteLike}

@RunWith(classOf[JUnitRunner])
class NotifierTest extends TestKit(ActorSystem("TestNotifier")) with FunSuiteLike with MockitoSugar with BeforeAndAfterEach {

  val mockAtomFeedListener = mock[AtomFeedListener]
  val actorRef = TestActorRef(new Notifier(mockAtomFeedListener))

  override def beforeEach() = {
    reset(mockAtomFeedListener)
  }

  test("a registered listener is notified on listener registration completion") {
    actorRef.underlyingActor.preStart()

    verify(mockAtomFeedListener).onLifecycleEvent(LifecycleEvents.LISTENER_REGISTERED)
  }

  test("a registered listener is notified on listener unregistration completion") {
    actorRef.underlyingActor.postStop()

    verify(mockAtomFeedListener).onLifecycleEvent(LifecycleEvents.LISTENER_UNREGISTERED)
  }

  test("a registered listener is notified of new entries exactly once") {
    val entry = "test-entry"

    actorRef ! NotifyListener(entry)

    verify(mockAtomFeedListener).onNewAtomEntry(entry)
  }

  test("a registered listener is notified on feed creation") {
    actorRef ! FeedReaderCreated

    verify(mockAtomFeedListener).onLifecycleEvent(LifecycleEvents.FEED_CREATED)
  }

  test("a registered listener is notified on feed activation") {
    actorRef ! FeedReaderActivated

    verify(mockAtomFeedListener).onLifecycleEvent(LifecycleEvents.FEED_ACTIVATED)
  }

  test("a registered listener is notified on feed deactivation") {
    actorRef ! FeedReaderDeactivated

    verify(mockAtomFeedListener).onLifecycleEvent(LifecycleEvents.FEED_DEACTIVATED)
  }

  test("a registered listener is notified on feed destruction") {
    actorRef ! FeedReaderDestroyed

    verify(mockAtomFeedListener).onLifecycleEvent(LifecycleEvents.FEED_DESTROYED)
  }
}
