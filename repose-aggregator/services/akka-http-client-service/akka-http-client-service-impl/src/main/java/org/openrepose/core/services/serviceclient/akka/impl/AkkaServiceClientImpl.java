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
import akka.actor.Props;
import akka.routing.RoundRobinRouter;
import akka.util.Timeout;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.openrepose.commons.utils.http.ServiceClient;
import org.openrepose.commons.utils.http.ServiceClientResponse;
import org.openrepose.core.services.httpclient.HttpClientService;
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient;
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClientException;
import org.slf4j.Logger;
import scala.concurrent.Await;
import scala.concurrent.Future;

import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;
import static akka.routing.ConsistentHashingRouter.ConsistentHashable;

public class AkkaServiceClientImpl implements AkkaServiceClient {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(AkkaServiceClientImpl.class);
    private static final long FUTURE_CACHE_TTL = 500;
    private static final TimeUnit FUTURE_CACHE_UNIT = TimeUnit.MILLISECONDS;
    private static final int CONNECTION_TIMEOUT_BUFFER_MILLIS = 1000;
    private final ServiceClient serviceClient;
    private final Cache<Object, Future> quickFutureCache;
    private ActorSystem actorSystem;
    private ActorRef tokenActorRef;

    public AkkaServiceClientImpl(String connectionPoolId, HttpClientService httpClientService) {
        this.serviceClient = new ServiceClient(connectionPoolId, httpClientService);
        final int numberOfActors = serviceClient.getPoolSize();

        Config customConf = ConfigFactory.load();
        Config baseConf = ConfigFactory.defaultReference();
        Config conf = customConf.withFallback(baseConf);
        actorSystem = ActorSystem.create("AuthClientActors", conf);
        quickFutureCache = CacheBuilder.newBuilder()
                .expireAfterWrite(FUTURE_CACHE_TTL, FUTURE_CACHE_UNIT)
                .build();

        tokenActorRef = actorSystem.actorOf(Props.create(AuthTokenFutureActor.class, serviceClient)
                .withRouter(new RoundRobinRouter(numberOfActors)), "authRequestRouter");
    }


    public void destroy() {
        actorSystem.shutdown();
    }

    @Override
    public ServiceClientResponse get(String hashKey, String uri, Map<String, String> headers) throws AkkaServiceClientException {
        AuthGetRequest authGetRequest = new AuthGetRequest(hashKey, uri, headers);
        try {
            Timeout timeout = new Timeout(serviceClient.getSocketTimeout() + CONNECTION_TIMEOUT_BUFFER_MILLIS, TimeUnit.MILLISECONDS);
            Future<ServiceClientResponse> future = getFuture(authGetRequest, timeout);
            return Await.result(future, timeout.duration());
        } catch (Exception e) {
            LOG.error("Error acquiring value from akka (GET) or the cache. Reason: {}", e.getLocalizedMessage());
            LOG.trace("", e);
            throw new AkkaServiceClientException("Error acquiring value from akka (GET) or the cache.", e);
        }
    }

    @Override
    public ServiceClientResponse post(String hashKey, String uri, Map<String, String> headers, String payload, MediaType contentMediaType) throws AkkaServiceClientException {
        AuthPostRequest authPostRequest = new AuthPostRequest(hashKey, uri, headers, payload, contentMediaType);
        try {
            Timeout timeout = new Timeout(serviceClient.getSocketTimeout() + CONNECTION_TIMEOUT_BUFFER_MILLIS, TimeUnit.MILLISECONDS);
            Future<ServiceClientResponse> future = getFuture(authPostRequest, timeout);
            return Await.result(future, timeout.duration());
        } catch (Exception e) {
            LOG.error("Error acquiring value from akka (POST) or the cache. Reason: {}", e.getLocalizedMessage());
            LOG.trace("", e);
            throw new AkkaServiceClientException("Error acquiring value from akka (POST) or the cache.", e);
        }
    }

    private Future getFuture(final ConsistentHashable hashableRequest, final Timeout timeout) throws ExecutionException {
        Object hashKey = hashableRequest.consistentHashKey();

        //http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/cache/Cache.html#get%28K,%20java.util.concurrent.Callable%29
        // Using this method, according to guava, is the right way to do the "cache pattern"
        return quickFutureCache.get(hashKey, new Callable<Future<Object>>() {

            @Override
            public Future<Object> call() throws Exception {

                return ask(tokenActorRef, hashableRequest, timeout);
            }
        });
    }
}
