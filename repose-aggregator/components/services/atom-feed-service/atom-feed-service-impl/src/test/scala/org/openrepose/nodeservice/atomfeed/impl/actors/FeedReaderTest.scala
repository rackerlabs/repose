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
import io.opentracing.Tracer.SpanBuilder
import io.opentracing.{Scope, Span, Tracer}
import org.apache.abdera.Abdera
import org.apache.abdera.model.Feed
import org.apache.http
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.impl.client.HttpClients
import org.junit.runner.RunWith
import org.mockito.AdditionalAnswers
import org.mockito.Matchers.{any, anyBoolean, anyString}
import org.mockito.Mockito.{reset, verify, when}
import org.openrepose.commons.utils.logging.TracingKey
import org.openrepose.core.services.httpclient.{HttpClientService, HttpClientServiceClient}
import org.openrepose.docs.repose.atom_feed_service.v1.EntryOrderType
import org.openrepose.nodeservice.atomfeed.impl.MockService
import org.openrepose.nodeservice.atomfeed.impl.actors.FeedReader.{CancelScheduledReading, ReadFeed, ScheduleReading}
import org.openrepose.nodeservice.atomfeed.impl.actors.Notifier._
import org.openrepose.nodeservice.atomfeed.impl.actors.NotifierManager.{BindFeedReader, Notify}
import org.openrepose.nodeservice.atomfeed.{AuthenticatedRequestFactory, AuthenticationRequestContext, FeedReadRequest}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSuiteLike}
import org.slf4j.MDC

import scala.concurrent.duration._
import scala.language.postfixOps

