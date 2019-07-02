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

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import akka.util.Timeout
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.junit.runner.RunWith
import org.mockito.Mockito.{never, reset, verify}
import org.openrepose.nodeservice.atomfeed.impl.actors.FeedReader.{CancelScheduledReading, ScheduleReading}
import org.openrepose.nodeservice.atomfeed.impl.actors.Notifier.{FeedReaderActivated, FeedReaderCreated, FeedReaderDeactivated, FeedReaderDestroyed}
import org.openrepose.nodeservice.atomfeed.impl.actors.NotifierManager._
import org.openrepose.nodeservice.atomfeed.{AtomFeedListener, LifecycleEvents}
import org.scalatest.concurrent.Eventually
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSuiteLike, Matchers}

import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.language.postfixOps

@RunWith(classOf[JUnitRunner])
class NotifierManagerTest
  extends TestKit(ActorSystem("TestNotifier"))
    with FunSuiteLike with BeforeAndAfterEach with MockitoSugar with Matchers with Eventually {

  implicit val timeout = Timeout(5 seconds)

  val ctx = LogManager.getContext(false).asInstanceOf[LoggerContext]
  val serviceListAppender = ctx.getConfiguration.getAppender("serviceList").asInstanceOf[ListAppender]
  val probe = TestProbe()
  val mockListener = mock[AtomFeedListener]

  var actorRef: TestActorRef[NotifierManager] = _

  override def beforeEach() = {
    reset(mockListener)
    actorRef = TestActorRef[NotifierManager]
  }

  test("a feed reader will be scheduled once bound if there is at least one listener registered and the service is enabled") {
    actorRef ! ServiceEnabled
    actorRef ! AddNotifier("test-id", mockListener)
    actorRef ! BindFeedReader(probe.ref)

    probe.expectMsg(ScheduleReading)
  }

  test("adding a notifier creates a child actor and logs") {
    actorRef ! AddNotifier("test-id", mockListener)

    actorRef.getSingleChild("test-idNotifier") shouldBe an[ActorRef]
    serviceListAppender.getEvents.exists(_.getMessage.getFormattedMessage.contains("Registered")) shouldBe true
  }

  test("adding the first notifier for a feed schedules reading if the service is enabled") {
    actorRef ! ServiceEnabled
    actorRef ! BindFeedReader(probe.ref)
    actorRef ! AddNotifier("test-id", mockListener)

    probe.expectMsg(ScheduleReading)
  }

  test("removing a registered notifier terminates it and and logs") {
    actorRef ! AddNotifier("test-id", mockListener)
    actorRef ! RemoveNotifier("test-id")

    serviceListAppender.getEvents.exists(_.getMessage.getFormattedMessage.contains("Unregistered")) shouldBe true
  }

  test("removing the last registered notifier cancels the feed reader schedule") {
    actorRef ! BindFeedReader(probe.ref)
    actorRef ! ServiceEnabled
    actorRef ! AddNotifier("test-id", mockListener)
    actorRef ! RemoveNotifier("test-id")

    probe.fishForMessage(hint = "cancel the feed reader schedule") {
      case CancelScheduledReading => true
      case _ => false
    }
  }

  test("all notifiers for a feed should be notified of an update") {
    val fakeListenerOne = new FakeAtomFeedListener
    val fakeListenerTwo = new FakeAtomFeedListener
    actorRef ! AddNotifier("test-id1", fakeListenerOne)
    actorRef ! AddNotifier("test-id2", fakeListenerTwo)

    actorRef ! Notify("test-entry")

    eventually {
      fakeListenerOne.lastAtomEntry shouldBe "test-entry"
      fakeListenerTwo.lastAtomEntry shouldBe "test-entry"
    }
  }

  test("a removed notifier should not be notified of an update") {
    actorRef ! AddNotifier("test-id1", mockListener)
    actorRef ! RemoveNotifier("test-id1")

    actorRef ! Notify("test-entry")

    verify(mockListener, never()).onNewAtomEntry("test-entry")
  }

  test("should schedule feed readers when the service is enabled if any notifiers are registered") {
    actorRef ! BindFeedReader(probe.ref)
    actorRef ! AddNotifier("test-id", mockListener)
    actorRef ! ServiceEnabled

    probe.expectMsg(ScheduleReading)
  }

  test("should not schedule feed readers when the service is enabled if no notifiers are registered") {
    actorRef ! BindFeedReader(probe.ref)
    actorRef ! ServiceEnabled

    probe.expectNoMsg()
  }

  test("should cancel feed reader schedules when the service is disabled") {
    actorRef ! BindFeedReader(probe.ref)
    actorRef ! ServiceDisabled

    probe.expectMsg(CancelScheduledReading)
  }

  test("should notify all registered notifiers of feed reader creation") {
    val fakeListenerOne = new FakeAtomFeedListener
    val fakeListenerTwo = new FakeAtomFeedListener
    actorRef ! AddNotifier("test-id1", fakeListenerOne)
    actorRef ! AddNotifier("test-id2", fakeListenerTwo)

    actorRef ! FeedReaderCreated

    eventually {
      fakeListenerOne.lastLifecycleEvent shouldBe LifecycleEvents.FEED_CREATED
      fakeListenerTwo.lastLifecycleEvent shouldBe LifecycleEvents.FEED_CREATED
    }
  }

  test("should notify all registered notifiers of feed reader activation") {
    val fakeListenerOne = new FakeAtomFeedListener
    val fakeListenerTwo = new FakeAtomFeedListener
    actorRef ! AddNotifier("test-id1", fakeListenerOne)
    actorRef ! AddNotifier("test-id2", fakeListenerTwo)

    actorRef ! FeedReaderActivated

    eventually {
      fakeListenerOne.lastLifecycleEvent shouldBe LifecycleEvents.FEED_ACTIVATED
      fakeListenerTwo.lastLifecycleEvent shouldBe LifecycleEvents.FEED_ACTIVATED
    }
  }

  test("should notify all registered notifiers of feed reader deactivation") {
    val fakeListenerOne = new FakeAtomFeedListener
    val fakeListenerTwo = new FakeAtomFeedListener
    actorRef ! AddNotifier("test-id1", fakeListenerOne)
    actorRef ! AddNotifier("test-id2", fakeListenerTwo)

    actorRef ! FeedReaderDeactivated

    eventually {
      fakeListenerOne.lastLifecycleEvent shouldBe LifecycleEvents.FEED_DEACTIVATED
      fakeListenerTwo.lastLifecycleEvent shouldBe LifecycleEvents.FEED_DEACTIVATED
    }
  }

  test("should notify all registered notifiers of feed reader destruction") {
    val fakeListenerOne = new FakeAtomFeedListener
    val fakeListenerTwo = new FakeAtomFeedListener
    actorRef ! AddNotifier("test-id1", fakeListenerOne)
    actorRef ! AddNotifier("test-id2", fakeListenerTwo)

    actorRef ! FeedReaderDestroyed

    eventually {
      fakeListenerOne.lastLifecycleEvent shouldBe LifecycleEvents.FEED_DESTROYED
      fakeListenerTwo.lastLifecycleEvent shouldBe LifecycleEvents.FEED_DESTROYED
    }
  }

  class FakeAtomFeedListener extends AtomFeedListener {
    var lastAtomEntry: String = _
    var lastLifecycleEvent: LifecycleEvents = _

    override def onNewAtomEntry(atomEntry: String): Unit = lastAtomEntry = atomEntry

    override def onLifecycleEvent(event: LifecycleEvents): Unit = lastLifecycleEvent = event
  }

}
