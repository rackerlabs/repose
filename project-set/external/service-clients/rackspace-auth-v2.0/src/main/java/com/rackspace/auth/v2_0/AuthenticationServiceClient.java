package com.rackspace.auth.v2_0;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import net.sf.ehcache.CacheManager;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;

/**
 * @author fran
 */
public class AuthenticationServiceClient {

    private final String targetHostUri;
    private final GenericServiceClient serviceClient;
    private final ResponseUnmarshaller responseUnmarshaller;
    private final CacheManager cacheManager;
    private final AuthenticationCache authenticationCache;
    private String adminToken = null;

    public AuthenticationServiceClient(String targetHostUri, String username, String password) {
        this.responseUnmarshaller = new ResponseUnmarshaller();
        this.serviceClient = new GenericServiceClient(username, password);
        this.targetHostUri = targetHostUri;
        this.cacheManager = new CacheManager();
        this.authenticationCache = new AuthenticationCache(cacheManager);
        this.adminToken = getAdminToken(username, password);
    }

    private String getAdminToken(String username, String password) {
        final ServiceClientResponse<AuthenticateResponse> serviceResponse = serviceClient.getAdminToken(targetHostUri + "/tokens", username, password);
        final int response = serviceResponse.getStatusCode();
        AuthenticateResponse authenticateResponse = null;

        switch (response) {
            case 200:
                authenticateResponse = responseUnmarshaller.unmarshall(serviceResponse.getData(), AuthenticateResponse.class);
        }

        return authenticateResponse.getToken().getId();
    }

    public AuthenticateResponse validateToken(Account account, String userToken) {
        final ServiceClientResponse<AuthenticateResponse> serviceResponse = serviceClient.get(targetHostUri + "/tokens/" + userToken, adminToken);
        final int response = serviceResponse.getStatusCode();
        AuthenticateResponse authenticateResponse = null;

        switch (response) {
            case 200:
                authenticateResponse = responseUnmarshaller.unmarshall(serviceResponse.getData(), AuthenticateResponse.class);
        }

        return authenticateResponse;
    }    

    public Groups getGroups(String userId) {
        final ServiceClientResponse<Groups> serviceResponse = serviceClient.get(targetHostUri + "/users/" + userId + "/RAX-KSGRP", adminToken);
        final int response = serviceResponse.getStatusCode();
        Groups groups = null;

        switch (response) {
            case 200:
                groups = responseUnmarshaller.unmarshall(serviceResponse.getData(), Groups.class);
        }

        return groups;
    }
}
