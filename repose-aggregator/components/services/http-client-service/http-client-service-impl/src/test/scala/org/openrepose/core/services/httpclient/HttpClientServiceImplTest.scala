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

import java.net.URL
import java.util.UUID

import org.apache.http.HttpHost
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.protocol.HttpContext
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{any, eq => isEq, _}
import org.mockito.Mockito.{verify, when}
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.healthcheck.{HealthCheckService, HealthCheckServiceProxy, Severity}
import org.openrepose.core.services.httpclient
import org.openrepose.core.services.httpclient.config.{HttpConnectionPoolsConfig, PoolConfig}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class HttpClientServiceImplTest extends FunSpec with BeforeAndAfterEach with MockitoSugar with Matchers {

  var configService: ConfigurationService = _
  var healthCheckService: HealthCheckService = _
  var healthCheckServiceProxy: HealthCheckServiceProxy = _
  var httpClientProvider: httpclient.HttpClientProvider = _
  var httpClientDecommissioner: HttpClientDecommissioner = _
  var httpClientService: HttpClientServiceImpl = _

  override def beforeEach(): Unit = {
    configService = mock[ConfigurationService]
    healthCheckService = mock[HealthCheckService]
    healthCheckServiceProxy = mock[HealthCheckServiceProxy]
    httpClientProvider = mock[httpclient.HttpClientProvider]
    httpClientDecommissioner = mock[HttpClientDecommissioner]

    when(healthCheckService.register()).thenReturn(healthCheckServiceProxy)

    httpClientService = new HttpClientServiceImpl(configService, healthCheckService, httpClientProvider, httpClientDecommissioner)
  }

  describe("init") {
    it("should register the configuration listener") {
      httpClientService.init()

      verify(configService).subscribeTo(
        isEq(HttpClientServiceImpl.DefaultConfig),
        any[URL](),
        isA(classOf[UpdateListener[HttpConnectionPoolsConfig]]),
        isA(classOf[Class[HttpConnectionPoolsConfig]])
      )
    }

    it("should report a configuration issue to the health check service") {
      httpClientService.init()

      verify(healthCheckService).register()
      verify(healthCheckServiceProxy).reportIssue(any[String], any[String], isEq(Severity.BROKEN))
    }
  }

  describe("destroy") {
    it("should unregister the configuration listener") {
      httpClientService.destroy()

      verify(configService).unsubscribeFrom(isEq(HttpClientServiceImpl.DefaultConfig), isA(classOf[UpdateListener[_]]))
    }
  }

  describe("getDefaultClient") {
    it("should throw an exception if the service has not yet initialized") {
      an[IllegalStateException] should be thrownBy httpClientService.getDefaultClient()
    }

    it("should return the default client") {
      httpClientService.init()

      val configurationListenerCaptor = ArgumentCaptor.forClass(classOf[UpdateListener[HttpConnectionPoolsConfig]])
      verify(configService).subscribeTo(
        anyString(),
        any[URL],
        configurationListenerCaptor.capture(),
        isEq(classOf[HttpConnectionPoolsConfig]))

      val poolsConfig = new HttpConnectionPoolsConfig()
      val defaultPoolConfig = minimalPoolConfig(_.setDefault(true))
      val otherPoolConfig = minimalPoolConfig(_.setDefault(false))
      Seq(
        defaultPoolConfig,
        otherPoolConfig
      ).foreach(poolsConfig.getPool.add)

      val defaultClient = mock[CloseableHttpClient]
      val otherClient = mock[CloseableHttpClient]
      when(httpClientProvider.createClient(defaultPoolConfig)).thenReturn(defaultClient)
      when(httpClientProvider.createClient(otherPoolConfig)).thenReturn(otherClient)

      val configurationListener = configurationListenerCaptor.getValue
      configurationListener.configurationUpdated(poolsConfig)

      val client = httpClientService.getDefaultClient()
      val request = RequestBuilder.get().build()
      client.execute(request)

      verify(defaultClient).execute(any[HttpHost], same(request), any[HttpContext])
    }
  }

  describe("getClient") {
    it("should throw an exception if the service has not yet initialized") {
      an[IllegalStateException] should be thrownBy httpClientService.getClient(null)
    }

    it("should return the default client if passed null") {
      httpClientService.init()

      val configurationListenerCaptor = ArgumentCaptor.forClass(classOf[UpdateListener[HttpConnectionPoolsConfig]])
      verify(configService).subscribeTo(
        anyString(),
        any[URL],
        configurationListenerCaptor.capture(),
        isEq(classOf[HttpConnectionPoolsConfig]))

      val poolsConfig = new HttpConnectionPoolsConfig()
      val defaultPoolConfig = minimalPoolConfig(_.setDefault(true))
      val otherPoolConfig = minimalPoolConfig(_.setDefault(false))
      Seq(
        defaultPoolConfig,
        otherPoolConfig
      ).foreach(poolsConfig.getPool.add)

      val defaultClient = mock[CloseableHttpClient]
      val otherClient = mock[CloseableHttpClient]
      when(httpClientProvider.createClient(defaultPoolConfig)).thenReturn(defaultClient)
      when(httpClientProvider.createClient(otherPoolConfig)).thenReturn(otherClient)

      val configurationListener = configurationListenerCaptor.getValue
      configurationListener.configurationUpdated(poolsConfig)

      val client = httpClientService.getClient(null)
      val request = RequestBuilder.get().build()
      client.execute(request)

      verify(defaultClient).execute(any[HttpHost], same(request), any[HttpContext])
    }

    it("should return the default client if passed an id for a client that does not exist") {
      httpClientService.init()

      val configurationListenerCaptor = ArgumentCaptor.forClass(classOf[UpdateListener[HttpConnectionPoolsConfig]])
      verify(configService).subscribeTo(
        anyString(),
        any[URL],
        configurationListenerCaptor.capture(),
        isEq(classOf[HttpConnectionPoolsConfig]))

      val poolsConfig = new HttpConnectionPoolsConfig()
      val defaultPoolConfig = minimalPoolConfig(_.setDefault(true))
      val otherPoolConfig = minimalPoolConfig(_.setDefault(false))
      Seq(
        defaultPoolConfig,
        otherPoolConfig
      ).foreach(poolsConfig.getPool.add)

      val defaultClient = mock[CloseableHttpClient]
      val otherClient = mock[CloseableHttpClient]
      when(httpClientProvider.createClient(defaultPoolConfig)).thenReturn(defaultClient)
      when(httpClientProvider.createClient(otherPoolConfig)).thenReturn(otherClient)

      val configurationListener = configurationListenerCaptor.getValue
      configurationListener.configurationUpdated(poolsConfig)

      val client = httpClientService.getClient("not-a-client-id")
      val request = RequestBuilder.get().build()
      client.execute(request)

      verify(defaultClient).execute(any[HttpHost], same(request), any[HttpContext])
    }

    it("should return the client identified by the id") {
      httpClientService.init()

      val configurationListenerCaptor = ArgumentCaptor.forClass(classOf[UpdateListener[HttpConnectionPoolsConfig]])
      verify(configService).subscribeTo(
        anyString(),
        any[URL],
        configurationListenerCaptor.capture(),
        isEq(classOf[HttpConnectionPoolsConfig]))

      val poolsConfig = new HttpConnectionPoolsConfig()
      val defaultPoolConfig = minimalPoolConfig(_.setDefault(true))
      val otherPoolConfig = minimalPoolConfig(_.setDefault(false))
      Seq(
        defaultPoolConfig,
        otherPoolConfig
      ).foreach(poolsConfig.getPool.add)

      val defaultClient = mock[CloseableHttpClient]
      val otherClient = mock[CloseableHttpClient]
      when(httpClientProvider.createClient(defaultPoolConfig)).thenReturn(defaultClient)
      when(httpClientProvider.createClient(otherPoolConfig)).thenReturn(otherClient)

      val configurationListener = configurationListenerCaptor.getValue
      configurationListener.configurationUpdated(poolsConfig)

      val client = httpClientService.getClient(otherPoolConfig.getId)
      val request = RequestBuilder.get().build()
      client.execute(request)

      verify(otherClient).execute(any[HttpHost], same(request), any[HttpContext])
    }
  }

  describe("getInternalClient") {
    it("should throw an exception if the service has not yet initialized") {
      an[IllegalStateException] should be thrownBy httpClientService.getInternalClient(null)
    }

    it("should return the default client if passed null") {
      httpClientService.init()

      val configurationListenerCaptor = ArgumentCaptor.forClass(classOf[UpdateListener[HttpConnectionPoolsConfig]])
      verify(configService).subscribeTo(
        anyString(),
        any[URL],
        configurationListenerCaptor.capture(),
        isEq(classOf[HttpConnectionPoolsConfig]))

      val poolsConfig = new HttpConnectionPoolsConfig()
      val defaultPoolConfig = minimalPoolConfig(_.setDefault(true))
      val otherPoolConfig = minimalPoolConfig(_.setDefault(false))
      Seq(
        defaultPoolConfig,
        otherPoolConfig
      ).foreach(poolsConfig.getPool.add)

      val defaultClient = mock[CloseableHttpClient]
      val otherClient = mock[CloseableHttpClient]
      when(httpClientProvider.createClient(defaultPoolConfig)).thenReturn(defaultClient)
      when(httpClientProvider.createClient(otherPoolConfig)).thenReturn(otherClient)

      val configurationListener = configurationListenerCaptor.getValue
      configurationListener.configurationUpdated(poolsConfig)

      val client = httpClientService.getInternalClient(null)
      val request = RequestBuilder.get().build()
      client.getClient.execute(request)

      verify(defaultClient).execute(request)
    }

    it("should return the default client if passed an id for a client that does not exist") {
      httpClientService.init()

      val configurationListenerCaptor = ArgumentCaptor.forClass(classOf[UpdateListener[HttpConnectionPoolsConfig]])
      verify(configService).subscribeTo(
        anyString(),
        any[URL],
        configurationListenerCaptor.capture(),
        isEq(classOf[HttpConnectionPoolsConfig]))

      val poolsConfig = new HttpConnectionPoolsConfig()
      val defaultPoolConfig = minimalPoolConfig(_.setDefault(true))
      val otherPoolConfig = minimalPoolConfig(_.setDefault(false))
      Seq(
        defaultPoolConfig,
        otherPoolConfig
      ).foreach(poolsConfig.getPool.add)

      val defaultClient = mock[CloseableHttpClient]
      val otherClient = mock[CloseableHttpClient]
      when(httpClientProvider.createClient(defaultPoolConfig)).thenReturn(defaultClient)
      when(httpClientProvider.createClient(otherPoolConfig)).thenReturn(otherClient)

      val configurationListener = configurationListenerCaptor.getValue
      configurationListener.configurationUpdated(poolsConfig)

      val client = httpClientService.getInternalClient("not-a-client-id")
      val request = RequestBuilder.get().build()
      client.getClient.execute(request)

      verify(defaultClient).execute(request)
    }

    it("should return the client identified by the id") {
      httpClientService.init()

      val configurationListenerCaptor = ArgumentCaptor.forClass(classOf[UpdateListener[HttpConnectionPoolsConfig]])
      verify(configService).subscribeTo(
        anyString(),
        any[URL],
        configurationListenerCaptor.capture(),
        isEq(classOf[HttpConnectionPoolsConfig]))

      val poolsConfig = new HttpConnectionPoolsConfig()
      val defaultPoolConfig = minimalPoolConfig(_.setDefault(true))
      val otherPoolConfig = minimalPoolConfig(_.setDefault(false))
      Seq(
        defaultPoolConfig,
        otherPoolConfig
      ).foreach(poolsConfig.getPool.add)

      val defaultClient = mock[CloseableHttpClient]
      val otherClient = mock[CloseableHttpClient]
      when(httpClientProvider.createClient(defaultPoolConfig)).thenReturn(defaultClient)
      when(httpClientProvider.createClient(otherPoolConfig)).thenReturn(otherClient)

      val configurationListener = configurationListenerCaptor.getValue
      configurationListener.configurationUpdated(poolsConfig)

      val client = httpClientService.getInternalClient(otherPoolConfig.getId)
      val request = RequestBuilder.get().build()
      client.getClient.execute(request)

      verify(otherClient).execute(request)
    }
  }

  def minimalPoolConfig(configurations: PoolConfig => Unit*): PoolConfig = {
    fluentPoolConfig(
      configurations.+:((_: PoolConfig).setId(UUID.randomUUID.toString)): _*)
  }

  def fluentPoolConfig(configurations: PoolConfig => Unit*): PoolConfig = {
    val poolConfig = new PoolConfig()
    configurations.foreach(_.apply(poolConfig))
    poolConfig
  }
}
