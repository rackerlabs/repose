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
package org.openrepose.commons.utils.json

import org.openrepose.commons.utils.string.Base64Helper
import play.api.libs.json._

object JsonHeaderHelper {

  /**
    * Converts an object to a base 64 encoded JSON [[String]].
    *
    * @param any any object
    * @param tjs a [[Writes]] converter
    * @tparam T the type of the object
    * @return a base 64 encoded JSON [[String]]
    */
  def anyToJsonHeader[T](any: T)(implicit tjs: Writes[T]): String = {
    Base64Helper.base64EncodeUtf8(Json.stringify(Json.toJson(any)(tjs)))
  }

  /**
    * Converts a [[String]] to a [[JsValue]].
    *
    * @param header the header value containing a base 64 encoded JSON string.
    * @return a [[JsValue]] representation of the JSON from the header
    */
  def jsonHeaderToValue(header: String): JsValue = {
    Json.parse(Base64Helper.base64DecodeUtf8(header))
  }
}
