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

import java.io.IOException
import java.nio.file.Paths
import java.security.GeneralSecurityException
import java.util.concurrent.TimeUnit

import com.codahale.metrics.httpclient._
import com.typesafe.scalalogging.slf4j.StrictLogging
import io.opentracing.Tracer
import javax.inject.{Inject, Named}
import javax.net.ssl.SSLContext
import org.apache.http.client.config.RequestConfig
import org.apache.http.config.{ConnectionConfig, MessageConstraints, RegistryBuilder, SocketConfig}
import org.apache.http.conn.socket.{ConnectionSocketFactory, PlainConnectionSocketFactory}
import org.apache.http.conn.ssl.{NoopHostnameVerifier, SSLConnectionSocketFactory, TrustAllStrategy}
import org.apache.http.impl.client.{CloseableHttpClient, HttpClientBuilder, HttpClients}
import org.apache.http.impl.conn.SystemDefaultDnsResolver
import org.apache.http.message.BasicHeader
import org.apache.http.ssl.{SSLContextBuilder, SSLContexts}
import org.openrepose.commons.utils.opentracing.httpclient.{ReposeTracingRequestInterceptor, ReposeTracingResponseInterceptor}
import org.openrepose.core.services.httpclient.config.PoolConfig
import org.openrepose.core.services.reporting.metrics.MetricsService
import org.openrepose.core.services.uriredaction.UriRedactionService
import org.openrepose.core.spring.ReposeSpringProperties
import org.springframework.beans.factory.annotation.Value

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.util.Try

/**
  * Creates [[CloseableHttpClient]]s from configuration.
  */
