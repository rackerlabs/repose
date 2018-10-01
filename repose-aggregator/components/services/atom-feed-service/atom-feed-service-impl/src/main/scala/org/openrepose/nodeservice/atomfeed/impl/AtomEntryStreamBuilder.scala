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

import java.io.ByteArrayInputStream
import java.net.URI
import java.util

import org.apache.abdera.Abdera
import org.apache.abdera.model.{Entry, Feed}
import org.apache.abdera.parser.stax.util.FOMList
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.{HttpGet, RequestBuilder}
import org.apache.http.util.EntityUtils
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.commons.utils.logging.TracingHeaderHelper
import org.openrepose.nodeservice.atomfeed.{AuthenticatedRequestFactory, AuthenticationRequestContext, FeedReadRequest}

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

/**
  * A container object for the build function.
  */
object AtomEntryStreamBuilder {

  private val parser = Abdera.getInstance().getParser

  /**
    * Calling this function will retrieve the first page of the Atom feed located at the provided baseFeedURI.
    * Subsequent pages will be retrieved lazily. To keep a reference to a Stream produced by this function without
    * making a request to the Atom feed, assign the result of this function to a lazy variable like so:
    * lazy val myFeed = build("http://my.feed.url", MyAuthenticatedRequestFactory)
    *
    * @param baseFeedURI           a static locator pointing to the "head" of an Atom feed (e.g., the subscription document)
    * @param context               a context object which contains information related to the request
    * @param authenticator         a URL processor which authenticates connections
    * @param authenticationTimeout a maximum [[Duration]] to wait on authentication before considering authentication
    *                              a failure
    * @return a Scala [[scala.collection.immutable.Stream]] which will yield Atom entries in an Atom feed located at the
    *         provided baseFeedURI. The entries will be yielded in the order in which they are read from the feed XML,
    *         from top to bottom.
    * @throws AuthenticationException when the authenticator fails to authenticate a request
    */
  def build(baseFeedURI: URI,
            httpClient: HttpClient,
            context: AuthenticationRequestContext,
            authenticator: Option[AuthenticatedRequestFactory] = None,
            authenticationTimeout: Duration = 1 second): Stream[Entry] = {
    buildR(baseFeedURI, httpClient, context, authenticator, authenticationTimeout)
  }

  private def buildR(feedURI: URI,
                     httpClient: HttpClient,
                     context: AuthenticationRequestContext,
                     authenticator: Option[AuthenticatedRequestFactory] = None,
                     authenticationTimeout: Duration = 1 second,
                     firstAttempt: Boolean = true): Stream[Entry] = {
    val feedReadRequest = new FeedReadRequest(feedURI)

    val authenticatedRequest = authenticator match {
      case Some(arf) =>
        val connectionFuture = Future(arf.authenticateRequest(feedReadRequest, context))
        Option(Await.result(connectionFuture, authenticationTimeout))
      case None =>
        Some(feedReadRequest)
    }

    authenticatedRequest match {
      case Some(readRequest) =>
        val tracingHeader = TracingHeaderHelper.createTracingHeader(context.getRequestId, "1.1 Repose (Repose/" + context.getReposeVersion + ")", None)
        feedReadRequest.getHeaders.put(CommonHttpHeader.TRACE_GUID, util.Arrays.asList(tracingHeader))

        val httpGet = RequestBuilder.get(feedReadRequest.getURI).build()
        feedReadRequest.getHeaders foreach { case (key, values) =>
          values.foreach(value => httpGet.addHeader(key, value))
        }

        val httpResponse = httpClient.execute(httpGet)
        try {
          val statusCode = httpResponse.getStatusLine.getStatusCode

          if (statusCode >= 200 && statusCode < 300) {
            val content = Option(httpResponse.getEntity)
              .map(_.getContent)
              .getOrElse(new ByteArrayInputStream(Array.empty[Byte]))
            // todo: support character encodings other than UTF-8, based on the response Content-Type charset
            val feed = parser.parse[Feed](content).getRoot

            feed.getLinks.find(link => link.getRel.equals("next")) match {
              case Some(nextPageLink) =>
                             // V This cast is important, it forces the list to fully realize and not stream
                feed.getEntries.asInstanceOf[FOMList[Entry]].getAsList.toStream #::: buildR(nextPageLink.getResolvedHref.toURI, httpClient, context, authenticator, authenticationTimeout)
              case None =>
                             // V This cast is important, it forces the list to fully realize and not stream
                feed.getEntries.asInstanceOf[FOMList[Entry]].getAsList.toStream
            }
          } else if (statusCode >= 400 && statusCode < 500) {
            authenticator.foreach(_.onInvalidCredentials)
            if (firstAttempt) {
              // Inform the authenticator that the request failed and retry once
              buildR(feedURI, httpClient, context, authenticator, authenticationTimeout, firstAttempt = false)
            } else {
              throw ClientErrorException(s"Could not handle Atom service response code: $statusCode")
            }
          } else {
            throw ServerResponseException(s"Could not handle Atom service response code: $statusCode")
          }
        } finally {
          EntityUtils.consume(httpResponse.getEntity)
        }
      case None =>
        throw AuthenticationException("Authenticated Atom service request failed for unknown reasons.")
    }
  }

  case class UnrecoverableIOException(cause: Throwable) extends RuntimeException(cause)

  case class ServerResponseException(message: String) extends Exception(message)

  case class ClientErrorException(message: String) extends Exception(message)

  case class AuthenticationException(message: String) extends Exception(message)

}
