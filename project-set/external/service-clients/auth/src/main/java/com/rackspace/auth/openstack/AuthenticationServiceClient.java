package com.rackspace.auth.openstack;

import com.rackspace.auth.*;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.ServiceClient;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.commons.util.transform.jaxb.JaxbEntityToXml;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.openstack.docs.identity.api.v2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class hosts the interaction between Repose and an OpenStack Identity Endpoint.
 */
public class AuthenticationServiceClient implements AuthenticationService {

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationServiceClient.class);
    private static final String AUTH_TOKEN_HEADER = "X-Auth-Token";
    private static final String ACCEPT_HEADER = "Accept";
    private static final String TOKENS = "/tokens/";
    public static final String ENDPOINTS = "/endpoints";
    private final String targetHostUri;
    private final ServiceClient serviceClient;
    private final ResponseUnmarshaller openStackCoreResponseUnmarshaller;
    private final ResponseUnmarshaller openStackGroupsResponseUnmarshaller;

    private AdminToken currentAdminToken;
    private final String requestBody;


    public AuthenticationServiceClient(String targetHostUri, String username, String password, String tenantId,
                                       ResponseUnmarshaller openStackCoreResponseUnmarshaller,
                                       ResponseUnmarshaller openStackGroupsResponseUnmarshaller,
                                       JaxbEntityToXml jaxbToString,
                                       ServiceClient serviceClient) {
        this.openStackCoreResponseUnmarshaller = openStackCoreResponseUnmarshaller;
        this.openStackGroupsResponseUnmarshaller = openStackGroupsResponseUnmarshaller;
        this.serviceClient = serviceClient;
        this.targetHostUri = targetHostUri;

        ObjectFactory objectFactory = new ObjectFactory();
        PasswordCredentialsRequiredUsername credentials = new PasswordCredentialsRequiredUsername();
        credentials.setUsername(username);
        credentials.setPassword(password);

        JAXBElement jaxbCredentials = objectFactory.createPasswordCredentials(credentials);

        AuthenticationRequest request = new AuthenticationRequest();

        if (!StringUtilities.isBlank(tenantId)) {
            request.setTenantId(tenantId);
        }

        request.setCredential(jaxbCredentials);

        JAXBElement jaxbRequest = objectFactory.createAuth(request);
        requestBody = jaxbToString.transform(jaxbRequest);
    }

    @Override
    public AuthToken validateToken(String tenant, String userToken) { //this is where we ask auth service if token is valid

        OpenStackToken token = null;

        ServiceClientResponse<AuthenticateResponse> serviceResponse = validateUser(userToken, tenant, false);

        switch (HttpStatusCode.fromInt(serviceResponse.getStatusCode())) {
            case OK:
                token = getOpenStackToken(serviceResponse);
                break;

            case NOT_FOUND:
                // User's token is bad
                LOG.error("Unable to validate token.  Invalid token. " + serviceResponse.getStatusCode());
                break;

            case UNAUTHORIZED:
                LOG.error("Unable to validate token: " + serviceResponse.getStatusCode() + " :admin token expired. Retrieving new admin token and retrying token validation...");

                serviceResponse = validateUser(userToken, tenant, true);

                if (serviceResponse.getStatusCode() == HttpStatusCode.OK.intValue()) {
                    token = getOpenStackToken(serviceResponse);
                } else if (serviceResponse.getStatusCode() == HttpStatusCode.NOT_FOUND.intValue()) {
                    LOG.error("Unable to validate token.  Invalid token. " + serviceResponse.getStatusCode());
                } else {
                    LOG.error("Still unable to validate token: " + serviceResponse.getStatusCode());
                    throw new AuthServiceException("Unable to authenticate user with configured Admin credentials");
                }
                break;


            default:
                LOG.error("Authentication Service returned an unexpected response status code: " + serviceResponse.getStatusCode());
                throw new AuthServiceException("Unable to validate token. Response from " + targetHostUri + ": " + serviceResponse.getStatusCode());
        }

        return token;
    }

    private ServiceClientResponse<AuthenticateResponse> validateUser(String userToken, String tenant, boolean force) {
        final Map<String, String> headers = new HashMap<String, String>();
        headers.put(ACCEPT_HEADER, MediaType.APPLICATION_XML);
        headers.put(AUTH_TOKEN_HEADER, getAdminToken(force));

        return serviceClient.get(targetHostUri + TOKENS + userToken, headers);
    }

    private OpenStackToken getOpenStackToken(ServiceClientResponse<AuthenticateResponse> serviceResponse) {
        final AuthenticateResponse authenticateResponse = openStackCoreResponseUnmarshaller.unmarshall(serviceResponse.getData(), AuthenticateResponse.class);
        OpenStackToken token = null;

        token = new OpenStackToken(authenticateResponse);
        return token;
    }

    @Override
    public List<Endpoint> getEndpointsForToken(String userToken) {
        final Map<String, String> headers = new HashMap<String, String>();

        headers.put(ACCEPT_HEADER, MediaType.APPLICATION_XML);


        headers.put(AUTH_TOKEN_HEADER, getAdminToken(false));

        ServiceClientResponse<EndpointList> endpointListResponse = serviceClient.get(targetHostUri + TOKENS + userToken +
                ENDPOINTS, headers);
        List<Endpoint> endpointList = new ArrayList<Endpoint>();

        switch (HttpStatusCode.fromInt(endpointListResponse.getStatusCode())) {
            case OK:
                endpointList = getEndpointList(endpointListResponse);

                break;
            case UNAUTHORIZED:
                LOG.error("Unable to get endpoints for user: " + endpointListResponse.getStatusCode() + " :admin token expired. " +
                        "Retrieving new admin token and retrying endpoints retrieval...");

                headers.put(AUTH_TOKEN_HEADER, getAdminToken(true));
                endpointListResponse = serviceClient.get(targetHostUri + TOKENS + userToken + ENDPOINTS, headers);

                if (endpointListResponse.getStatusCode() == HttpStatusCode.ACCEPTED.intValue()) {
                    endpointList = getEndpointList(endpointListResponse);
                } else {
                    LOG.error("Still unable to get endpoints: " + endpointListResponse.getStatusCode());
                    throw new AuthServiceException("Unable to retrieve service catalog for user with configured Admin credentials");
                }
                break;
            default:
                LOG.error("Unable to get endpoints for token. Status code: " + endpointListResponse.getStatusCode());
                throw new AuthServiceException("Unable to retrieve service catalog for user. Response from " + targetHostUri + ": " + endpointListResponse.getStatusCode());

        }

        return endpointList;
    }

    // Method to take in the format and token, then use that info to get the endpoints catalog from auth, and return it encoded.
    @Override
    public String getBase64EndpointsStringForHeaders(String userToken, String format) {
        final Map<String, String> headers = new HashMap<String, String>();
        String adminToken = "";

        //defaulting to json format
        if (format.equalsIgnoreCase("xml")) {
            format = MediaType.APPLICATION_XML;
        } else {
            format = MediaType.APPLICATION_JSON;
        }

        //telling the service what format to send the endpoints to us in
        headers.put(ACCEPT_HEADER, format);
        headers.put(AUTH_TOKEN_HEADER, getAdminToken(false));


        ServiceClientResponse serviceClientResponse = serviceClient.get(targetHostUri + TOKENS + userToken + ENDPOINTS, headers);

        String rawEndpointsData = "";

        switch (HttpStatusCode.fromInt(serviceClientResponse.getStatusCode())) {
            case OK:
                rawEndpointsData = convertStreamToBase64String(serviceClientResponse.getData());
                break;
            case UNAUTHORIZED:
                LOG.error("Unable to get endpoints for user: " + serviceClientResponse.getStatusCode() + " :admin token expired. Retrieving new admin token and retrying endpoints retrieval...");

                headers.put(AUTH_TOKEN_HEADER, getAdminToken(true));
                serviceClientResponse = serviceClient.get(targetHostUri + TOKENS + userToken + ENDPOINTS, headers);

                if (serviceClientResponse.getStatusCode() == HttpStatusCode.ACCEPTED.intValue()) {
                    rawEndpointsData = convertStreamToBase64String(serviceClientResponse.getData());
                } else {
                    LOG.error("Still unable to get endpoints: " + serviceClientResponse.getStatusCode());
                    throw new AuthServiceException("Unable to retrieve service catalog for user with configured Admin credentials");
                }
                break;
            default:
                LOG.error("Unable to get endpoints for token. Status code: " + serviceClientResponse.getStatusCode());
                throw new AuthServiceException("Unable to retrieve service catalog for user. Response from " + targetHostUri + ": " + serviceClientResponse.getStatusCode());

        }

        return rawEndpointsData;
    }

    private String convertStreamToBase64String(InputStream inputStream) {
        StringWriter stringWriter = new StringWriter();
        try {
            IOUtils.copy(inputStream, stringWriter, "UTF8");
        } catch (IOException e) {
            LOG.error(e.getMessage(), "Unable to copy stream.");
        }
        String stringFromStream = stringWriter.toString();
        byte[] encodedString = Base64.encodeBase64(stringFromStream.getBytes());
        return new String(encodedString);
    }

    private List<Endpoint> getEndpointList(ServiceClientResponse<EndpointList> endpointListResponse) {
        List<Endpoint> endpointList = new ArrayList<Endpoint>();

        final EndpointList unmarshalledEndpoints = openStackCoreResponseUnmarshaller.unmarshall(endpointListResponse.getData(), EndpointList.class);

        if (unmarshalledEndpoints != null) {
            endpointList = unmarshalledEndpoints.getEndpoint();
        }

        return endpointList;
    }

    @Override
    public AuthGroups getGroups(String userId) {
        final Map<String, String> headers = new HashMap<String, String>();

        headers.put(ACCEPT_HEADER, MediaType.APPLICATION_XML);
        headers.put(AUTH_TOKEN_HEADER, getAdminToken(false));


        ServiceClientResponse<Groups> serviceResponse = serviceClient.get(targetHostUri + "/users/" + userId + "/RAX-KSGRP", headers);
        AuthGroups authGroups = null;

        switch (HttpStatusCode.fromInt(serviceResponse.getStatusCode())) {
            case OK:
                authGroups = getAuthGroups(serviceResponse);
                break;
            case UNAUTHORIZED:
                LOG.error("Unable to get groups for user: " + serviceResponse.getStatusCode() + " :admin token expired. Retrieving new admin token and retrying groups retrieval...");


                headers.put(AUTH_TOKEN_HEADER, getAdminToken(true));

                serviceResponse = serviceClient.get(targetHostUri + "/users/" + userId + "/RAX-KSGRP", headers);

                if (serviceResponse.getStatusCode() == HttpStatusCode.ACCEPTED.intValue()) {
                    authGroups = getAuthGroups(serviceResponse);
                } else {
                    LOG.error("Still unable to get groups: " + serviceResponse.getStatusCode());
                    throw new AuthServiceException("Unable to retrieve groups for user. Response from " + targetHostUri + ": " + serviceResponse.getStatusCode());

                }
                break;
            default:
                LOG.error("Unable to get groups for user id: " + userId + " Status code: " + serviceResponse.getStatusCode());
                throw new AuthServiceException("Unable to retrieve groups for user. Response from " + targetHostUri + ": " + serviceResponse.getStatusCode());


        }

        return authGroups;
    }

    private AuthGroups getAuthGroups(ServiceClientResponse<Groups> serviceResponse) {
        final List<AuthGroup> authGroupList = new ArrayList<AuthGroup>();
        final Groups groups = openStackGroupsResponseUnmarshaller.unmarshall(serviceResponse.getData(), Groups.class);

        if (groups != null) {
            for (Group group : groups.getGroup()) {
                final AuthGroup authGroup = new OpenStackGroup(group);
                authGroupList.add(authGroup);
            }

            return new AuthGroups(authGroupList);
        } else {
            LOG.warn("Response unmarshaller returned null groups.");


            return null;
        }
    }

    private synchronized String getAdminToken(boolean force) {

        String adminToken = !force && currentAdminToken != null && currentAdminToken.isValid() ? currentAdminToken.getToken() : null;

        if (adminToken == null) {
            final ServiceClientResponse<AuthenticateResponse> serviceResponse = serviceClient.post(targetHostUri + "/tokens", requestBody, MediaType.APPLICATION_XML_TYPE);

            switch (HttpStatusCode.fromInt(serviceResponse.getStatusCode())) {
                case OK:
                    final AuthenticateResponse authenticateResponse = openStackCoreResponseUnmarshaller.unmarshall(serviceResponse.getData(), AuthenticateResponse.class);

                    Token token = authenticateResponse.getToken();
                    currentAdminToken = new AdminToken(token.getId(), token.getExpires().toGregorianCalendar());
                    adminToken = currentAdminToken.getToken();
                    break;

                default:
                    LOG.error("Unable to get admin token.  Verify admin credentials. " + serviceResponse.getStatusCode());
                    currentAdminToken = null;
                    throw new AuthServiceException("Unable to retrieve admin token ");

            }
        }

        return adminToken;
    }

}
