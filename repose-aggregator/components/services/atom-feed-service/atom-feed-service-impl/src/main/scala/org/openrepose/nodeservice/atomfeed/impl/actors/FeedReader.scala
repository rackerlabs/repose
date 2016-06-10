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
import java.net.{URL, UnknownServiceException}
import org.apache.abdera.model.Entry

import akka.actor._
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.abdera.Abdera
import org.apache.abdera.i18n.iri.IRI
import org.apache.abdera.parser.ParseException
import org.openrepose.commons.utils.logging.TracingKey
import org.openrepose.docs.repose.atom_feed_service.v1.EntryOrderType
import org.openrepose.nodeservice.atomfeed.AuthenticatedRequestFactory
import org.openrepose.nodeservice.atomfeed.impl.AtomEntryStreamBuilder
import org.openrepose.nodeservice.atomfeed.impl.AtomEntryStreamBuilder.{AuthenticationException, UnrecoverableIOException}
import org.openrepose.nodeservice.atomfeed.impl.actors.Notifier.{FeedReaderActivated, FeedReaderCreated, FeedReaderDeactivated, FeedReaderDestroyed}
import org.openrepose.nodeservice.atomfeed.impl.actors.NotifierManager._
import org.openrepose.nodeservice.atomfeed.impl.auth.AuthenticationRequestContextImpl
import org.slf4j.MDC

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

object FeedReader {

  def props(feedUri: String,
            authenticatedRequestFactory: Option[AuthenticatedRequestFactory],
            notifierManager: ActorRef,
            pollingFrequency: Int,
            order: EntryOrderType,
            reposeVersion: String): Props =
    Props(new FeedReader(feedUri, authenticatedRequestFactory, notifierManager, pollingFrequency, order, reposeVersion))

  object ReadFeed

  object ScheduleReading

  object CancelScheduledReading

}

class FeedReader(feedUri: String,
                 authenticatedRequestFactory: Option[AuthenticatedRequestFactory],
                 notifierManager: ActorRef,
                 pollingFrequency: Int,
                 order: EntryOrderType,
                 reposeVersion: String)
  extends Actor with LazyLogging {

  import FeedReader._

  val parser = Abdera.getInstance().getParser
  val feedUrl = new URL(feedUri)

  var firstReadDone = false
  var highWaterMark: Option[IRI] = None
  var schedule: Option[Cancellable] = None

  override def preStart(): Unit = {
    // bind this feed reader to its corresponding notifier manager
    notifierManager ! BindFeedReader(self)
    notifierManager ! FeedReaderCreated
  }

  override def postStop(): Unit = {
    notifierManager ! FeedReaderDestroyed
  }

  override def receive: Receive = {
    case ReadFeed =>
      val requestId = java.util.UUID.randomUUID().toString
      MDC.put(TracingKey.TRACING_KEY, requestId)

      try {
        def getMyStream(): Stream[Entry] = {
          AtomEntryStreamBuilder.build(feedUrl, AuthenticationRequestContextImpl(reposeVersion, requestId), authenticatedRequestFactory)
        }

        val entryStream = Option(order) match {
          case Some(reverse) if reverse.value().equals("reverse-read") =>
            getMyStream().reverse
          case _ =>
            getMyStream()
        }

        if (firstReadDone) {
          val newEntryStream = highWaterMark match {
            case Some(mark) => entryStream.takeWhile(entry => !entry.getId.equals(mark))
            case None => entryStream
          }

          newEntryStream foreach { entry =>
            val entryString = new StringWriter()
            entry.writeTo(entryString)
            // todo: wrap atom entry in type-safe object
            notifierManager ! Notify(entryString.toString)
          }
        }

        firstReadDone = true
        highWaterMark = entryStream.headOption.map(_.getId)
      } catch {
        case AuthenticationException =>
          logger.error("Authentication failed -- connection to Atom service could not be established")
        case e@(_: UnknownServiceException | _: IOException | _: UnrecoverableIOException) =>
          logger.error("Connection to Atom service failed -- an invalid URI may have been provided, or " +
            "authentication credentials may be invalid", e)
        case pe: ParseException =>
          logger.error("Failed to parse the Atom feed", pe)
        case e: Exception =>
          logger.error("Feed was unable to be read", e)
      }
    case ScheduleReading =>
      schedule = schedule orElse {
        logger.info("Scheduled feed reader for URI: " + feedUri)
        val newSchedule = Some(context.system.scheduler.schedule(
          Duration.Zero,
          pollingFrequency.seconds,
          self,
          ReadFeed
        ))

        notifierManager ! FeedReaderActivated
        newSchedule
      }
    case CancelScheduledReading =>
      schedule foreach { cancellable =>
        logger.info("Cancelled scheduled feed reader for URI: " + feedUri)
        cancellable.cancel()
        notifierManager ! FeedReaderDeactivated
      }
      schedule = None
      highWaterMark = None
      firstReadDone = false
  }
}
