package com.rackspace.papi.service.authclient.akka;

import akka.actor.*;
import com.rackspace.papi.commons.util.http.ServiceClient;

public class RequestRoutingActor {

    public RequestRoutingActor(final ServiceClient serviceClient, ActorSystem actorSystem, int numberOfActors) {

       /* actorSystem.actorOf(new Props(new UntypedActorFactory() {
            public UntypedActor create() {
                return new AuthRequestActor(serviceClient);
            }
        }).withRouter(new RoundRobinRouter(numberOfActors)),"authRequestRouter");
    */
    }

}
