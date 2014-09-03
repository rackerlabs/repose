package com.rackspace.papi.service.serviceclient.akka;


import akka.actor.UntypedActor;
import com.rackspace.papi.commons.util.http.ServiceClient;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;

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
            ReusableServiceClientResponse reusableServiceClientResponse = new ReusableServiceClientResponse(serviceClientResponse.getStatusCode(), serviceClientResponse.getData());
            getSender().tell(reusableServiceClientResponse, getContext().parent());
        } else if( message instanceof AuthPostRequest) {
            final AuthPostRequest apr = (AuthPostRequest) message;
            ServiceClientResponse scr = serviceClient.post(apr.getUri(), apr.getPayload(), apr.getContentMediaType(), apr.getAcceptMediaType());
            ReusableServiceClientResponse rscr = new ReusableServiceClientResponse(scr.getStatusCode(), scr.getHeaders(), scr.getData());
            getSender().tell(rscr, getContext().parent());
        } else {
            unhandled(message);
        }
    }
}
