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
import java.net.URL
import java.util.Date

import akka.actor.{Actor, ActorRef, ActorRefFactory, Props}
import org.apache.abdera.Abdera
import org.apache.abdera.model.Feed
import org.openrepose.docs.repose.atom_feed_service.v1.EntryOrderType
import org.openrepose.nodeservice.atomfeed.impl.actors.Notifier.NotifyListeners

import scala.collection.JavaConversions._

object FeedReader {
  object ReadFeed

  def props(feedUri: String, notifierMaker: ActorRefFactory => ActorRef, order: EntryOrderType): Props =
    Props(new FeedReader(feedUri, notifierMaker, order))
}

class FeedReader(feedUri: String, notifierMaker: ActorRefFactory => ActorRef, order: EntryOrderType)
  extends Actor {

  import FeedReader._

  val notifier = notifierMaker(context)

  val abdera = Abdera.getInstance()
  val parser = abdera.getParser
  val feedUrl = new URL(feedUri)

  var highWaterMark: Date = new Date()

  override def receive: Receive = {
    case ReadFeed =>
      val feed = parser.parse[Feed](feedUrl.openStream(), feedUrl.toString).getRoot

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
  }
}
