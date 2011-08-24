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

import com.rackspacecloud.docs.auth.api.v1.*;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.util.Calendar;

/**
 *
 * @author jhopper
 */
public class AuthServiceClient {

    private final HttpClient client;
    private final ResponseUnmarshaller responseUnmarshaller;
    private final String targetHostUri;

    public AuthServiceClient(String targetHostUri, String username, String password) {
        this.client = new HttpClient();
        this.responseUnmarshaller = new ResponseUnmarshaller();
        this.targetHostUri = targetHostUri;

        final Credentials defaultCredentials = new UsernamePasswordCredentials(username, password);
        client.getState().setCredentials(AuthScope.ANY, defaultCredentials);
    }

    public AuthenticationResponse validateToken(Account account, String token) throws AuthServiceException {
        final NameValuePair[] queryParameters = new NameValuePair[2];
        queryParameters[0] = new NameValuePair("belongsTo", account.getId());
        queryParameters[1] = new NameValuePair("type", account.getType());

        final GetMethod validateTokenMethod = get(targetHostUri + "/token/" + token, queryParameters);
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
        final GetMethod groupsMethod = get(targetHostUri + "/users/" + userId + "/groups", null);
        final int response = groupsMethod.getStatusCode();
        GroupsList groups = null;

        switch (response) {
            case 200:
                groups = responseUnmarshaller.unmarshall(groupsMethod, GroupsList.class);
        }

        return groups;
    }

    public ServiceCatalog getServiceCatalogForUser(String user) throws AuthServiceException {
        final GetMethod getServiceCatalogMethod = get(targetHostUri + "/users/" + user + "/serviceCatalog", null);
        final int response = getServiceCatalogMethod.getStatusCode();
        ServiceCatalog catalog = null;

        switch (response) {
            case 200:
                catalog = responseUnmarshaller.unmarshall(getServiceCatalogMethod, ServiceCatalog.class);
        }

        return catalog;
    }

    public AuthorizationResponse authorizeUser(String user, String requestedUri) throws AuthServiceException {
        final GetMethod getServiceCatalogMethod = get(targetHostUri + "/users/" + user + "/serviceCatalog", null);
        final int response = getServiceCatalogMethod.getStatusCode();
        AuthorizationResponse authorizationResponse = null;

        switch (response) {
            case 200:
                final ServiceCatalog catalog = responseUnmarshaller.unmarshall(getServiceCatalogMethod, ServiceCatalog.class);

                if (catalog != null) {
                    for (Service service : catalog.getService()) {
                        for (Endpoint endpoint : service.getEndpoint()) {
                            if (requestedUri.startsWith(endpoint.getPublicURL())) {
                                authorizationResponse = new AuthorizationResponse(response, true);
                            }
                        }
                    }
                }

                if (authorizationResponse == null) {
                    authorizationResponse = new AuthorizationResponse(403, false);
                }
                break;
            default :
                authorizationResponse = new AuthorizationResponse(response, false);                     
        }

        return authorizationResponse;
    }

    private GetMethod get(String uri, NameValuePair[] queryParameters) throws AuthServiceException {
        final GetMethod getMethod = new GetMethod(uri);
        getMethod.addRequestHeader("Accept", "application/xml");

        if (queryParameters != null) {
            getMethod.setQueryString(queryParameters);
        }

        try {
            client.executeMethod(getMethod);
        } catch (IOException ioe) {
            throw new AuthServiceException("Failed to successfully communicate with Auth v1.1 Service ("
                    + targetHostUri + "): " + ioe.getMessage(), ioe);
        }

        return getMethod;
    }
}
