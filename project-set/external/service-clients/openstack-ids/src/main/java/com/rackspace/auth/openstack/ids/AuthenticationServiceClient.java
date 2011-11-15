package com.rackspace.auth.openstack.ids;

import com.rackspace.auth.openstack.ids.cache.EhcacheWrapper;
import net.sf.ehcache.CacheManager;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;

import java.util.Calendar;

/**
 * @author fran
 */
public class AuthenticationServiceClient {

    private final String targetHostUri;
    private final GenericServiceClient serviceClient;
    private final ResponseUnmarshaller responseUnmarshaller;
    private final CacheManager cacheManager;
    private final EhcacheWrapper ehcacheWrapper;

    public AuthenticationServiceClient(String targetHostUri, String username, String password) {
        this.responseUnmarshaller = new ResponseUnmarshaller();
        this.serviceClient = new GenericServiceClient(username, password);
        this.targetHostUri = targetHostUri;
        this.cacheManager = new CacheManager();
        this.ehcacheWrapper = new EhcacheWrapper(cacheManager);
    }

    public CachableTokenInfo validateToken(String tenant, String userToken, String adminUsername, String adminPassword) {
        CachableTokenInfo token = (CachableTokenInfo) ehcacheWrapper.get(tenant);

        if (token == null) {
            final ServiceClientResponse<AuthenticateResponse> serviceResponse = serviceClient.get(targetHostUri + "/tokens/" + userToken, getAdminToken(adminUsername, adminPassword));
            final int response = serviceResponse.getStatusCode();
            AuthenticateResponse authenticateResponse = null;

            switch (response) {
                case 200:
                    authenticateResponse = responseUnmarshaller.unmarshall(serviceResponse.getData(), AuthenticateResponse.class);
                        token = new CachableTokenInfo(authenticateResponse);
                        final Long expireTtl = authenticateResponse.getToken().getExpires().toGregorianCalendar().getTimeInMillis() - Calendar.getInstance().getTimeInMillis();

                        ehcacheWrapper.put(tenant, token, expireTtl.intValue());
            }
        }

        return token;
    }

    private String getAdminToken(String username, String password) {
        String adminToken = (String) ehcacheWrapper.get(username+password);

        if (adminToken == null) {
            final ServiceClientResponse<AuthenticateResponse> serviceResponse = serviceClient.getAdminToken(targetHostUri + "/tokens", username, password);
            final int response = serviceResponse.getStatusCode();
            AuthenticateResponse authenticateResponse = null;

            switch (response) {
                case 200:
                    authenticateResponse = responseUnmarshaller.unmarshall(serviceResponse.getData(), AuthenticateResponse.class);
                    adminToken = authenticateResponse.getToken().getId();

                    final Long expireTtl = authenticateResponse.getToken().getExpires().toGregorianCalendar().getTimeInMillis() - Calendar.getInstance().getTimeInMillis();

                    ehcacheWrapper.put(username+password, adminToken, expireTtl.intValue());
            }
        }

        return adminToken;
    }
}
