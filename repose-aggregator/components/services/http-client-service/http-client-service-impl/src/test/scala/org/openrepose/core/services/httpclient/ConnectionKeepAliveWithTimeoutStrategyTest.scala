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

import org.apache.http.message.BasicHttpResponse
import org.apache.http.protocol.HTTP.CONN_KEEP_ALIVE
import org.apache.http.{HttpStatus, HttpVersion}
import org.junit.runner.RunWith
import org.openrepose.core.services.httpclient
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class ConnectionKeepAliveWithTimeoutStrategyTest extends FunSpec with Matchers {
  describe("getKeepAliveDuration") {
    it("should return the configured timeout of zero if no header timeout is present") {
      val configuredTimeout = 0

      val connectionKeepAliveWithTimeoutStrategy = new httpclient.ConnectionKeepAliveWithTimeoutStrategy(configuredTimeout)

      val response = new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK")

      val keepAlive = connectionKeepAliveWithTimeoutStrategy.getKeepAliveDuration(response, null)

      keepAlive shouldBe configuredTimeout
    }

    it("should return the configured timeout if no header timeout is present") {
      val configuredTimeout = 10

      val connectionKeepAliveWithTimeoutStrategy = new httpclient.ConnectionKeepAliveWithTimeoutStrategy(configuredTimeout)

      val response = new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK")

      val keepAlive = connectionKeepAliveWithTimeoutStrategy.getKeepAliveDuration(response, null)

      keepAlive shouldBe configuredTimeout
    }

    it("should return the configured timeout if the header timeout is zero") {
      val configuredTimeout = 10
      val headerTimeout = 0

      val connectionKeepAliveWithTimeoutStrategy = new httpclient.ConnectionKeepAliveWithTimeoutStrategy(configuredTimeout)

      val response = new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK")
      response.setHeader(CONN_KEEP_ALIVE, "timeout=" + String.valueOf(headerTimeout))

      val keepAlive = connectionKeepAliveWithTimeoutStrategy.getKeepAliveDuration(response, null)

      keepAlive shouldBe configuredTimeout
    }

    it("should return the header timeout if it is not zero") {
      val configuredTimeout = 10
      val headerTimeout = 9

      val connectionKeepAliveWithTimeoutStrategy = new httpclient.ConnectionKeepAliveWithTimeoutStrategy(configuredTimeout)

      val response = new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK")
      response.setHeader(CONN_KEEP_ALIVE, "timeout=" + String.valueOf(headerTimeout))

      val keepAlive = connectionKeepAliveWithTimeoutStrategy.getKeepAliveDuration(response, null)

      keepAlive shouldBe headerTimeout.seconds.toMillis
    }
  }
}
