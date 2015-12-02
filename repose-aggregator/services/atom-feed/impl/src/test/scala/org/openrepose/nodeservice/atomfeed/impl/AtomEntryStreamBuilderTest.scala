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
package org.openrepose.nodeservice.atomfeed.impl

import java.io.StringWriter
import java.net.{URL, URLConnection}
import java.util.Date

import akka.http.scaladsl.model._
import org.apache.abdera.Abdera
import org.apache.abdera.model.Feed
import org.junit.runner.RunWith
import org.mockito.AdditionalAnswers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.openrepose.nodeservice.atomfeed.AuthenticatedRequestFactory
import org.openrepose.nodeservice.atomfeed.impl.actors.MockService
import org.openrepose.nodeservice.atomfeed.impl.auth.NoopAuthenticatedRequestFactory
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSuite}

@RunWith(classOf[JUnitRunner])
class AtomEntryStreamBuilderTest extends FunSuite with BeforeAndAfter with MockitoSugar {

  val mockAuthRequestFactory = mock[AuthenticatedRequestFactory]
  val abdera = Abdera.getInstance()

  var mockAtomFeedService: MockService = _
  var feed: Feed = _

  before {
    reset(mockAuthRequestFactory)
    when(mockAuthRequestFactory.authenticateRequest(any[URLConnection])).thenAnswer(AdditionalAnswers.returnsFirstArg())

    mockAtomFeedService = new MockService()

    feed = abdera.newFeed()
    feed.setId("tag:openrepose.org,2007:/feed")
    feed.setTitle("Test Title")
    feed.setSubtitle("Test Subtitle")
    feed.setUpdated(new Date())
    feed.addAuthor("Repose")
    feed.addLink(mockAtomFeedService.getUrl + "/feed?page=2", "next")
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
  }

  test("should retrieve all entries from a complete feed") {
    val entryOne = feed.addEntry()
    entryOne.setId("tag:openrepose.org,2007:/feed/entries/1")
    entryOne.setTitle("Test Title")
    entryOne.addAuthor("Test")
    entryOne.setContent("entryOne")
    entryOne.setUpdated(new Date())

    val entryTwo = feed.addEntry()
    entryTwo.setId("tag:openrepose.org,2007:/feed/entries/2")
    entryTwo.setTitle("Test Title")
    entryTwo.addAuthor("Test")
    entryTwo.setContent("entryTwo")
    entryTwo.setUpdated(new Date())

    finishSetup()

    val entryStream = AtomEntryStreamBuilder.build(new URL(mockAtomFeedService.getUrl + "/feed"), new NoopAuthenticatedRequestFactory())

    assert(entryStream.exists(entry => entry.getContent.equals("entryOne")))
    assert(entryStream.exists(entry => entry.getContent.equals("entryTwo")))
  }

  test("the order of events in the stream should match the top-down order of events in the feed") {
    val entryOne = feed.addEntry()
    entryOne.setId("tag:openrepose.org,2007:/feed/entries/1")
    entryOne.setTitle("Test Title")
    entryOne.addAuthor("Test")
    entryOne.setContent("entryOne")
    entryOne.setUpdated(new Date())

    val entryTwo = feed.addEntry()
    entryTwo.setId("tag:openrepose.org,2007:/feed/entries/2")
    entryTwo.setTitle("Test Title")
    entryTwo.addAuthor("Test")
    entryTwo.setContent("entryTwo")
    entryTwo.setUpdated(new Date())

    finishSetup()

    val entryStream = AtomEntryStreamBuilder.build(new URL(mockAtomFeedService.getUrl + "/feed"), new NoopAuthenticatedRequestFactory())

    assert(entryStream.head.getContent.equals("entryOne"))
    assert(entryStream.drop(1).head.getContent.equals("entryTwo"))
  }

  test("should retrieve all entries from a paged feed in order") {
    val feedPageOne = abdera.newFeed()
    feedPageOne.setId("tag:openrepose.org,2007:/feed")
    feedPageOne.setTitle("Test Title")
    feedPageOne.setSubtitle("Test Subtitle")
    feedPageOne.setUpdated(new Date())
    feedPageOne.addAuthor("Repose")
    feedPageOne.addLink(mockAtomFeedService.getUrl + "/feed?page=2", "next")

    val feedPageTwo = abdera.newFeed()
    feedPageTwo.setId("tag:openrepose.org,2007:/feed?page=2")
    feedPageTwo.setTitle("Test Title")
    feedPageTwo.setSubtitle("Test Subtitle")
    feedPageTwo.setUpdated(new Date())
    feedPageTwo.addAuthor("Repose")

    val entryOne = feedPageOne.addEntry()
    entryOne.setId("tag:openrepose.org,2007:/feed/entries/1")
    entryOne.setTitle("Test Title")
    entryOne.addAuthor("Test")
    entryOne.setContent("entryOne")
    entryOne.setUpdated(new Date())

    val entryTwo = feedPageTwo.addEntry()
    entryTwo.setId("tag:openrepose.org,2007:/feed/entries/2")
    entryTwo.setTitle("Test Title")
    entryTwo.addAuthor("Test")
    entryTwo.setContent("entryTwo")
    entryTwo.setUpdated(new Date())

    val swpo = new StringWriter()
    val swpt = new StringWriter()
    feedPageOne.writeTo(swpo)
    feedPageTwo.writeTo(swpt)

    mockAtomFeedService.requestHandler = {
      case HttpRequest(_, Uri.Path("/feed"), _, _, _) =>
        HttpResponse(entity = HttpEntity(MediaTypes.`application/atom+xml`, swpo.toString))

      case HttpRequest(_, Uri.Path("/feed?page=2", _, _, _)) =>
        HttpResponse(entity = HttpEntity(MediaTypes.`application/atom+xml`, swpt.toString))

      case HttpRequest(_, _, _, _, _) =>
        HttpResponse(404, entity = "Not Found")
    }

    mockAtomFeedService.start()

    val entryStream = AtomEntryStreamBuilder.build(new URL(mockAtomFeedService.getUrl + "/feed"), new NoopAuthenticatedRequestFactory())

    assert(entryStream.head.getContent.equals("entryOne"))
    assert(entryStream.drop(1).head.getContent.equals("entryTwo"))
  }

  ignore("should retrieve all entries from an archived feed")()
}
