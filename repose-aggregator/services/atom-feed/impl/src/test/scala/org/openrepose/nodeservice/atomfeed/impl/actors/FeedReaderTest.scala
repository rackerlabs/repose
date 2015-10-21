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

import java.io.StringWriter
import java.util.Date

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import org.apache.abdera.Abdera
import org.apache.abdera.model.Feed
import org.junit.runner.RunWith
import org.openrepose.docs.repose.atom_feed_service.v1.{AuthenticationType, EntryOrderType}
import org.openrepose.nodeservice.atomfeed.impl.actors.FeedReader.ReadFeed
import org.openrepose.nodeservice.atomfeed.impl.actors.Notifier.NotifyListeners
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSuiteLike}

import scala.language.postfixOps

@RunWith(classOf[JUnitRunner])
class FeedReaderTest(_system: ActorSystem)
  extends TestKit(_system) with FunSuiteLike with BeforeAndAfter {

  def this() = this(ActorSystem("FeedReaderTest"))

  val notifierProbe = TestProbe()
  val abdera = Abdera.getInstance()

  var feed: Feed = _
  var actorRef: TestActorRef[FeedReader] = _
  var mockAtomFeedService: MockService = _

  before {
    mockAtomFeedService = new MockService()

    feed = abdera.newFeed()
    feed.setId("tag:openrepose.org,2007:/feed")
    feed.setTitle("Test Title")
    feed.setSubtitle("Test Subtitle")
    feed.setUpdated(new Date())
    feed.addAuthor("Repose")
  }

  def finishSetup(): Unit = {
    val sw = new StringWriter()
    feed.writeTo(sw)

    mockAtomFeedService.requestHandler = {
      case HttpRequest(_, Uri.Path("/feed"), _, _, _) =>
        HttpResponse(entity = HttpEntity(MediaTypes.`application/atom+xml`, sw.toString))

      case HttpRequest(_, _, _, _, _) =>
        HttpResponse(404, entity = "Not Found")
    }

    mockAtomFeedService.start()

    val authType = new AuthenticationType()
    authType.setFqcn("org.openrepose.nodeservice.atomfeed.impl.auth.NoopAuthenticatedRequestFactory")

    actorRef = TestActorRef(
      new FeedReader(mockAtomFeedService.getUrl + "/feed",
        actorRefFactory =>
          actorRefFactory.actorOf(Authenticator.props(authType)),
        _ => notifierProbe.ref,
        EntryOrderType.RANDOM)
    )
  }

  test("the notifier actor should not receive a message when a old atom entry is found") {
    val entry = feed.addEntry()
    entry.setId("tag:openrepose.org,2007:/feed/entries/1")
    entry.setTitle("Test Title")
    entry.addAuthor("Repose")
    entry.setSummary("This is a test")
    entry.setContent("This is a test")
    entry.setUpdated(new Date())
    entry.setPublished(new Date())

    finishSetup()

    actorRef ! ReadFeed

    notifierProbe.expectNoMsg()
  }

  test("the notifier actor should receive a message when a new atom entry is found") {
    val entry = feed.addEntry()
    entry.setId("tag:openrepose.org,2007:/feed/entries/1")
    entry.setTitle("Test Title")
    entry.addAuthor("Repose")
    entry.setSummary("This is a test")
    entry.setContent("This is a test")
    entry.setUpdated(new Date(System.currentTimeMillis + 60000))
    entry.setPublished(new Date(System.currentTimeMillis + 60000))

    finishSetup()

    actorRef ! ReadFeed

    notifierProbe.expectMsgClass(classOf[NotifyListeners])
  }

  test("only new atom entries should be sent") {
    val oldEntry = feed.addEntry()
    oldEntry.setId("tag:openrepose.org,2007:/feed/entries/1")
    oldEntry.setTitle("Old Entry")
    oldEntry.addAuthor("Repose")
    oldEntry.setSummary("This is a test")
    oldEntry.setContent("This is a test")
    oldEntry.setUpdated(new Date())
    oldEntry.setPublished(new Date())

    val newEntry = feed.addEntry()
    newEntry.setId("tag:openrepose.org,2007:/feed/entries/2")
    newEntry.setTitle("New Entry")
    newEntry.addAuthor("Repose")
    newEntry.setSummary("This is a test")
    newEntry.setContent("This is a test")
    newEntry.setUpdated(new Date(System.currentTimeMillis + 60000))
    newEntry.setPublished(new Date(System.currentTimeMillis + 60000))

    finishSetup()

    actorRef ! ReadFeed

    val entries = notifierProbe.expectMsgClass(classOf[NotifyListeners]).atomEntries
    assert(entries.size == 1)
  }

  test("the newest atom entries should come first in the list") {
    val oldEntry = feed.addEntry()
    oldEntry.setId("tag:openrepose.org,2007:/feed/entries/1")
    oldEntry.setTitle("Old Entry")
    oldEntry.addAuthor("Repose")
    oldEntry.setSummary("This is a test")
    oldEntry.setContent("This is a test")
    oldEntry.setUpdated(new Date(System.currentTimeMillis + 60000))
    oldEntry.setPublished(new Date(System.currentTimeMillis + 60000))

    val oesw = new StringWriter()
    oldEntry.writeTo(oesw)
    val oldEntryHash = oesw.toString.hashCode

    val newEntry = feed.addEntry()
    newEntry.setId("tag:openrepose.org,2007:/feed/entries/2")
    newEntry.setTitle("New Entry")
    newEntry.addAuthor("Repose")
    newEntry.setSummary("This is a test")
    newEntry.setContent("This is a test")
    newEntry.setUpdated(new Date(System.currentTimeMillis + 30000))
    newEntry.setPublished(new Date(System.currentTimeMillis + 30000))

    val nesw = new StringWriter()
    newEntry.writeTo(nesw)
    val newEntryHash = nesw.toString.hashCode

    finishSetup()

    actorRef ! ReadFeed

    val entries = notifierProbe.expectMsgClass(classOf[NotifyListeners]).atomEntries
    assert(entries.map(_.hashCode) == List(newEntryHash, oldEntryHash))
  }
}
