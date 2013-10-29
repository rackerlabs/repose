package com.rackspace.papi.service.authclient.akka;


import akka.actor.*;
import akka.routing.RoundRobinRouter;
import akka.util.Timeout;
import com.rackspace.papi.commons.util.http.ServiceClient;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;

public class AkkaAuthenticationClientImpl implements AkkaAuthenticationClient {

    final private ServiceClient serviceClient;
    private ActorSystem actorSystem;
    private ActorRef tokenActorRef;
    private int numberOfActors = 20;
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(AkkaAuthenticationClientImpl.class);
    private ConcurrentHashMap<String, Future> tokenFutureMap;
    final Timeout t = new Timeout(Duration.create(50, TimeUnit.SECONDS));


    public AkkaAuthenticationClientImpl(ServiceClient pServiceClient) {
        this.serviceClient = pServiceClient;

        Config customConf = ConfigFactory.parseString(
                "akka {actor { default-dispatcher {throughput = 10} } }");
        Config regularConf = ConfigFactory.defaultReference();
        Config combinedConf = customConf.withFallback(regularConf);

        actorSystem = ActorSystem.create("AuthClientActors", ConfigFactory.load(combinedConf));

        tokenFutureMap = new ConcurrentHashMap<String, Future>();

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

        if (!tokenFutureMap.containsKey(token)) {
            synchronized (tokenFutureMap) {
                if (!tokenFutureMap.containsKey(token)) {
                    newFuture = ask(tokenActorRef, authGetRequest, t);
                    tokenFutureMap.putIfAbsent(token, newFuture);
                }
            }
        }

        return tokenFutureMap.get(token);
    }


}
