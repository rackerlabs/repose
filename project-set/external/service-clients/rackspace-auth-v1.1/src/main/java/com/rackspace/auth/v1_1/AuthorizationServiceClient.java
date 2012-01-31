package com.rackspace.auth.v1_1;

import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspacecloud.docs.auth.api.v1.Endpoint;
import com.rackspacecloud.docs.auth.api.v1.Service;
import com.rackspacecloud.docs.auth.api.v1.ServiceCatalog;

/**
 * @author fran
 */
public class AuthorizationServiceClient {
    private final String targetHostUri;
    private final ServiceClient serviceClient;
    private final ResponseUnmarshaller responseUnmarshaller;

    public AuthorizationServiceClient(String targetHostUri, String username, String password) {
        this.targetHostUri = targetHostUri;
        this.serviceClient = new ServiceClient(username, password);
        this.responseUnmarshaller = new ResponseUnmarshaller();
    }
    
    public ServiceCatalog getServiceCatalogForUser(String user) throws AuthServiceException {
        final ServiceClientResponse getServiceCatalogMethod = serviceClient.get(targetHostUri + "/users/" + user + "/serviceCatalog");
        final int response = getServiceCatalogMethod.getStatusCode();
        ServiceCatalog catalog = null;

        switch (response) {
            case 200:
                catalog = responseUnmarshaller.unmarshall(getServiceCatalogMethod.getData(), ServiceCatalog.class);
        }

        return catalog;
    }

    public AuthorizationResponse authorizeUser(String user, String requestedUri) throws AuthServiceException {
        final ServiceClientResponse getServiceCatalogMethod = serviceClient.get(targetHostUri + "/users/" + user + "/serviceCatalog", null);
        final int response = getServiceCatalogMethod.getStatusCode();
        AuthorizationResponse authorizationResponse = null;

        switch (response) {
            case 200:
                final ServiceCatalog catalog = responseUnmarshaller.unmarshall(getServiceCatalogMethod.getData(), ServiceCatalog.class);

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
}
