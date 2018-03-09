/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.services.serviceclient.akka.impl;

import akka.actor.UntypedActor;
import io.opentracing.Scope;
import io.opentracing.noop.NoopScopeManager;
import io.opentracing.util.GlobalTracer;
import org.openrepose.commons.utils.http.ServiceClient;
import org.openrepose.commons.utils.http.ServiceClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class AuthTokenFutureActor extends UntypedActor {

    private static final Logger LOG = LoggerFactory.getLogger(AuthTokenFutureActor.class);

    private ServiceClient serviceClient;

    public AuthTokenFutureActor(ServiceClient serviceClient) {
        this.serviceClient = serviceClient;
    }

    @Override
    public void onReceive(Object message) {
        Scope scope = NoopScopeManager.NoopScope.INSTANCE;
        if (message instanceof ActorRequest) {
            //Get the immutable map, and set all my thread context
            final ActorRequest request = (ActorRequest) message;
            for (String key : request.getLoggingContextMap().keySet()) {
                MDC.put(key, request.getLoggingContextMap().get(key));
            }
            scope = GlobalTracer.get().scopeManager().activate(request.getActiveSpan(), false);
        }

        LOG.trace("AuthTokenFutureActor request!");

        if (message instanceof AuthGetRequest) {
            final AuthGetRequest authRequest = (AuthGetRequest) message;
            ServiceClientResponse serviceClientResponse = serviceClient.get(authRequest.getUri(), authRequest.getHeaders());
            ReusableServiceClientResponse reusableServiceClientResponse = new ReusableServiceClientResponse(serviceClientResponse.getStatus(), serviceClientResponse.getHeaders(), serviceClientResponse.getData());
            getSender().tell(reusableServiceClientResponse, getContext().parent());
        } else if (message instanceof AuthPostRequest) {
            final AuthPostRequest apr = (AuthPostRequest) message;
            ServiceClientResponse scr = serviceClient.post(apr.getUri(), apr.getHeaders(), apr.getPayload(), apr.getContentMediaType());
            ReusableServiceClientResponse rscr = new ReusableServiceClientResponse(scr.getStatus(), scr.getHeaders(), scr.getData());
            getSender().tell(rscr, getContext().parent());
        } else {
            unhandled(message);
        }

        // todo: these might not be called if a RuntimeException is thrown -- should this method be refactored?
        MDC.clear();
        scope.close();
    }
}
