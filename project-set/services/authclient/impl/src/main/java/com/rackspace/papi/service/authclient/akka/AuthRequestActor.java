package com.rackspace.papi.service.authclient.akka;

import akka.actor.UntypedActor;
import com.rackspace.papi.commons.util.http.ServiceClient;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;

/**
 *
 */
public class AuthRequestActor extends UntypedActor {

    private ServiceClient serviceClient;

    @Override
    public void onReceive(Object message) throws Exception {

        if (message instanceof AuthRequest) {
            AuthRequest authRequest = (AuthRequest) message;

            ServiceClientResponse response = serviceClient.get(authRequest.getUri(), authRequest.getHeaders());
            getSender().tell(response, getSelf());
        } else {
            unhandled(message);
        }
    }

}
