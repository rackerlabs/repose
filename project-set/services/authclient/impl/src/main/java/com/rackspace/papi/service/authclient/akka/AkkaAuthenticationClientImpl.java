package com.rackspace.papi.service.authclient.akka;


import akka.actor.*;
import akka.routing.ConsistentHashingRouter;
import akka.util.Timeout;
import com.rackspace.papi.commons.util.http.ServiceClient;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;

public class AkkaAuthenticationClientImpl {

    final private ServiceClient serviceClient;
    private ActorSystem actorSystem;
    private ActorRef tokenHashRouter;
    private int numberOfActors=20;


    public AkkaAuthenticationClientImpl(ServiceClient pServiceClient) {
        this.serviceClient = pServiceClient;
        actorSystem = ActorSystem.create("AuthClientActors");

        tokenHashRouter = actorSystem.actorOf(new Props(new UntypedActorFactory() {
            public UntypedActor create() {
                return new AuthTokenFutureActor(actorSystem , serviceClient);
            }
        }).withRouter(new ConsistentHashingRouter(numberOfActors)),"authRequestRouter");

    }



    public ServiceClientResponse validateToken(String token, String uri, Map<String, String> headers){

       ServiceClientResponse serviceClientResponse =null;

        AuthGetRequest authGetRequest = new AuthGetRequest(token, uri, headers);
        final Timeout t = new Timeout(Duration.create(50,
                TimeUnit.SECONDS));

        Future<Object> future = ask(tokenHashRouter, authGetRequest, t);

        while (!future.isCompleted()) {
            // sleep
            try {
                Thread.sleep(3l);
            } catch (InterruptedException e) {
                // do something with exception
            }
        }

        try{
             serviceClientResponse = (ServiceClientResponse) Await.result(future, t.duration());
        } catch(Exception e){
            //Log exception
        }




        return serviceClientResponse;
    }





}
