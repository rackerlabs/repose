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
package org.openrepose.filters.ratelimiting

import java.io.InputStream

import play.api.libs.json.{JsError, JsSuccess, Json}

import scala.io.Source
import scala.xml.Elem

object UpstreamJsonToXml {

  type AbsoluteLimitsType = Map[String, Int]

  def convert(jsonStream: InputStream): String = {
    val input: String = Source.fromInputStream(jsonStream).getLines() mkString ""

    val json = Json.parse(input)

    val ubermap = (json \ "limits" \ "absolute").validate[AbsoluteLimitsType]

    val wat = ubermap match {
      case s: JsSuccess[AbsoluteLimitsType] =>
        s.get
      case f: JsError =>
        throw new Exception(s"Unable to parse JSON structure from upstream: ${f}")
    }

    val xmlLimitsList: Iterable[Elem] = wat.map {
      case (key, value) => {
        //Convert this into an XML string thingy
          <limit name={key} value={value.toString()}/>
      }
    }

    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>" +
    <limits xmlns="http://docs.openstack.org/common/api/v1.0">
      <absolute>
        {xmlLimitsList}
      </absolute>
    </limits>.toString()
  }
}
