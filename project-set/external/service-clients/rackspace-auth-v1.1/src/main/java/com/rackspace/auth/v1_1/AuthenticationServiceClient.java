package com.rackspace.auth.v1_1;

import com.rackspacecloud.docs.auth.api.v1.FullToken;
import com.rackspacecloud.docs.auth.api.v1.GroupsList;
import net.sf.ehcache.CacheManager;

import java.util.Calendar;

/**
 *
 * @author jhopper
 */
public class AuthenticationServiceClient {
    private final String targetHostUri;
    private final ServiceClient serviceClient;
    private final ResponseUnmarshaller responseUnmarshaller;
    private final CacheManager cacheManager;
    private final AuthenticationCache authenticationCache;

    public AuthenticationServiceClient(String targetHostUri, String username, String password) {
        this.responseUnmarshaller = new ResponseUnmarshaller();
        this.serviceClient = new ServiceClient(username, password);
        this.targetHostUri = targetHostUri;
        this.cacheManager = new CacheManager();
        this.authenticationCache = new AuthenticationCache(cacheManager);
    }

    public boolean validateToken(Account account, String token) {
        boolean validated = authenticationCache.tokenIsCached(account.getUsername(), token);

        if (!validated) {

            final ServiceClientResponse<FullToken> validateTokenMethod = serviceClient.get(targetHostUri + "/token/" + token, 
                    "belongsTo", account.getUsername(),
                    "type", account.getType());
            final int response = validateTokenMethod.getStatusCode();
            switch (response) {
                case 200:
                    final FullToken tokenResponse = responseUnmarshaller.unmarshall(validateTokenMethod.getData(), FullToken.class);
                    final Long expireTtl = tokenResponse.getExpires().toGregorianCalendar().getTimeInMillis() - Calendar.getInstance().getTimeInMillis();

                    authenticationCache.cacheUserAuthToken(account.getUsername(), expireTtl.intValue(), tokenResponse.getId());
                    validated = true;                    
            }
        }

        return validated;
    }

    public GroupsList getGroups(String userId) {
        final ServiceClientResponse<GroupsList> serviceResponse = serviceClient.get(targetHostUri + "/users/" + userId + "/groups");
        final int response = serviceResponse.getStatusCode();
        GroupsList groups = null;

        switch (response) {
            case 200:
                groups = responseUnmarshaller.unmarshall(serviceResponse.getData(), GroupsList.class);
        }

        return groups;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (cacheManager != null) {
                cacheManager.shutdown();
            }   
        } finally {
            super.finalize();
        }
    }
}
