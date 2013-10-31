package com.rackspace.papi.service.authclient.akka;


import akka.actor.*;
import akka.routing.RoundRobinRouter;
import akka.util.Timeout;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.rackspace.papi.commons.util.http.ServiceClient;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;

public class AkkaAuthenticationClientImpl implements AkkaAuthenticationClient {

    final private ServiceClient serviceClient;
    private ActorSystem actorSystem;
    private ActorRef tokenActorRef;
    private int numberOfActors = 20;
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(AkkaAuthenticationClientImpl.class);
    final Timeout t = new Timeout(Duration.create(50, TimeUnit.SECONDS));
    private final Cache<String, Future> quickFutureCache;

    private static final long FUTURE_CACHE_TTL = 500;
    private static final long MAX_FUTURE_CACHE_SIZE = 1000;  // just guessing on this

    public AkkaAuthenticationClientImpl(ServiceClient pServiceClient) {
        this.serviceClient = pServiceClient;

        Config customConf = ConfigFactory.parseString(
                "akka { actor { default-dispatcher {throughput = 10} } }");
        Config regularConf = ConfigFactory.defaultReference();
        Config combinedConf = customConf.withFallback(regularConf);

        actorSystem = ActorSystem.create("AuthClientActors", ConfigFactory.load(combinedConf));

        quickFutureCache = CacheBuilder.newBuilder()
                .expireAfterWrite(FUTURE_CACHE_TTL, TimeUnit.MILLISECONDS)
                .build();

        tokenActorRef = actorSystem.actorOf(new Props(new UntypedActorFactory() {
            public UntypedActor create() {
                return new AuthTokenFutureActor(serviceClient);
            }
        }).withRouter(new RoundRobinRouter(numberOfActors)),"authRequestRouter");

    }


    @Override
    public ServiceClientResponse validateToken(String token, String uri, Map<String, String> headers) {

        ServiceClientResponse serviceClientResponse = null;

        AuthGetRequest authGetRequest = new AuthGetRequest(token, uri, headers);

        Future<ServiceClientResponse> future = getFuture(authGetRequest);
        try {

            serviceClientResponse = Await.result(future, Duration.create(6, TimeUnit.SECONDS));

             } catch (Exception e) {
            //TODO
        }

        return serviceClientResponse;
    }


    public Future getFuture(AuthGetRequest authGetRequest) {
        String token = authGetRequest.getToken();

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


}
