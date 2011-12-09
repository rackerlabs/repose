package com.rackspace.auth.v1_1;

import com.rackspacecloud.docs.auth.api.v1.FullToken;
import com.rackspacecloud.docs.auth.api.v1.GroupsList;
import net.sf.ehcache.CacheManager;

import java.util.Calendar;
import java.util.GregorianCalendar;

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

    public AuthenticationServiceClient(String targetHostUri, ResponseUnmarshaller responseUnmarshaller, ServiceClient serviceClient,
                                       CacheManager cacheManager, AuthenticationCache authenticationCache) {
        this.targetHostUri = targetHostUri;
        this.responseUnmarshaller = responseUnmarshaller;
        this.serviceClient = serviceClient;
        this.cacheManager = cacheManager;
        this.authenticationCache = authenticationCache;
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
                    final int expireTtl = getTtl(tokenResponse.getExpires().toGregorianCalendar());

                    authenticationCache.cacheUserAuthToken(account.getUsername(), expireTtl, tokenResponse.getId());
                    validated = true;                    
            }
        }

        return validated;
    }

    /**
     * TODO: Could there be problems with this if the Auth Service sends us an expiration date that is in a different
     * time zone than the box on which repose is running.
     *
     */
    public static int getTtl(GregorianCalendar expirationDate) {
        final Long expireTtl = expirationDate.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
        return expireTtl.intValue();
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
