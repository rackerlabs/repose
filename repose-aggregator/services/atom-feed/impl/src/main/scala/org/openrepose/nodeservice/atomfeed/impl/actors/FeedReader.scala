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

import java.io.{IOException, StringWriter}
import java.net.{URL, URLConnection, UnknownServiceException}
import java.util.Date

import akka.actor.{Actor, ActorRef, ActorRefFactory, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.abdera.Abdera
import org.apache.abdera.model.Feed
import org.apache.abdera.parser.ParseException
import org.openrepose.docs.repose.atom_feed_service.v1.EntryOrderType
import org.openrepose.nodeservice.atomfeed.impl.actors.Authenticator.{AuthenticateURLConnection, InvalidateCache}
import org.openrepose.nodeservice.atomfeed.impl.actors.Notifier.NotifyListeners

import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object FeedReader {
  object ReadFeed

  def props(feedUri: String,
            authenticatorMaker: ActorRefFactory => ActorRef,
            notifierMaker: ActorRefFactory => ActorRef,
            order: EntryOrderType): Props =
    Props(new FeedReader(feedUri, authenticatorMaker, notifierMaker, order))
}

class FeedReader(feedUri: String,
                 authenticatorMaker: ActorRefFactory => ActorRef,
                 notifierMaker: ActorRefFactory => ActorRef,
                 order: EntryOrderType)
  extends Actor with LazyLogging {

  import FeedReader._

  val authenticator = authenticatorMaker(context)
  val notifier = notifierMaker(context)

  val abdera = Abdera.getInstance()
  val parser = abdera.getParser
  val feedUrl = new URL(feedUri)

  var highWaterMark: Date = new Date()

  override def receive: Receive = {
    case ReadFeed =>
      // TODO: Configurable wait on authentication?
      implicit val timeout = Timeout(5 seconds)

      val connectionFuture = ask(authenticator, AuthenticateURLConnection(feedUrl.openConnection())).mapTo[URLConnection]
      val authenticatedConnection = Option(Await.result(connectionFuture, timeout.duration))

      authenticatedConnection match {
        case Some(connection) =>
          try {
            val feed = parser.parse[Feed](connection.getInputStream, feedUrl.toString).getRoot

            // According to RFC 4287, "this specification assigns no significance to the order of atom:entry elements within
            // the feed."
            // So we'll sort the list of all atom entries and only take those which have been updated more recently
            // than the last time the feed was read.
            //
            // The order configuration item is not currently used. Notifying listeners of entries based on the time at which
            // they were last updated satisfies both "random" and "update" orderings.
            val newEntries = feed.sortEntriesByUpdated(true).getEntries.toList.takeWhile(_.getUpdated.after(highWaterMark))

            newEntries.headOption foreach { newestEntry =>
              highWaterMark = newestEntry.getUpdated

              val newEntryStrings = newEntries map { atomEntry =>
                val entryString = new StringWriter()
                atomEntry.writeTo(entryString)
                entryString.toString
              }

              notifier ! NotifyListeners(newEntryStrings.reverse)
            }
          } catch {
            case e@(_: UnknownServiceException | _: IOException) =>
              logger.warn("Connection to Atom service failed -- an invalid URI may have been provided, or " +
                "authentication credentials may be invalid", e)
              authenticator ! InvalidateCache
            case pe: ParseException =>
              logger.warn("Failed to parse the Atom feed", pe)
          }
        case None =>
          logger.error("Authentication failed -- connection to Atom service could not be established")
      }
  }
}
