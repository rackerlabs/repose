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

package org.openrepose.filters.uristripper

import com.typesafe.scalalogging.StrictLogging
import javax.xml.transform.ErrorListener
import javax.xml.transform.TransformerException
import org.xml.sax.SAXParseException


private object LogErrorListener {
  val trace   = "^\\[TRACE\\]\\s+(.*)".r
  val debug   = "^\\[DEBUG\\]\\s+(.*)".r
  val info    = "^\\[INFO\\]\\s+(.*)".r
  val warning = "^\\[WARNING\\]\\s+(.*)".r
  val error   = "^\\[ERROR\\]\\s+(.*)".r
  val se      = "^\\[SE\\]\\s+(.*)".r
}

import LogErrorListener._

class LogErrorListener extends ErrorListener with StrictLogging {

  private def logException(e : TransformerException, default : => Unit) : Unit = {
    e.getMessage() match {
      case trace(m) => logger.trace(m)
      case debug(m) =>  logger.debug(m)
      case info(m) => logger.info(m)
      case warning(m) => logger.warn(m)
      case error(m) => logger.error(m)
      case se(m) => logger.error(m)
        throw new SAXParseException (m, null)
      case s : String => default
    }
  }

  override def error (exception : TransformerException) : Unit =
    logException(exception, logger.error(exception.getMessage()))

  override def fatalError (exception : TransformerException) : Unit =
    logException(exception, logger.error(exception.getMessage()))

  override def warning (exception : TransformerException) : Unit =
    logException(exception, logger.warn(exception.getMessage()))

}
