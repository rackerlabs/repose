package com.rackspace.auth.v1_1;

/*
 *  Copyright 2010 Rackspace.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

/**
 *
 * @author jhopper
 */
public class AuthenticationResponse {

    private final int responseCode;
    private final String authId;
    private final boolean authenticated;
    private final int ttl;

    public AuthenticationResponse(int responseCode, String authId, boolean authenticated, int ttl) {
        this.responseCode = responseCode;
        this.authId = authId;
        this.authenticated = authenticated;
        this.ttl = ttl;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public boolean authenticated() {
        return authenticated;
    }

    public String getAuthToken() {
        return authId;
    }

    public int getTtl() {
        return ttl;
    }
}
