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

import akka.routing.ConsistentHashingRouter.ConsistentHashable;

import javax.ws.rs.core.MediaType;
import java.util.Map;

public class AuthPostRequest extends ActorRequest implements ConsistentHashable {

    private String uri;
    private Map<String, String> headers;
    private String hashKey;
    private String payload;
    private MediaType contentMediaType;

    public AuthPostRequest(String hashKey, String uri, Map<String, String> headers, String payload, MediaType contentMediaType) {
        this.uri = uri;
        this.headers = headers;
        this.payload = payload;
        this.hashKey = hashKey;
        this.contentMediaType = contentMediaType;
    }

    public String getUri() {
        return uri;
    }

    public String getPayload() {
        return payload;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public String consistentHashKey() {
        return hashKey;
    }

    public MediaType getContentMediaType() {
        return contentMediaType;
    }
}
