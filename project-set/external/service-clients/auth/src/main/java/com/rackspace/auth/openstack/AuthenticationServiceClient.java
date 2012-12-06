package com.rackspace.auth.openstack;

import com.rackspace.auth.AuthGroup;
import com.rackspace.auth.AuthGroups;
import com.rackspace.auth.AuthServiceException;
import com.rackspace.auth.AuthToken;
import com.rackspace.auth.ResponseUnmarshaller;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.ServiceClient;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import org.openstack.docs.identity.api.v2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBElement;
import java.util.*;

/**
 * @author fran
 */
public class AuthenticationServiceClient implements AuthenticationService {

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationServiceClient.class);
    private static final String AUTH_TOKEN_HEADER = "X-Auth-Token";
    private static final String ACCEPT_HEADER = "Accept";
    private final String targetHostUri;
    private final ServiceClient serviceClient;
    private final ResponseUnmarshaller openStackCoreResponseUnmarshaller;
    private final ResponseUnmarshaller openStackGroupsResponseUnmarshaller;
    private AdminToken currentAdminToken;
    private final JAXBElement jaxbRequest;

    public AuthenticationServiceClient(String targetHostUri, String username, String password, String tenantId,
            ResponseUnmarshaller openStackCoreResponseUnmarshaller,
            ResponseUnmarshaller openStackGroupsResponseUnmarshaller) {
        this.openStackCoreResponseUnmarshaller = openStackCoreResponseUnmarshaller;
        this.openStackGroupsResponseUnmarshaller = openStackGroupsResponseUnmarshaller;
        this.serviceClient = new ServiceClient();
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

        this.jaxbRequest = objectFactory.createAuth(request);
    }

    @Override
    public AuthToken validateToken(String tenant, String userToken) {

        OpenStackToken token = null;

        ServiceClientResponse<AuthenticateResponse> serviceResponse = validateUser(userToken, false);

        switch (HttpStatusCode.fromInt(serviceResponse.getStatusCode())) {
            case OK:
                token = getOpenStackToken(tenant, serviceResponse);
                break;

            case NOT_FOUND:
                // User's token is bad
                LOG.warn("Unable to validate token for tenant.  Invalid token. " + serviceResponse.getStatusCode());
                break;

            case UNAUTHORIZED:
                LOG.warn("Unable to validate token for tenant: " + serviceResponse.getStatusCode() + " :admin token expired. Retrieving new admin token and retrying token validation...");

                serviceResponse = validateUser(userToken, true);

                if (serviceResponse.getStatusCode() == HttpStatusCode.OK.intValue()) {
                    token = getOpenStackToken(tenant, serviceResponse);
                } else {
                    LOG.warn("Still unable to validate token for tenant: " + serviceResponse.getStatusCode());
                    throw new AuthServiceException("Unable to authenticate user with configured Admin credentials");
                }
                break;

            case INTERNAL_SERVER_ERROR:
                // Internal server error from auth
                LOG.warn("Internal server error from auth. " + serviceResponse.getStatusCode());
                break;
        }

        return token;
    }

    private ServiceClientResponse<AuthenticateResponse> validateUser(String userToken, boolean force) {
        ServiceClientResponse<AuthenticateResponse> serviceResponse;

        final Map<String, String> headers = new HashMap<String, String>();
        headers.put(ACCEPT_HEADER, MediaType.APPLICATION_XML);
        headers.put(AUTH_TOKEN_HEADER, getAdminToken(force));
        serviceResponse = serviceClient.get(targetHostUri + "/tokens/" + userToken, headers);

        return serviceResponse;
    }

    private OpenStackToken getOpenStackToken(String tenant, ServiceClientResponse<AuthenticateResponse> serviceResponse) {
        final AuthenticateResponse authenticateResponse = openStackCoreResponseUnmarshaller.unmarshall(serviceResponse.getData(), AuthenticateResponse.class);
        OpenStackToken token = null;

        if (!StringUtilities.isBlank(tenant)) {
            if (StringUtilities.nullSafeEqualsIgnoreCase(tenant, authenticateResponse.getToken().getTenant().getId())) {
                token = new OpenStackToken(tenant, authenticateResponse);
            }
        } else {
            token = new OpenStackToken(authenticateResponse);
        }


        return token;
    }

    @Override
    public List<Endpoint> getEndpointsForToken(String userToken) {
        final Map<String, String> headers = new HashMap<String, String>();

        headers.put(ACCEPT_HEADER, MediaType.APPLICATION_XML);
        headers.put(AUTH_TOKEN_HEADER, getAdminToken(false));

        ServiceClientResponse<EndpointList> endpointListResponse = serviceClient.get(targetHostUri + "/tokens/" + userToken + "/endpoints", headers);
        List<Endpoint> endpointList = new ArrayList<Endpoint>();

        switch (HttpStatusCode.fromInt(endpointListResponse.getStatusCode())) {
            case OK:
                endpointList = getEndpointList(endpointListResponse);

                break;
            case UNAUTHORIZED:
                LOG.warn("Unable to get endpoints for user: " + endpointListResponse.getStatusCode() + " :admin token expired. Retrieving new admin token and retrying endpoints retrieval...");

                headers.put(AUTH_TOKEN_HEADER, getAdminToken(true));
                endpointListResponse = serviceClient.get(targetHostUri + "/tokens/" + userToken + "/endpoints", headers);

                if (endpointListResponse.getStatusCode() == HttpStatusCode.ACCEPTED.intValue()) {
                    endpointList = getEndpointList(endpointListResponse);
                } else {
                    LOG.warn("Still unable to get endpoints: " + endpointListResponse.getStatusCode());
                    throw new AuthServiceException("Unable to retrieve service catalog for user with configured Admin credentials");
                }
                break;
            default:
                LOG.warn("Unable to get endpoints for token. Status code: " + endpointListResponse.getStatusCode());
                break;
        }

        return endpointList;
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
                LOG.warn("Unable to get groups for user: " + serviceResponse.getStatusCode() + " :admin token expired. Retrieving new admin token and retrying groups retrieval...");

                headers.put(AUTH_TOKEN_HEADER, getAdminToken(true));

                serviceResponse = serviceClient.get(targetHostUri + "/users/" + userId + "/RAX-KSGRP", headers);

                if (serviceResponse.getStatusCode() == HttpStatusCode.ACCEPTED.intValue()) {
                    authGroups = getAuthGroups(serviceResponse);
                } else {
                    LOG.warn("Still unable to get groups: " + serviceResponse.getStatusCode());
                }

                break;
            default:
                LOG.warn("Unable to get groups for user id: " + userId + " Status code: " + serviceResponse.getStatusCode());
                break;

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
            final ServiceClientResponse<AuthenticateResponse> serviceResponse = serviceClient.post(targetHostUri + "/tokens", jaxbRequest, MediaType.APPLICATION_XML_TYPE);

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
                    break;
            }
        }

        return adminToken;
    }
}
