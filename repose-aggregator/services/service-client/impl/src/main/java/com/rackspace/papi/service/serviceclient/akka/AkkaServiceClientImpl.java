package com.rackspace.papi.service.serviceclient.akka;


import akka.actor.*;
import akka.routing.RoundRobinRouter;
import akka.util.Timeout;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.rackspace.papi.commons.util.http.ServiceClient;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.service.httpclient.HttpClientService;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;

public class AkkaServiceClientImpl implements AkkaServiceClient {

    final private ServiceClient serviceClient;
    private ActorSystem actorSystem;
    private ActorRef tokenActorRef;
    private int numberOfActors;
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(AkkaServiceClientImpl.class);
    final Timeout t = new Timeout(50, TimeUnit.SECONDS);
    private final Cache<String, Future> quickFutureCache;

    private static final long FUTURE_CACHE_TTL = 500;

    @Autowired
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


    @Override
    public ServiceClientResponse get(String key, String uri, Map<String, String> headers) {

        ServiceClientResponse reusableServiceserviceClientResponse = null;
        AuthGetRequest authGetRequest = new AuthGetRequest(key, uri, headers);
        Future<ServiceClientResponse> future = getFuture(authGetRequest);
        try {
            reusableServiceserviceClientResponse = Await.result(future, Duration.create(50, TimeUnit.SECONDS));
        } catch (Exception e) {
            LOG.error("error with akka future: " + e.getMessage());
        }
        return reusableServiceserviceClientResponse;
    }

    @Override
    public void shutdown(){
        actorSystem.shutdown();

    }

    public Future getFuture(AuthGetRequest authGetRequest) {
        String token = authGetRequest.hashKey();
        Future<Object> newFuture;
        if (!quickFutureCache.asMap().containsKey(token)) {
            synchronized (quickFutureCache) {
                if (!quickFutureCache.asMap().containsKey(token)) {
                    newFuture = ask(tokenActorRef, authGetRequest, t);
                    quickFutureCache.asMap().putIfAbsent(token, newFuture);
                }
            }
        }
        return quickFutureCache.asMap().get(token);
    }

    public ServiceClient getServiceClient(HttpClientService httpClientService){
        return new ServiceClient(null,httpClientService);
    }
}
