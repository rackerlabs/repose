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
import java.net.URI
import java.util.Date

import akka.http.scaladsl.model._
import org.apache.abdera.Abdera
import org.apache.abdera.model.Feed
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.HttpClients
import org.junit.runner.RunWith
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.{AdditionalAnswers, ArgumentCaptor}
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.nodeservice.atomfeed.impl.auth.AuthenticationRequestContextImpl
import org.openrepose.nodeservice.atomfeed.{AuthenticatedRequestFactory, AuthenticationRequestContext, AuthenticationRequestException, FeedReadRequest}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSuite}

import scala.concurrent.TimeoutException
import scala.concurrent.duration._
import scala.language.postfixOps

@RunWith(classOf[JUnitRunner])
class AtomEntryStreamBuilderTest extends FunSuite with BeforeAndAfterEach with MockitoSugar {

  val mockAuthRequestFactory = mock[AuthenticatedRequestFactory]
  val abdera = Abdera.getInstance()

  var mockAtomFeedService: MockService = _
  var httpClient: HttpClient = _
  var feed: Feed = _

  override def beforeEach() = {
    httpClient = HttpClients.createDefault()

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
        HttpResponse(entity = HttpEntity(ContentType.WithMissingCharset(MediaTypes.`application/atom+xml`), sw.toString.getBytes))

      case HttpRequest(_, _, _, _, _) =>
        HttpResponse(404, entity = "Not Found")
    }