@Named
class HttpClientProvider @Inject()(@Value(ReposeSpringProperties.CORE.CONFIG_ROOT) configRoot: String,
                                   @Value(ReposeSpringProperties.CORE.REPOSE_VERSION) reposeVersion: String,
                                   tracer: Tracer,
                                   uriRedactionService: UriRedactionService,
                                   metricsService: MetricsService)
  extends StrictLogging {

  def createClient(clientConfig: PoolConfig): CloseableHttpClient = {
    logger.info("HTTP client {} is being created", clientConfig.getId)

    val socketConfig = getSocketConfigBuilder
      .setSoTimeout(clientConfig.getHttpSocketTimeout)
      .setTcpNoDelay(clientConfig.isHttpTcpNodelay)
      .build()

    val messageConstraints = getMessageConstraintsBuilder
      .setMaxHeaderCount(clientConfig.getHttpConnectionMaxHeaderCount)
      .setMaxLineLength(clientConfig.getHttpConnectionMaxLineLength)
      .build()

    val connectionConfig = getConnectionConfigBuilder
      .setBufferSize(clientConfig.getHttpSocketBufferSize)
      .setMessageConstraints(messageConstraints)
      .build()

    val sslContext =
      Option(clientConfig.getKeystoreFilename).flatMap { _ =>
        Try {
          generateSslContext(
            clientConfig.getKeystoreFilename,
            clientConfig.getKeystorePassword,
            clientConfig.getKeyPassword,
            clientConfig.getTruststoreFilename,
            clientConfig.getTruststorePassword)
        }.recover {
          case e@(_: GeneralSecurityException | _: IOException) =>
            logger.warn("Failed to properly configure the SSL client for {} due to: {}", clientConfig.getId, e.getLocalizedMessage)
            logger.trace("", e)
            logger.info("Failing over to basic trusting SSL context")
            throw e
        }.toOption
      }.getOrElse {
        getSslContextBuilder
          .loadTrustMaterial(TrustAllStrategy.INSTANCE)
          .build()
      }

    // Content compression is disabled for backwards compatibility.
    // Enabling it means that an Accept-Encoding header is always sent on the request (allowing for compression).
    val requestConfig = getRequestConfigBuilder
      .setConnectTimeout(clientConfig.getHttpConnectionTimeout)
      .setContentCompressionEnabled(false)
      .build()

    val headers = Option(clientConfig.getHeaders)
      .map(_.getHeader
        .asScala
        .map(header => new BasicHeader(header.getName, header.getValue))
        .asJava)

    val sslConnectionSocketFactory = new SSLConnectionSocketFactory(
      sslContext,
      null,
      null,
      NoopHostnameVerifier.INSTANCE)

    val connectionSocketFactoryRegistry = RegistryBuilder.create[ConnectionSocketFactory]()
      .register("http", PlainConnectionSocketFactory.getSocketFactory)
      .register("https", sslConnectionSocketFactory)
      .build()

    val connectionManager = new InstrumentedHttpClientConnectionManager(
      metricsService.getRegistry,
      connectionSocketFactoryRegistry,
      null,
      null,
      SystemDefaultDnsResolver.INSTANCE,
      -1,
      TimeUnit.MILLISECONDS,
      clientConfig.getId)
    connectionManager.setDefaultMaxPerRoute(clientConfig.getHttpConnManagerMaxPerRoute)
    connectionManager.setMaxTotal(clientConfig.getHttpConnManagerMaxTotal)
    connectionManager.setDefaultSocketConfig(socketConfig)
    connectionManager.setDefaultConnectionConfig(connectionConfig)

    // Create and configure the HTTP client
    // Interceptors provide OpenTracing support
    // Note that although we always register these interceptors, the provided Tracer may be a NoopTracer,
    // making nearly all of the work done by these interceptors a no-op
    val clientBuilder = getHttpClientBuilder
      .setRequestExecutor(new InstrumentedHttpRequestExecutor(metricsService.getRegistry, HttpClientMetricNameStrategies.METHOD_ONLY))
      .setConnectionManager(connectionManager)
      .setKeepAliveStrategy(new ConnectionKeepAliveWithTimeoutStrategy(clientConfig.getKeepaliveTimeout))
      .disableCookieManagement()
      .addInterceptorLast(new ReposeTracingRequestInterceptor(tracer, reposeVersion, uriRedactionService))
      .addInterceptorLast(new ReposeTracingResponseInterceptor())
      .disableRedirectHandling()
      .setDefaultRequestConfig(requestConfig)
    headers.foreach(clientBuilder.setDefaultHeaders)

    // Add caching to the raw client and return
    getCachingHttpClient(clientBuilder.build(), clientConfig.getCacheTtl.milliseconds)
  }

  private def generateSslContext(keystoreFilename: String,
                                 keystorePassword: String,
                                 keyPassword: String,
                                 truststoreFilename: String,
                                 truststorePassword: String): SSLContext = {
    val keystoreFile = Paths.get(configRoot)
      .resolve(keystoreFilename)
      .toFile

    val keystorePass = Option(keystorePassword)
      .map(_.toCharArray)
      .orNull

    val keyPass = Option(keyPassword)
      .map(_.toCharArray)
      .orNull

    val (truststoreFile, truststorePass) = Option(truststoreFilename) match {
      case Some(filename) =>
        val tsFile = Paths.get(configRoot)
          .resolve(filename)
          .toFile

        val tsPass = Option(truststorePassword)
          .map(_.toCharArray)
          .orNull

        (tsFile, tsPass)
      case None =>
        (keystoreFile, keystorePass)
    }

    getSslContextBuilder
      .loadKeyMaterial(keystoreFile, keystorePass, keyPass)
      .loadTrustMaterial(truststoreFile, truststorePass)
      .build()
  }

  // Builder access methods
  def getSocketConfigBuilder: SocketConfig.Builder = {
    SocketConfig.custom()
  }

  def getMessageConstraintsBuilder: MessageConstraints.Builder = {
    MessageConstraints.custom()
  }

  def getConnectionConfigBuilder: ConnectionConfig.Builder = {
    ConnectionConfig.custom()
  }

  def getSslContextBuilder: SSLContextBuilder = {
    SSLContexts.custom()
  }

  def getRequestConfigBuilder: RequestConfig.Builder = {
    RequestConfig.custom()
  }

  def getHttpClientBuilder: HttpClientBuilder = {
    HttpClients.custom()
  }

  def getCachingHttpClient(closeableHttpClient: CloseableHttpClient, cacheDuration: Duration): CachingHttpClient = {
    new CachingHttpClient(closeableHttpClient, cacheDuration)
  }
}
