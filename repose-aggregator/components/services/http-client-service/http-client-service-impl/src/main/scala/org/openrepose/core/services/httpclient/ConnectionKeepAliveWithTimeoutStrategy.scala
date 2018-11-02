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
package org.openrepose.core.services.httpclient

import org.apache.http.HttpResponse
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy
import org.apache.http.protocol.HttpContext

/**
  * Custom keep alive strategy that defaults to the non-standard Keep-Alive header to communicate to the client
  * the period of time in seconds they intend to keep the connection alive on the server side.
  * If this header is present in the response, the value in this header will be used to determine the maximum
  * length of time to keep a persistent connection open for.
  * If the Keep-Alive header is NOT present in the response, the value of keepalive.timeout is
  * evaluated.
  * If this value is 0, the connection will be kept alive indefinitely.
  * If the value is greater than 0, the connection will be kept alive for the number of milliseconds specified.
  */
class ConnectionKeepAliveWithTimeoutStrategy(timeout: Int) extends DefaultConnectionKeepAliveStrategy {
  override def getKeepAliveDuration(response: HttpResponse, context: HttpContext): Long = {
    val duration = super.getKeepAliveDuration(response, context)

    if (duration > 0) {
      duration
    } else {
      timeout
    }
  }
}
