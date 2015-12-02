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

import java.net.URL

import org.apache.abdera.Abdera
import org.apache.abdera.model.{Entry, Feed}
import org.openrepose.nodeservice.atomfeed.AuthenticatedRequestFactory

import scala.collection.JavaConversions._

/**
  * A container object for the build function.
  */
object AtomEntryStreamBuilder {

  private val parser = Abdera.getInstance().getParser

  /**
    * @param baseFeedUrl a static locator pointing to the "head" of an Atom feed (e.g., the subscription document)
    * @param authenticator a URL processor which authenticates connections
    * @return a Scala [[scala.collection.immutable.Stream]] which will yield Atom entries in an Atom feed located at the
    *         provided baseFeedUrl. The entries will be yielded in the order in which they are read from the feed XML,
    *         from top to bottom.
    */
  def build(baseFeedUrl: URL, authenticator: AuthenticatedRequestFactory): Stream[Entry] = {
    val feedInputStream = authenticator.authenticateRequest(baseFeedUrl.openConnection()).getInputStream
    val feed = parser.parse[Feed](feedInputStream).getRoot
    feedInputStream.close()

    feed.getLinks.find(link => link.getRel.equals("next")) match {
      case Some(nextPageLink) =>
        feed.getEntries.toStream #::: build(nextPageLink.getResolvedHref.toURL, authenticator)
      case None =>
        feed.getEntries.toStream
    }
  }
}
