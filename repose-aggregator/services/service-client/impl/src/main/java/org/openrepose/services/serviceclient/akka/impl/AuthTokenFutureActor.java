package org.openrepose.services.serviceclient.akka.impl;


import akka.actor.UntypedActor;
import org.openrepose.commons.utils.http.ServiceClient;
import org.openrepose.commons.utils.http.ServiceClientResponse;

public class AuthTokenFutureActor extends UntypedActor {

    private ServiceClient serviceClient;

    public AuthTokenFutureActor(ServiceClient serviceClient) {
        this.serviceClient = serviceClient;
    }

    @Override
    public void onReceive(Object message) throws Exception {

        if (message instanceof AuthGetRequest) {
            final AuthGetRequest authRequest = (AuthGetRequest) message;
            ServiceClientResponse serviceClientResponse = serviceClient.get(authRequest.getUri(), authRequest.getHeaders());
            ReusableServiceClientResponse reusableServiceClientResponse = new ReusableServiceClientResponse(serviceClientResponse.getStatus(), serviceClientResponse.getData());
            getSender().tell(reusableServiceClientResponse, getContext().parent());
        } else if( message instanceof AuthPostRequest) {
            final AuthPostRequest apr = (AuthPostRequest) message;
            ServiceClientResponse scr = serviceClient.post(apr.getUri(), apr.getHeaders(), apr.getPayload(), apr.getContentMediaType());
            ReusableServiceClientResponse rscr = new ReusableServiceClientResponse(scr.getStatus(), scr.getHeaders(), scr.getData());
            getSender().tell(rscr, getContext().parent());
        } else {
            unhandled(message);
        }
    }
}
