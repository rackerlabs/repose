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

import java.io.IOException
import java.net.URL

import org.apache.abdera.Abdera
import org.apache.abdera.model.{Entry, Feed}
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.commons.utils.logging.TracingHeaderHelper
import org.openrepose.nodeservice.atomfeed.{AuthenticatedRequestFactory, AuthenticationRequestContext}

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.Try

/**
  * A container object for the build function.
  */
object AtomEntryStreamBuilder {

  private val parser = Abdera.getInstance().getParser

  /**
    * Calling this function will retrieve the first page of the Atom feed located at the provided baseFeedUrl.
    * Subsequent pages will be retrieved lazily. To keep a reference to a Stream produced by this function without
    * making a request to the Atom feed, assign the result of this function to a lazy variable like so:
    * lazy val myFeed = build("http://my.feed.url", MyAuthenticatedRequestFactory)
    *
    * @param baseFeedUrl             a static locator pointing to the "head" of an Atom feed (e.g., the subscription document)
    * @param context                 a context object which contains information related to the request
    * @param authenticator           a URL processor which authenticates connections
    * @param authenticationTimeLimit a maximum [[Duration]] to wait on authentication before considering authentication
    *                                a failure
    * @return a Scala [[scala.collection.immutable.Stream]] which will yield Atom entries in an Atom feed located at the
    *         provided baseFeedUrl. The entries will be yielded in the order in which they are read from the feed XML,
    *         from top to bottom.
    * @throws AuthenticationException when the authenticator fails to authenticate a request
    */
  def build(baseFeedUrl: URL,
            context: AuthenticationRequestContext,
            authenticator: Option[AuthenticatedRequestFactory] = None,
            authenticationTimeLimit: Duration = 1 second): Stream[Entry] = {
    buildR(baseFeedUrl, context, authenticator, authenticationTimeLimit)
  }

  private def buildR(baseFeedUrl: URL,
                     context: AuthenticationRequestContext,
                     authenticator: Option[AuthenticatedRequestFactory] = None,
                     authenticationTimeLimit: Duration = 1 second,
                     firstAttempt: Boolean = true): Stream[Entry] = {
    val baseFeedConnection = baseFeedUrl.openConnection()

    val authenticatedConnection = authenticator match {
      case Some(arf) =>
        val connectionFuture = Future(arf.authenticateRequest(baseFeedConnection, context))
        Option(Await.result(connectionFuture, authenticationTimeLimit))
      case None =>
        Some(baseFeedConnection)
    }

    authenticatedConnection match {
      case Some(urlConnection) =>
        val tracingHeader = TracingHeaderHelper.createTracingHeader(context.getRequestId, "1.1 Repose (Repose/" + context.getReposeVersion + ")", None)
        urlConnection.setRequestProperty(CommonHttpHeader.TRACE_GUID.toString, tracingHeader)

        try {
          val feedInputStream = urlConnection.getInputStream
          val feed = parser.parse[Feed](feedInputStream).getRoot

          feed.getLinks.find(link => link.getRel.equals("next")) match {
            case Some(nextPageLink) =>
              feed.getEntries.toStream #::: buildR(nextPageLink.getResolvedHref.toURL, context, authenticator, authenticationTimeLimit)
            case None =>
              feed.getEntries.toStream
          }
        } catch {
          case ioe: IOException =>
            authenticator.foreach(_.onInvalidCredentials())
            if (firstAttempt) {
              // Inform the authenticator that the request failed and retry once
              buildR(baseFeedUrl, context, authenticator, authenticationTimeLimit, firstAttempt = false)
            } else {
              throw UnrecoverableIOException(ioe)
            }
        }
      case None =>
        throw AuthenticationException
    }
  }

  case class UnrecoverableIOException(cause: Throwable) extends RuntimeException(cause)

  object AuthenticationException extends Exception

}