    mockAtomFeedService.start()
  }

  test("should throw an exception if authenticating the request takes too long") {
    finishSetup()

    val waitingAuthFactory = Some(new AuthenticatedRequestFactory {
      override def onInvalidCredentials(): Unit = ???

      override def authenticateRequest(feedReadRequest: FeedReadRequest, context: AuthenticationRequestContext): FeedReadRequest = {
        // Infinite loop to prove that the Future safeguard actually bounds the authenticator
        while (true) {
          Thread.sleep(1000)
        }
        feedReadRequest
      }
    })

    intercept[TimeoutException] {
      AtomEntryStreamBuilder.build(new URI(mockAtomFeedService.getUrl + "/feed"), httpClient, AuthenticationRequestContextImpl("requestId", "1.0"), waitingAuthFactory, 100 millis)
    }
  }

  test("should add a tracing header to the request to the Atom service") {
    finishSetup()

    val capturedfeedReadRequest = ArgumentCaptor.forClass(classOf[FeedReadRequest])

    reset(mockAuthRequestFactory)
    when(mockAuthRequestFactory.authenticateRequest(capturedfeedReadRequest.capture(), any[AuthenticationRequestContext]))
      .thenAnswer(AdditionalAnswers.returnsFirstArg())

    AtomEntryStreamBuilder.build(new URI(mockAtomFeedService.getUrl + "/feed"), httpClient, AuthenticationRequestContextImpl("requestId", "1.0"), Some(mockAuthRequestFactory))

    assert(!capturedfeedReadRequest.getValue.getHeaders.get(CommonHttpHeader.TRACE_GUID).isEmpty)
  }

  test("should throw an AuthenticationException if the factory returns null") {
    reset(mockAuthRequestFactory)
    when(mockAuthRequestFactory.authenticateRequest(any[FeedReadRequest], any[AuthenticationRequestContext]))
      .thenThrow(new AuthenticationRequestException("Failed to authenticate the request."))

    finishSetup()

    intercept[AuthenticationRequestException] {
      AtomEntryStreamBuilder.build(new URI(mockAtomFeedService.getUrl + "/feed"), httpClient, AuthenticationRequestContextImpl("requestId", "1.0"), Some(mockAuthRequestFactory))
    }
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

    val entryStream = AtomEntryStreamBuilder.build(new URI(mockAtomFeedService.getUrl + "/feed"), httpClient, AuthenticationRequestContextImpl("requestId", "1.0"))

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

    val entryStream = AtomEntryStreamBuilder.build(new URI(mockAtomFeedService.getUrl + "/feed"), httpClient, AuthenticationRequestContextImpl("requestId", "1.0"))

    assert(entryStream.head.getContent.equals("entryOne"))
    assert(entryStream.drop(1).head.getContent.equals("entryTwo"))
  }

  test("should retrieve all entries from a paged feed in order") {
    mockAtomFeedService.start()

    val feedPageOne = abdera.newFeed()
    feedPageOne.setId("tag:openrepose.org,2007:/feed")
    feedPageOne.setTitle("Test Title")
    feedPageOne.setSubtitle("Test Subtitle")
    feedPageOne.setUpdated(new Date())
    feedPageOne.addAuthor("Repose")
    feedPageOne.addLink(mockAtomFeedService.getUrl + "/feed2", "next")

    val feedPageTwo = abdera.newFeed()
    feedPageTwo.setId("tag:openrepose.org,2007:/feed2")
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
        HttpResponse(entity = HttpEntity(ContentType.WithMissingCharset(MediaTypes.`application/atom+xml`), swpo.toString.getBytes))

      case HttpRequest(_, Uri.Path("/feed2"), _, _, _) =>
        HttpResponse(entity = HttpEntity(ContentType.WithMissingCharset(MediaTypes.`application/atom+xml`), swpt.toString.getBytes))

      case HttpRequest(_, _, _, _, _) =>
        HttpResponse(404, entity = "Not Found")
    }

    val entryStream = AtomEntryStreamBuilder.build(new URI(mockAtomFeedService.getUrl + "/feed"), httpClient, AuthenticationRequestContextImpl("requestId", "1.0"))

    assert(entryStream.head.getContent.equals("entryOne"))
    assert(entryStream.drop(1).head.getContent.equals("entryTwo"))
  }

  test("should callback on the authentication mechanism on bad credentials") {
    mockAtomFeedService.start()

    mockAtomFeedService.requestHandler = {
      case HttpRequest(_, Uri.Path("/feed"), _, _, _) =>
        HttpResponse(403)

      case HttpRequest(_, _, _, _, _) =>
        HttpResponse(404, entity = "Not Found")
    }

    reset(mockAuthRequestFactory)
    when(mockAuthRequestFactory.authenticateRequest(any[FeedReadRequest], any[AuthenticationRequestContext]))
      .thenAnswer(AdditionalAnswers.returnsFirstArg())

    intercept[AtomEntryStreamBuilder.ClientErrorException] {
      AtomEntryStreamBuilder.build(new URI(mockAtomFeedService.getUrl + "/feed"), httpClient, AuthenticationRequestContextImpl("", ""), Some(mockAuthRequestFactory))
    }

    verify(mockAuthRequestFactory, times(2)).onInvalidCredentials()
    verify(mockAuthRequestFactory, times(2)).authenticateRequest(any[FeedReadRequest], any[AuthenticationRequestContext])
  }

  test("should retry once if authentication credentials go bad after the initial page fetch") {
    mockAtomFeedService.start()

    val feedPageOne = abdera.newFeed()
    feedPageOne.setId("tag:openrepose.org,2007:/feed")
    feedPageOne.setTitle("Test Title")
    feedPageOne.setSubtitle("Test Subtitle")
    feedPageOne.setUpdated(new Date())
    feedPageOne.addAuthor("Repose")
    feedPageOne.addLink(mockAtomFeedService.getUrl + "/feed2", "next")

    val feedPageTwo = abdera.newFeed()
    feedPageTwo.setId("tag:openrepose.org,2007:/feed2")
    feedPageTwo.setTitle("Test Title")
    feedPageTwo.setSubtitle("Test Subtitle")
    feedPageTwo.setUpdated(new Date())
    feedPageTwo.addAuthor("Repose")

    val swpo = new StringWriter()
    val swpt = new StringWriter()
    feedPageOne.writeTo(swpo)
    feedPageTwo.writeTo(swpt)

    mockAtomFeedService.requestHandler = {
      case HttpRequest(_, Uri.Path("/feed"), _, _, _) =>
        HttpResponse(entity = HttpEntity(ContentType.WithMissingCharset(MediaTypes.`application/atom+xml`), swpo.toString.getBytes))

      case HttpRequest(_, Uri.Path("/feed2"), _, _, _) =>
        HttpResponse(403)

      case HttpRequest(_, _, _, _, _) =>
        HttpResponse(404, entity = "Not Found")
    }

    reset(mockAuthRequestFactory)
    when(mockAuthRequestFactory.authenticateRequest(any[FeedReadRequest], any[AuthenticationRequestContext]))
      .thenAnswer(AdditionalAnswers.returnsFirstArg())

    intercept[AtomEntryStreamBuilder.ClientErrorException] {
      AtomEntryStreamBuilder.build(new URI(mockAtomFeedService.getUrl + "/feed"), httpClient, AuthenticationRequestContextImpl("", ""), Some(mockAuthRequestFactory))
    }

    verify(mockAuthRequestFactory, times(2)).onInvalidCredentials()
    verify(mockAuthRequestFactory, times(3)).authenticateRequest(any[FeedReadRequest], any[AuthenticationRequestContext])
  }
}
