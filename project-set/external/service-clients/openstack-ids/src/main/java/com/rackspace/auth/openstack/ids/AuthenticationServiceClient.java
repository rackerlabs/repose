package com.rackspace.auth.openstack.ids;

import com.rackspace.auth.openstack.ids.cache.EhcacheWrapper;
import net.sf.ehcache.CacheManager;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;

import java.util.Calendar;

/**
 * @author fran
 */
public class AuthenticationServiceClient implements OpenStackAuthenticationService {

    private final String targetHostUri;
    private final GenericServiceClient serviceClient;
    private final ResponseUnmarshaller responseUnmarshaller;
    private final CacheManager cacheManager;
    private final EhcacheWrapper ehcacheWrapper;
    private final String adminTokenCacheKey;

    public AuthenticationServiceClient(String targetHostUri, String username, String password) {
        this.responseUnmarshaller = new ResponseUnmarshaller();
        this.serviceClient = new GenericServiceClient(username, password);
        this.targetHostUri = targetHostUri;
        this.cacheManager = new CacheManager();
        this.ehcacheWrapper = new EhcacheWrapper(cacheManager);
        
        adminTokenCacheKey = "Admin_Token:" + (username + password).hashCode();
    }

    @Override
    public CachableTokenInfo validateToken(String tenant, String userToken) {
        CachableTokenInfo token = (CachableTokenInfo) ehcacheWrapper.get(tenant);

        if (token == null) {
            final ServiceClientResponse<AuthenticateResponse> serviceResponse = serviceClient.get(targetHostUri + "/tokens/" + userToken, getAdminToken());
            final int response = serviceResponse.getStatusCode();

            switch (response) {
                case 200:
                    final AuthenticateResponse authenticateResponse = responseUnmarshaller.unmarshall(serviceResponse.getData(), AuthenticateResponse.class);
                    final Long expireTtl = authenticateResponse.getToken().getExpires().toGregorianCalendar().getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
                    ehcacheWrapper.put(tenant, token, expireTtl.intValue());

                    token = new CachableTokenInfo(authenticateResponse);
            }
        }

        return token;
    }

    private String getAdminToken() {
        String adminToken = (String) ehcacheWrapper.get(adminTokenCacheKey);

        if (adminToken == null) {
            final ServiceClientResponse<AuthenticateResponse> serviceResponse = serviceClient.getAdminToken(targetHostUri + "/tokens");
            final int response = serviceResponse.getStatusCode();

            switch (response) {
                case 200:
                    final AuthenticateResponse authenticateResponse = responseUnmarshaller.unmarshall(serviceResponse.getData(), AuthenticateResponse.class);
                    adminToken = authenticateResponse.getToken().getId();

                    final Long expireTtl = authenticateResponse.getToken().getExpires().toGregorianCalendar().getTimeInMillis() - Calendar.getInstance().getTimeInMillis();

                    ehcacheWrapper.put(adminTokenCacheKey, adminToken, expireTtl.intValue());
            }
        }

        return adminToken;
    }
}
