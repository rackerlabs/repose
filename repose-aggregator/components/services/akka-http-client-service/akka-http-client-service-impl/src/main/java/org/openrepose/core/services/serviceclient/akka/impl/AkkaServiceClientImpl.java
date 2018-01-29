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
package org.openrepose.core.services.serviceclient.akka.impl;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.routing.Broadcast;
import akka.routing.RoundRobinPool;
import akka.util.Timeout;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.openrepose.commons.config.manager.UpdateFailedException;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.utils.http.ServiceClient;
import org.openrepose.commons.utils.http.ServiceClientResponse;
import org.openrepose.core.service.httpclient.config.HttpConnectionPoolConfig;
import org.openrepose.core.service.httpclient.config.PoolType;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.httpclient.HttpClientService;
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient;
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClientException;
import org.slf4j.Logger;
import scala.concurrent.Await;
import scala.concurrent.Future;

import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;
import static akka.routing.ConsistentHashingRouter.ConsistentHashable;

public class AkkaServiceClientImpl implements AkkaServiceClient, UpdateListener<HttpConnectionPoolConfig> {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(AkkaServiceClientImpl.class);
    private static final long FUTURE_CACHE_TTL = 500;
    private static final TimeUnit FUTURE_CACHE_UNIT = TimeUnit.MILLISECONDS;
    private static final int CONNECTION_TIMEOUT_BUFFER_MILLIS = 1000;
    private static final String HTTP_CONN_POOL_CONFIG_NAME = "http-connection-pool.cfg.xml";

    private final String connectionPoolId;
    private final ServiceClient serviceClient;
    private final ConfigurationService configurationService;
    private final Cache<Object, Future> quickFutureCache;

    private boolean initialized = false;
    private int numberOfActors;
    private int socketTimeout;
    private ActorSystem actorSystem;
    private ActorRef tokenActorRef;

    public AkkaServiceClientImpl(String connectionPoolId,
                                 HttpClientService httpClientService,
                                 ConfigurationService configurationService) {
        this.connectionPoolId = connectionPoolId;
        this.serviceClient = new ServiceClient(connectionPoolId, httpClientService);
        this.configurationService = configurationService;

        Config customConf = ConfigFactory.load();
        Config baseConf = ConfigFactory.defaultReference();
        Config conf = customConf.withFallback(baseConf);
        actorSystem = ActorSystem.create("AuthClientActors", conf);

        configurationService.subscribeTo(HTTP_CONN_POOL_CONFIG_NAME, this, HttpConnectionPoolConfig.class);

        quickFutureCache = CacheBuilder.newBuilder()
                .expireAfterWrite(FUTURE_CACHE_TTL, FUTURE_CACHE_UNIT)
                .build();
    }

    @Override
    public void destroy() {
        configurationService.unsubscribeFrom(HTTP_CONN_POOL_CONFIG_NAME, this);
        actorSystem.shutdown();
    }

    @Override
    public ServiceClientResponse get(String hashKey, String uri, Map<String, String> headers) throws AkkaServiceClientException {
        return get(hashKey, uri, headers, true);
    }

    @Override
    public ServiceClientResponse get(String hashKey, String uri, Map<String, String> headers, boolean checkCache) throws AkkaServiceClientException {


        try {
            Timeout timeout = new Timeout(socketTimeout + CONNECTION_TIMEOUT_BUFFER_MILLIS, TimeUnit.MILLISECONDS);
            AuthGetRequest authGetRequest = new AuthGetRequest(hashKey, uri, headers);
            Future<ServiceClientResponse>  future = getFuture(authGetRequest, timeout, checkCache);
            return Await.result(future, timeout.duration());
        } catch (Exception e) {
            LOG.error("Error acquiring value from akka (GET) or the cache. Reason: {}", e.getLocalizedMessage());
            LOG.trace("", e);
            throw new AkkaServiceClientException("Error acquiring value from akka (GET) or the cache.", e);
        }
    }

    @Override
    public ServiceClientResponse post(String hashKey, String uri, Map<String, String> headers, String payload, MediaType contentMediaType) throws AkkaServiceClientException {
        return post(hashKey, uri, headers, payload, contentMediaType, true);
    }

    @Override
    public ServiceClientResponse post(String hashKey, String uri, Map<String, String> headers, String payload, MediaType contentMediaType, boolean checkCache) throws AkkaServiceClientException {
        try {
            Timeout timeout = new Timeout(socketTimeout + CONNECTION_TIMEOUT_BUFFER_MILLIS, TimeUnit.MILLISECONDS);
            AuthPostRequest authPostRequest = new AuthPostRequest(
                    hashKey, uri, headers, payload, contentMediaType);
            Future<ServiceClientResponse> future = getFuture(authPostRequest, timeout, checkCache);

            return Await.result(future, timeout.duration());
        } catch (Exception e) {
            LOG.error("Error acquiring value from akka (POST) or the cache. Reason: {}", e.getLocalizedMessage());
            LOG.trace("", e);
            throw new AkkaServiceClientException("Error acquiring value from akka (POST) or the cache.", e);
        }
    }

    private Future getFuture(ConsistentHashable hashableRequest, Timeout timeout, boolean checkCache) throws ExecutionException {
        Object hashKey = hashableRequest.consistentHashKey();
        LOG.trace("Getting future for: {}", hashKey);

        if (checkCache) {
            //http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/cache/Cache.html#get%28K,%20java.util.concurrent.Callable%29
            // Using this method, according to guava, is the right way to do the "cache pattern"
            return quickFutureCache.get(hashKey, () -> makeRequest(hashableRequest, timeout));
        } else {
            return makeRequest(hashableRequest, timeout);
        }
    }

    private Future makeRequest(ConsistentHashable hashableRequest, Timeout timeout) {
        LOG.trace("Call for: {}", hashableRequest.consistentHashKey());
        return ask(tokenActorRef, hashableRequest, timeout);
    }

    @Override
    public void configurationUpdated(HttpConnectionPoolConfig configurationObject) throws UpdateFailedException {
        PoolType defaultPool = configurationObject.getPool().get(0);

        boolean isPoolConfigured = false;
        for (PoolType pool : configurationObject.getPool()) {
            if (pool.isDefault()) {
                defaultPool = pool;
            }

            if (pool.getId().equals(connectionPoolId)) {
                numberOfActors = pool.getHttpConnManagerMaxTotal();
                socketTimeout = pool.getHttpSocketTimeout();
                isPoolConfigured = true;
                break;
            }
        }

        if (!isPoolConfigured) {
            LOG.warn("Pool " + connectionPoolId + " not available -- using the default pool settings");
            numberOfActors = defaultPool.getHttpConnManagerMaxTotal();
            socketTimeout = defaultPool.getHttpSocketTimeout();
        }

        // Kill all current actors once they are done processing, allowing the router to die
        if (tokenActorRef != null) {
            tokenActorRef.tell(new Broadcast(PoisonPill.getInstance()), ActorRef.noSender());
        }

        // Create a new router actor
        tokenActorRef = actorSystem.actorOf(Props.create(AuthTokenFutureActor.class, serviceClient)
                .withRouter(new RoundRobinPool(numberOfActors)), "authRequestRouter");

        initialized = true;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }
}
