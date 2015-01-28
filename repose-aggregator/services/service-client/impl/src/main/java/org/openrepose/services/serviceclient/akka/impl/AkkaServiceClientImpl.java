package org.openrepose.services.serviceclient.akka.impl;


import akka.actor.*;
import akka.routing.RoundRobinRouter;
import akka.util.Timeout;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.openrepose.commons.utils.http.ServiceClient;
import org.openrepose.commons.utils.http.ServiceClientResponse;
import org.openrepose.services.httpclient.HttpClientService;
import org.openrepose.services.serviceclient.akka.AkkaServiceClientException;
import org.openrepose.services.serviceclient.akka.AkkaServiceClient;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final ServiceClient serviceClient;
    private ActorSystem actorSystem;
    private ActorRef tokenActorRef;
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(AkkaServiceClientImpl.class);
    private final Cache<Object, Future> quickFutureCache;

    private static final long FUTURE_CACHE_TTL = 500;
    private static final TimeUnit FUTURE_CACHE_UNIT = TimeUnit.MILLISECONDS;
    private static final int CONNECTION_TIMEOUT_BUFFER_MILLIS = 1000;

    @Autowired
    public AkkaServiceClientImpl(HttpClientService httpClientService) {
        this.serviceClient = getServiceClient(httpClientService);
        final int numberOfActors = serviceClient.getPoolSize();

        Config customConf = ConfigFactory.load();
        Config baseConf = ConfigFactory.defaultReference();
        Config conf = customConf.withFallback(baseConf);
        actorSystem = ActorSystem.create("AuthClientActors", conf);
        quickFutureCache = CacheBuilder.newBuilder()
                .expireAfterWrite(FUTURE_CACHE_TTL, FUTURE_CACHE_UNIT)
                .build();

        tokenActorRef = actorSystem.actorOf(new Props(new UntypedActorFactory() {
            public UntypedActor create() {
                return new AuthTokenFutureActor(serviceClient);
            }
        }).withRouter(new RoundRobinRouter(numberOfActors)), "authRequestRouter");
    }


    @Override
    public ServiceClientResponse get(String hashKey, String uri, Map<String, String> headers) throws AkkaServiceClientException {
        ServiceClientResponse serviceClientResponse = null;
        AuthGetRequest authGetRequest = new AuthGetRequest(hashKey, uri, headers);
        try {
            Timeout timeout = new Timeout(serviceClient.getSocketTimeout() + CONNECTION_TIMEOUT_BUFFER_MILLIS, TimeUnit.MILLISECONDS);
            Future<ServiceClientResponse> future = getFuture(authGetRequest, timeout);
            serviceClientResponse = Await.result(future, timeout.duration());
        } catch (Exception e) {
            LOG.error("Error acquiring value from akka (GET) or the cache. Reason: {}", e.getLocalizedMessage());
            LOG.trace("", e);
            throw new AkkaServiceClientException("Error acquiring value from akka (GET) or the cache.", e);
        }
        return serviceClientResponse;
    }

    @Override
    public ServiceClientResponse post(String hashKey, String uri, Map<String, String> headers, String payload, MediaType contentMediaType) throws AkkaServiceClientException {
        ServiceClientResponse serviceClientResponse = null;
        AuthPostRequest authPostRequest = new AuthPostRequest(hashKey, uri, headers, payload, contentMediaType);
        try {
            Timeout timeout = new Timeout(serviceClient.getSocketTimeout() + CONNECTION_TIMEOUT_BUFFER_MILLIS, TimeUnit.MILLISECONDS);
            Future<ServiceClientResponse> future = getFuture(authPostRequest, timeout);
            serviceClientResponse = Await.result(future, timeout.duration());
        } catch (Exception e) {
            LOG.error("Error acquiring value from akka (POST) or the cache. Reason: {}", e.getLocalizedMessage());
            LOG.trace("", e);
            throw new AkkaServiceClientException("Error acquiring value from akka (POST) or the cache.", e);
        }
        return serviceClientResponse;
    }

    @Override
    public void shutdown() {
        actorSystem.shutdown();
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

    public ServiceClient getServiceClient(HttpClientService httpClientService) {
        return new ServiceClient(null, httpClientService);
    }
}
