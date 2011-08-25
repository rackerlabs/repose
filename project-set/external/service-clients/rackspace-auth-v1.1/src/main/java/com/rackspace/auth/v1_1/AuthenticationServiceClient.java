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
package com.rackspace.auth.v1_1;

import com.rackspacecloud.docs.auth.api.v1.FullToken;
import com.rackspacecloud.docs.auth.api.v1.GroupsList;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.Calendar;

/**
 *
 * @author jhopper
 */
public class AuthenticationServiceClient {
    private final String targetHostUri;
    private final ServiceClient serviceClient;
    private final ResponseUnmarshaller responseUnmarshaller;

    public AuthenticationServiceClient(String targetHostUri, String username, String password) {
        this.responseUnmarshaller = new ResponseUnmarshaller();
        this.serviceClient = new ServiceClient(username, password);
        this.targetHostUri = targetHostUri;
    }

    public AuthenticationResponse validateToken(Account account, String token) {
        final NameValuePair[] queryParameters = new NameValuePair[2];
        queryParameters[0] = new NameValuePair("belongsTo", account.getUsername());
        queryParameters[1] = new NameValuePair("type", account.getType());

        final GetMethod validateTokenMethod = serviceClient.get(targetHostUri + "/token/" + token, queryParameters);
        final int response = validateTokenMethod.getStatusCode();
        AuthenticationResponse authenticationResponse;

        switch (response) {
            case 200:
                final FullToken tokenResponse = responseUnmarshaller.unmarshall(validateTokenMethod, FullToken.class);
                final Long expireTtl = tokenResponse.getExpires().toGregorianCalendar().getTimeInMillis() - Calendar.getInstance().getTimeInMillis();

                authenticationResponse = new AuthenticationResponse(response, tokenResponse.getId(), true, expireTtl.intValue());
                break;
            default :
                authenticationResponse = new AuthenticationResponse(response, null, false, -1);
        }

        return authenticationResponse;
    }

    public GroupsList getGroups(String userId) {
        final GetMethod groupsMethod = serviceClient.get(targetHostUri + "/users/" + userId + "/groups", null);
        final int response = groupsMethod.getStatusCode();
        GroupsList groups = null;

        switch (response) {
            case 200:
                groups = responseUnmarshaller.unmarshall(groupsMethod, GroupsList.class);
        }

        return groups;
    }
}
