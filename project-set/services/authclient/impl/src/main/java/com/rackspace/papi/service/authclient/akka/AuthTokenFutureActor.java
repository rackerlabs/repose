package com.rackspace.papi.service.authclient.akka;


import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import com.rackspace.papi.commons.util.http.ServiceClient;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import scala.concurrent.Future;

import java.util.HashMap;
import java.util.concurrent.Callable;

import static akka.dispatch.Futures.future;

public class AuthTokenFutureActor extends UntypedActor {

    private HashMap<String,Future>  tokenFutures;
    private ServiceClient serviceClient;
    private ActorSystem actorSystem;

    public AuthTokenFutureActor(ActorSystem actorSystem, ServiceClient serviceClient) {
        this.tokenFutures = new HashMap<String,Future>();
        this.serviceClient = serviceClient;
        this.actorSystem =   actorSystem;
    }


    @Override
    public void onReceive(Object message) throws Exception {

        if (message instanceof AuthGetRequest) {

            final AuthGetRequest authRequest = (AuthGetRequest) message;
            String token=authRequest.getToken();

            if(tokenFutures.containsKey(token)){
                getSender().tell(tokenFutures.get(token), getSelf());
            } else{
                Future<ServiceClientResponse> future = future( new Callable<ServiceClientResponse>() {
                    @Override
                    public ServiceClientResponse call() throws Exception {
                        return serviceClient.get(authRequest.getUri(), authRequest.getHeaders());
                    }
                }, actorSystem.dispatcher());

                tokenFutures.put(token,future);
                getSender().tell(tokenFutures.get(token), getContext().parent());

            }

        } else {
            unhandled(message);
        }
    }
}
