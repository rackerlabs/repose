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
package org.openrepose.commons.utils.jmx

import java.util
import javax.management.{MalformedObjectNameException, ObjectName}

import com.typesafe.scalalogging.StrictLogging

import scala.util.Try

/**
  * A standard utility to generate [[ObjectName]] instances.
  */
object JmxObjectNameFactory extends StrictLogging {
  private val NameKey = "name"
  private val KeySegmentDelimiter = "\\."
  private val KeyFormat = "%03d"

  @throws[MalformedObjectNameException]
  def getName(domain: String, name: String): ObjectName = {
    (Try {
      //
      // Split the name on every period and give each segment its own property.
      // Doing so should give us unlimited nested buckets in JConsole.
      // Since the name argument is provided by the user, we always quote it.
      //
      val objectNameProperties = new util.Hashtable[String, String]()
      name.split(KeySegmentDelimiter).zipWithIndex foreach { case (nameSegment, index) =>
        objectNameProperties.put(KeyFormat.format(index + 1), ObjectName.quote(nameSegment))
      }

      // If for some reason the ObjectName is still a pattern, fall back to quoting the domain.
      var objectName = new ObjectName(domain, objectNameProperties)
      if (objectName.isPattern) {
        objectName = new ObjectName(ObjectName.quote(domain), objectNameProperties)
      }

      // Return the deterministic, ordered canonical name
      new ObjectName(objectName.getCanonicalName)
    } recover {
      case mone: MalformedObjectNameException =>
        logger.info("Unable to create ObjectName {} {} using our custom format", domain, name, mone)
        new ObjectName(domain, NameKey, ObjectName.quote(name))
    } recover {
      case mone: MalformedObjectNameException =>
        logger.warn("Unable to create ObjectName {} {}", domain, name, mone)
        throw mone
    }).get
  }
}
