package com.rackspace.papi.service.authclient.akka;


import akka.actor.*;
import akka.routing.ConsistentHashingRouter;
import akka.util.Timeout;
import com.rackspace.papi.commons.util.http.ServiceClient;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;

public class AkkaAuthenticationClient {

    final private ServiceClient serviceClient;
    private ActorSystem actorSystem;
    private ActorRef tokenHashRouter;
    private int numberOfActors=20;


    public AkkaAuthenticationClient(ServiceClient pServiceClient) {
        this.serviceClient = pServiceClient;
        actorSystem = ActorSystem.create("AuthClientActors");

        tokenHashRouter = actorSystem.actorOf(new Props(new UntypedActorFactory() {
            public UntypedActor create() {
                return new AuthTokenFutureActor(actorSystem , serviceClient);
            }
        }).withRouter(new ConsistentHashingRouter(numberOfActors)),"authRequestRouter");

    }



    private ServiceClientResponse validateToken(String token, String uri, Map<String, String> headers){


        AuthGetRequest authGetRequest = new AuthGetRequest(token, uri, headers);
        final Timeout t = new Timeout(Duration.create(5,
                TimeUnit.SECONDS));

        Future<Object> future = ask(tokenHashRouter, authGetRequest, t);

        while (!future.isCompleted()) {
            // sleep
            try {
                Thread.sleep(1l);
            } catch (InterruptedException e) {
                // do something with exception
            }
        }


        return null;
    }





}
