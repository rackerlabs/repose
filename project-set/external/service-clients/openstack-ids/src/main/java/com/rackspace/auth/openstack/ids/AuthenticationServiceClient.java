package com.rackspace.auth.openstack.ids;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;

import java.util.ArrayList;
import java.util.List;

import org.openstack.docs.identity.api.v2.Endpoint;
import org.openstack.docs.identity.api.v2.EndpointList;
import org.openstack.docs.identity.api.v2.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author fran
 */
public class AuthenticationServiceClient implements OpenStackAuthenticationService {

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationServiceClient.class);
    private final String targetHostUri;
    private final GenericServiceClient serviceClient;
    private final OpenStackCoreResponseUnmarshaller openStackCoreResponseUnmarshaller;
    private final OpenStackGroupsResponseUnmarshaller openStackGroupsResponseUnmarshaller;
    private AdminToken currentAdminToken;
    private final long groupsTtl;

    public AuthenticationServiceClient(String targetHostUri, String username, String password, long groupsTtl) {
        this.openStackCoreResponseUnmarshaller = new OpenStackCoreResponseUnmarshaller();
        this.openStackGroupsResponseUnmarshaller = new OpenStackGroupsResponseUnmarshaller();
        this.serviceClient = new GenericServiceClient(username, password);
        this.targetHostUri = targetHostUri;
        this.groupsTtl = groupsTtl;
    }

    @Override
    public CachableUserInfo validateToken(String tenant, String userToken) {
        CachableUserInfo token = null;

        final ServiceClientResponse<AuthenticateResponse> serviceResponse = serviceClient.get(targetHostUri + "/tokens/" + userToken, getAdminToken(), "belongsTo", tenant);

        switch (serviceResponse.getStatusCode()) {
            case 200:
                final AuthenticateResponse authenticateResponse = openStackCoreResponseUnmarshaller.unmarshall(serviceResponse.getData(), AuthenticateResponse.class);
                token = new CachableUserInfo(tenant, authenticateResponse);
                break;

            case 404: // User's token is bad
                LOG.warn("Unable to validate token for tenant.  Invalid token. " + serviceResponse.getStatusCode());
                break;

            case 401: // Admin token is bad most likely
                LOG.warn("Unable to validate token for tenant.  Has the admin token expired? " + serviceResponse.getStatusCode());
                break;

            case 500: // Internal server error from auth
                LOG.warn("Internal server error from auth. " + serviceResponse.getStatusCode());
                break;
        }

        return token;
    }

    @Override
    public List<Endpoint> getEndpointsForToken(String userToken) {
        final ServiceClientResponse<EndpointList> endpointListResponse = serviceClient.get(targetHostUri + "/tokens/" + userToken + "/endpoints", getAdminToken());
        List<Endpoint> endpointList = new ArrayList<Endpoint>();

        switch (endpointListResponse.getStatusCode()) {
            case 200:
                final EndpointList unmarshalledEndpoints = openStackCoreResponseUnmarshaller.unmarshall(endpointListResponse.getData(), EndpointList.class);

                if (unmarshalledEndpoints != null) {
                    endpointList = unmarshalledEndpoints.getEndpoint();
                }

                break;

            default:
                LOG.warn("Unable to get endpoints for token: " + endpointListResponse.getStatusCode());
                break;
        }

        return endpointList;
    }

    @Override
    public CachableGroupInfo getGroups(String userId) {
        CachableGroupInfo cachableGroupInfo = null;

        final ServiceClientResponse<Groups> serviceResponse = serviceClient.get(targetHostUri + "/users/" + userId + "/RAX-KSGRP", getAdminToken());


        switch (serviceResponse.getStatusCode()) {
            case 200:
                final Groups groups = openStackGroupsResponseUnmarshaller.unmarshall(serviceResponse.getData(), Groups.class);
                cachableGroupInfo = new CachableGroupInfo(userId, groups, groupsTtl);
                break;
            default:
                LOG.warn("Unable to get groups for user id: " + userId + ".  Received Status Code: " + serviceResponse.getStatusCode());
                break;

        }

        return cachableGroupInfo;
    }

    private synchronized String getAdminToken() {
        String adminToken = currentAdminToken != null && currentAdminToken.isValid() ? currentAdminToken.getToken() : null;

        if (adminToken == null) {
            final ServiceClientResponse<AuthenticateResponse> serviceResponse = serviceClient.getAdminToken(targetHostUri + "/tokens");

            switch (serviceResponse.getStatusCode()) {
                case 200:
                    final AuthenticateResponse authenticateResponse = openStackCoreResponseUnmarshaller.unmarshall(serviceResponse.getData(), AuthenticateResponse.class);

                    Token token = authenticateResponse.getToken();
                    currentAdminToken = new AdminToken(token.getId(), token.getExpires().toGregorianCalendar());
                    adminToken = currentAdminToken.getToken();
                    break;

                default:
                    LOG.error("Unable to get admin token.  Verify admin credentials. " + serviceResponse.getStatusCode());
                    break;
            }
        }

        return adminToken;
    }
}
