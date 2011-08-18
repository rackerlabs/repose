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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.StringReader;
import java.util.Calendar;

/**
 *
 * @author jhopper
 */
public class AuthServiceClient {

    private final HttpClient client;
    private final Unmarshaller jaxbTypeUnmarshaller;
    private final String targetHostUri;

    public AuthServiceClient(String targetHostUri, String username, String password) {
        this.client = new HttpClient();

        try {
            final JAXBContext jaxbContext = JAXBContext.newInstance(com.rackspacecloud.docs.auth.api.v1.ObjectFactory.class);
            jaxbTypeUnmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException jaxbe) {
            throw new AuthServiceException(
                    "Possible deployment problem! Unable to build JAXB Context for Auth v1.1 schema types. Reason: "
                    + jaxbe.getMessage(), jaxbe);
        }

        this.targetHostUri = targetHostUri;

        final Credentials defaultCredentials = new UsernamePasswordCredentials(username, password);
        client.getState().setCredentials(AuthScope.ANY, defaultCredentials);
    }

    public AuthenticationResponse validateToken(Account account, String token) throws AuthServiceException {
        final GetMethod validateTokenMethod = new GetMethod(targetHostUri + "/token/" + token);

        final NameValuePair[] queryParameters = new NameValuePair[2];
        queryParameters[0] = new NameValuePair("belongsTo", account.getId());
        queryParameters[1] = new NameValuePair("type", account.getType());

        validateTokenMethod.setQueryString(queryParameters);
        validateTokenMethod.addRequestHeader("Accept", "application/xml");

        final int response = execute(validateTokenMethod);

        switch (response) {
            case 200:
                final FullToken tokenResponse = marshallFromMethod(validateTokenMethod, FullToken.class);
                final Long expireTtl = tokenResponse.getExpires().toGregorianCalendar().getTimeInMillis() - Calendar.getInstance().getTimeInMillis();

                return new AuthenticationResponse(response, tokenResponse.getId(), true, expireTtl.intValue());
        }

        return new AuthenticationResponse(response, null, false, -1);
    }

    public ServiceCatalog getServiceCatalogForUser(String user) throws AuthServiceException {
        final GetMethod getServiceCatalogMethod = new GetMethod(targetHostUri + "/users/" + user + "/serviceCatalog");
        getServiceCatalogMethod.addRequestHeader("Accept", "application/xml");

        final int response = execute(getServiceCatalogMethod);

        ServiceCatalog catalog = null;

        switch (response) {
            case 200:
                catalog = marshallFromMethod(getServiceCatalogMethod, ServiceCatalog.class);
        }

        return catalog;
    }

    public GroupsList getGroups(String userId) {
        final GetMethod groupsMethod = new GetMethod(targetHostUri + "/users/" + userId + "/groups");
        groupsMethod.addRequestHeader("Accept", "application/xml");

        final int response = execute(groupsMethod);

        GroupsList groups = null;

        switch (response) {
            case 200:
                groups = marshallFromMethod(groupsMethod, GroupsList.class);
        }

        return groups;
    }

    //TODO: Fix multiple returns
    public AuthorizationResponse authorizeUser(String user, String requestedUri) throws AuthServiceException {
        final GetMethod getServiceCatalogMethod = new GetMethod(targetHostUri + "/users/" + user + "/serviceCatalog");
        getServiceCatalogMethod.addRequestHeader("Accept", "application/xml");

        final int response = execute(getServiceCatalogMethod);

        switch (response) {
            case 200:
                final ServiceCatalog catalog = marshallFromMethod(getServiceCatalogMethod, ServiceCatalog.class);

                if (catalog != null) {
                    for (Service service : catalog.getService()) {
                        for (Endpoint endpoint : service.getEndpoint()) {
                            if (requestedUri.startsWith(endpoint.getPublicURL())) {
                                return new AuthorizationResponse(response, true);
                            }
                        }
                    }
                }

                return new AuthorizationResponse(403, false);
        }

        return new AuthorizationResponse(response, false);
    }

    private <T> T marshallFromMethod(GetMethod method, Class<T> expectedType) {
        try {
            return marshalResponse(method.getResponseBodyAsString(), expectedType);
        } catch (IOException ioe) {
            throw new AuthServiceException("Failed to get response body from response.", ioe);
        }
    }

    private <T> T marshalResponse(String responseBody, Class<T> classToMarshallTo) throws AuthServiceException {
        try {
            final Object o = jaxbTypeUnmarshaller.unmarshal(new StringReader(responseBody));

            if (o instanceof JAXBElement && ((JAXBElement) o).getDeclaredType().equals(classToMarshallTo)) {
                return ((JAXBElement<T>) o).getValue();
            } else if (o instanceof FullToken) {
                return classToMarshallTo.cast(o);
            } else {
                throw new AuthServiceException("Failed to unmarshal response body. Unexpected element encountered. Body output is in debug.");
                        
            }
        } catch (JAXBException jaxbe) {
            throw new AuthServiceException("Failed to unmarshal response body. Body output is in debug. Reason: "
                    + jaxbe.getMessage(), jaxbe);
        }
    }

    private int execute(GetMethod method) {
        try {
            return client.executeMethod(method);
        } catch (IOException ioe) {
            throw new AuthServiceException("Failed to successfully communicate with Auth v1.1 Service ("
                    + targetHostUri + "): " + ioe.getMessage(), ioe);
        }
    }
}