@RunWith(classOf[JUnitRunner])
class FeedReaderTest(_system: ActorSystem)
  extends TestKit(_system) with FunSuiteLike with BeforeAndAfterEach with MockitoSugar {

  val mockAuthRequestFactory = mock[AuthenticatedRequestFactory]
  val notifierProbe = TestProbe()
  val abdera = Abdera.getInstance()

  var feed: Feed = _
  var actorRef: TestActorRef[FeedReader] = _
  var mockAtomFeedService: MockService = _
  var mockHttpClientService: HttpClientService = _
  var mockTracer: Tracer = _
  var mockSpanBuilder: SpanBuilder = _
  var mockScope: Scope = _
  var mockSpan: Span = _

  def this() = this(ActorSystem("FeedReaderTest"))

  override def beforeEach() = {
    mockHttpClientService = mock[HttpClientService]
    when(mockHttpClientService.getClient(anyString)).thenReturn(getTestClient)

    mockTracer = mock[Tracer]
    mockSpanBuilder = mock[SpanBuilder]
    mockScope = mock[Scope]
    mockSpan = mock[Span]
    when(mockTracer.buildSpan(anyString())).thenReturn(mockSpanBuilder)
    when(mockSpanBuilder.withTag(anyString(), anyString())).thenReturn(mockSpanBuilder)
    when(mockSpanBuilder.ignoreActiveSpan()).thenReturn(mockSpanBuilder)
    when(mockSpanBuilder.startActive(anyBoolean())).thenReturn(mockScope)
    when(mockScope.span()).thenReturn(mockSpan)

    reset(mockAuthRequestFactory)
    when(mockAuthRequestFactory.authenticateRequest(any[FeedReadRequest], any[AuthenticationRequestContext]))
      .thenAnswer(AdditionalAnswers.returnsFirstArg())

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

    actorRef = TestActorRef(
      new FeedReader(mockAtomFeedService.getUrl + "/feed",
        mockHttpClientService,
        mockTracer,
        "",
        Some(mockAuthRequestFactory),
        1 second,
        notifierProbe.ref,
        5,
        EntryOrderType.READ,
        "1.0")
    )
  }

  test("should bind to the notifier manager and send a lifecycle update when started") {
    actorRef = TestActorRef(
      new FeedReader("http://test.url/feed",
        mockHttpClientService,
        mockTracer,
        "",
        Some(mockAuthRequestFactory),
        1 second,
        notifierProbe.ref,
        5,
        EntryOrderType.READ,
        "1.0")
    )

    notifierProbe.expectMsgClass(classOf[BindFeedReader])
    notifierProbe.expectMsg(FeedReaderCreated)
  }

  test("should send a lifecycle update when stopped") {
    actorRef = TestActorRef(
      new FeedReader("http://test.url/feed",
        mockHttpClientService,
        mockTracer,
        "",
        Some(mockAuthRequestFactory),
        1 second,
        notifierProbe.ref,
        5,
        EntryOrderType.READ,
        "1.0")
    )
    actorRef.stop()

    notifierProbe.fishForMessage() {
      case FeedReaderDestroyed => true
      case _ => false
    }
  }

  test("schedule reading should schedule this feed to be read and send a lifecycle message to the notifier manager") {
    finishSetup()

    actorRef ! ScheduleReading

    assert(actorRef.underlyingActor.schedule.isDefined)
    notifierProbe.fishForMessage() {
      case FeedReaderActivated => true
      case _ => false
    }
  }

  test("cancelling scheduled reading should prevent this feed from being read and send a lifecycle message to the notifier manager") {
    finishSetup()

    actorRef ! ScheduleReading
    actorRef ! CancelScheduledReading

    assert(actorRef.underlyingActor.schedule.isEmpty)
    notifierProbe.fishForMessage(hint = "feed reader deactivated") {
      case FeedReaderDeactivated => true
      case _ => false
    }
  }

  test("should add the request id to slf4j MDC") {
    finishSetup()

    actorRef ! ReadFeed

    assert(MDC.get(TracingKey.TRACING_KEY) != null)
    notifierProbe.receiveWhile(500 milliseconds) {
      case _ => true
    }
  }

  test("should start a new trace span when reading the feed") {
    finishSetup()

    actorRef ! ReadFeed

    verify(mockSpanBuilder).ignoreActiveSpan()
    verify(mockSpanBuilder).startActive(true)
    verify(mockScope).close()
    notifierProbe.receiveWhile(500 milliseconds) {
      case _ => true
    }
  }

  test("the notifier actor should not receive a message when an old atom entry is found") {
    notifierProbe.ignoreMsg {
      case _: BindFeedReader => true
      case Notifier.FeedReaderCreated => true
    }

    val entryOne = feed.addEntry()
    entryOne.setId("tag:openrepose.org,2007:/feed/entries/1")
    entryOne.setTitle("Test Title")
    entryOne.addAuthor("Repose")
    entryOne.setSummary("This is a test")
    entryOne.setContent("This is a test")
    entryOne.setUpdated(new Date())
    entryOne.setPublished(new Date())

    finishSetup()

    actorRef ! ReadFeed

    notifierProbe.expectNoMsg()
  }

  test("the notifier actor should receive a message when a new atom entry is found after initial empty feed") {
    finishSetup()

    actorRef ! ReadFeed

    val entry = feed.addEntry()
    entry.setId("tag:openrepose.org,2007:/feed/entries/1")
    entry.setTitle("Test Title")
    entry.addAuthor("Repose")
    entry.setSummary("This is a test")
    entry.setContent("This is a test")
    entry.setUpdated(new Date(System.currentTimeMillis + 60000))
    entry.setPublished(new Date(System.currentTimeMillis + 60000))

    val sw = new StringWriter()
    feed.writeTo(sw)

    mockAtomFeedService.requestHandler = {
      case HttpRequest(_, Uri.Path("/feed"), _, _, _) =>
        HttpResponse(entity = HttpEntity(MediaTypes.`application/atom+xml`, sw.toString))

      case HttpRequest(_, _, _, _, _) =>
        HttpResponse(404, entity = "Not Found")
    }

    actorRef ! ReadFeed

    notifierProbe.expectMsgClass(classOf[Notify])
    notifierProbe.expectNoMsg()
  }

  test("the notifier actor should receive a message when a new atom entry is found after initial populated feed") {
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

    val entryTwo = feed.insertEntry()
    entryTwo.setId("tag:openrepose.org,2007:/feed/entries/2")
    entryTwo.setTitle("Test Title")
    entryTwo.addAuthor("Repose")
    entryTwo.setSummary("This is a test")
    entryTwo.setContent("This is a test")
    entryTwo.setUpdated(new Date(System.currentTimeMillis + 60000))
    entryTwo.setPublished(new Date(System.currentTimeMillis + 60000))

    val sw = new StringWriter()
    feed.writeTo(sw)

    mockAtomFeedService.requestHandler = {
      case HttpRequest(_, Uri.Path("/feed"), _, _, _) =>
        HttpResponse(entity = HttpEntity(MediaTypes.`application/atom+xml`, sw.toString))

      case HttpRequest(_, _, _, _, _) =>
        HttpResponse(404, entity = "Not Found")
    }

    actorRef ! ReadFeed

    notifierProbe.expectMsgClass(classOf[Notify])
    notifierProbe.expectNoMsg()
  }

  test("the newest atom entries should come first in the list") {
    finishSetup()

    actorRef ! ReadFeed

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

    val newEntry = feed.insertEntry()
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

    val sw = new StringWriter()
    feed.writeTo(sw)

    mockAtomFeedService.requestHandler = {
      case HttpRequest(_, Uri.Path("/feed"), _, _, _) =>
        HttpResponse(entity = HttpEntity(MediaTypes.`application/atom+xml`, sw.toString))

      case HttpRequest(_, _, _, _, _) =>
        HttpResponse(404, entity = "Not Found")
    }

    actorRef ! ReadFeed

    val messages = notifierProbe.expectMsgAllClassOf(classOf[Notify], classOf[Notify]).toArray
    notifierProbe.expectNoMsg()
    assert(messages(0).atomEntry.hashCode == newEntryHash)
    assert(messages(1).atomEntry.hashCode == oldEntryHash)
  }

  test("the newest atom entries should come last in the list (reverse-read)") {
    finishSetup()

    actorRef = TestActorRef(
      new FeedReader(mockAtomFeedService.getUrl + "/feed",
        mockHttpClientService,
        mockTracer,
        "",
        Some(mockAuthRequestFactory),
        1 second,
        notifierProbe.ref,
        5,
        EntryOrderType.REVERSE_READ,
        "1.0")
    )

    actorRef ! ReadFeed

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

    val newEntry = feed.insertEntry()
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

    val sw = new StringWriter()
    feed.writeTo(sw)

    mockAtomFeedService.requestHandler = {
      case HttpRequest(_, Uri.Path("/feed"), _, _, _) =>
        HttpResponse(entity = HttpEntity(MediaTypes.`application/atom+xml`, sw.toString))

      case HttpRequest(_, _, _, _, _) =>
        HttpResponse(404, entity = "Not Found")
    }

    actorRef ! ReadFeed

    val messages = notifierProbe.expectMsgAllClassOf(classOf[Notify], classOf[Notify]).toArray
    notifierProbe.expectNoMsg()
    assert(messages(0).atomEntry.hashCode == oldEntryHash)
    assert(messages(1).atomEntry.hashCode == newEntryHash)
  }

  def getTestClient: HttpClientServiceClient = new HttpClientServiceClient(null, null, null) {
    val client: HttpClient = HttpClients.createDefault()

    override def execute(request: HttpUriRequest): http.HttpResponse = client.execute(request)
  }
}
