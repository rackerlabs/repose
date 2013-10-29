package com.rackspace.papi.service.authclient.akka;


import akka.actor.*;
import akka.routing.ConsistentHashingRouter;
import akka.util.Timeout;
import com.rackspace.papi.commons.util.http.ServiceClient;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.Promise;
import scala.concurrent.duration.Duration;
import scala.util.Success;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;

public class AkkaAuthenticationClientImpl implements AkkaAuthenticationClient {

    final private ServiceClient serviceClient;
    private ActorSystem actorSystem;
    private ActorRef tokenHashRouter;
    private int numberOfActors=20;
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(AkkaAuthenticationClientImpl.class);



    public AkkaAuthenticationClientImpl(ServiceClient pServiceClient) {
        this.serviceClient = pServiceClient;

        Config customConf = ConfigFactory.parseString(
                "akka {actor { default-dispatcher {throughput = 10} } }");
        Config regularConf = ConfigFactory.defaultReference();
        Config combinedConf = customConf.withFallback(regularConf);

        actorSystem = ActorSystem.create("AuthClientActors", ConfigFactory.load(combinedConf));



        tokenHashRouter = actorSystem.actorOf(new Props(new UntypedActorFactory() {
            public UntypedActor create() {
                return new AuthTokenFutureActor(actorSystem , serviceClient);
            }
        }).withRouter(new ConsistentHashingRouter(numberOfActors)),"authRequestRouter");

    }



    @Override
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

             serviceClientResponse = (ServiceClientResponse) ((Success)((Promise)Await.result(future, t.duration())).future().value().get()).get();
        } catch(Exception e){
            //Log exception
        }




        return serviceClientResponse;
    }





}
