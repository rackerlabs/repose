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
import org.openrepose.services.serviceclient.akka.AkkaServiceClient;
import org.slf4j.Logger;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;
import static akka.routing.ConsistentHashingRouter.ConsistentHashable;

@Named
public class AkkaServiceClientImpl implements AkkaServiceClient {

    private final ServiceClient serviceClient;
    private ActorSystem actorSystem;
    private ActorRef tokenActorRef;
    private int numberOfActors;
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(AkkaServiceClientImpl.class);
    final Timeout t = new Timeout(50, TimeUnit.SECONDS);
    private final Cache<Object, Future> quickFutureCache;

    private static final long FUTURE_CACHE_TTL = 500;

    @Inject
    public AkkaServiceClientImpl(HttpClientService httpClientService) {
        this.serviceClient = getServiceClient(httpClientService);
        numberOfActors = serviceClient.getPoolSize();

        Config customConf = ConfigFactory.load();
        Config baseConf = ConfigFactory.defaultReference();
        Config conf = customConf.withFallback(baseConf);
        actorSystem = ActorSystem.create("AuthClientActors", conf);
        quickFutureCache = CacheBuilder.newBuilder()
                .expireAfterWrite(FUTURE_CACHE_TTL, TimeUnit.MILLISECONDS)
                .build();

        tokenActorRef = actorSystem.actorOf(new Props(new UntypedActorFactory() {
            public UntypedActor create() {
                return new AuthTokenFutureActor(serviceClient);
            }
        }).withRouter(new RoundRobinRouter(numberOfActors)), "authRequestRouter");
    }


    @PreDestroy
    public void destroy() {
        //Tie this actor system to our bean lifecycle
        actorSystem.shutdown();
    }

    @Override
    public ServiceClientResponse get(String key, String uri, Map<String, String> headers) {

        ServiceClientResponse reusableServiceserviceClientResponse = null;
        AuthGetRequest authGetRequest = new AuthGetRequest(key, uri, headers);
        try {
            Future<ServiceClientResponse> future = getFuture(authGetRequest);
            reusableServiceserviceClientResponse = Await.result(future, Duration.create(50, TimeUnit.SECONDS));
        } catch (Exception e) {
            LOG.error("Error acquiring value from akka (GET) or the cache", e);
        }
        return reusableServiceserviceClientResponse;
    }

    @Override
    public ServiceClientResponse post(String requestKey, String uri, Map<String, String> headers, String payload, MediaType contentMediaType) {
        ServiceClientResponse scr = null;
        AuthPostRequest apr = new AuthPostRequest(requestKey, uri, headers, payload, contentMediaType);
        try {
            Future<ServiceClientResponse> future = getFuture(apr);
            scr = Await.result(future, Duration.create(50, TimeUnit.SECONDS));
        } catch (Exception e) {
            LOG.error("Error acquiring value from akka (POST) or the cache", e);
        }
        return scr;
    }


    private Future getFuture(final ConsistentHashable hashableRequest) throws ExecutionException {
        Object hashKey = hashableRequest.consistentHashKey();

        //http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/cache/Cache.html#get%28K,%20java.util.concurrent.Callable%29
        // Using this method, according to guava, is the right way to do the "cache pattern"
        return quickFutureCache.get(hashKey, new Callable<Future<Object>>() {

            @Override
            public Future<Object> call() throws Exception {
                return ask(tokenActorRef, hashableRequest, t);
            }
        });

    }

    public ServiceClient getServiceClient(HttpClientService httpClientService) {
        return new ServiceClient(null, httpClientService);
    }
}
