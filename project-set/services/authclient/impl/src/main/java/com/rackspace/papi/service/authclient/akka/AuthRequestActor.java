package com.rackspace.papi.service.authclient.akka;

import akka.actor.UntypedActor;
import com.rackspace.papi.commons.util.http.ServiceClient;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;

/**
 * Actor responsible for handling authentication requests and returning
 * service client responses
 */
public class AuthRequestActor extends UntypedActor {

    private ServiceClient serviceClient;

    public AuthRequestActor(ServiceClient serviceClient) {
        this.serviceClient = serviceClient;
    }

    @Override
    public void onReceive(Object message) throws Exception {

        if (message instanceof AuthGetRequest) {
            AuthGetRequest authRequest = (AuthGetRequest) message;

            ServiceClientResponse response = serviceClient.get(authRequest.getUri(), authRequest.getHeaders());
            getSender().tell(response, getSelf());
        } else {
            unhandled(message);
        }
    }

}
