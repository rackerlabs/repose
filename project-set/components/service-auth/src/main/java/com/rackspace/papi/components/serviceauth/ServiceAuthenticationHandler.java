package com.rackspace.papi.components.serviceauth;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterDirector;
import org.slf4j.Logger;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.components.serviceauth.config.Credentials;
import com.rackspace.papi.components.serviceauth.config.ServiceAuthConfig;
import com.rackspace.papi.filter.logic.AbstractFilterLogicHandler;

import java.io.UnsupportedEncodingException;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author jhopper
 */
public class ServiceAuthenticationHandler extends AbstractFilterLogicHandler {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ServiceAuthenticationHandler.class);
    private String base64EncodedCredentials;

    private final UpdateListener<ServiceAuthConfig> serviceAuthorizationConfigurationListener = new UpdateListener<ServiceAuthConfig>() {

        @Override
        public void configurationUpdated(ServiceAuthConfig modifiedConfig) {
            if (modifiedConfig.getHttpBasic() != null) {
                final Credentials creds = modifiedConfig.getHttpBasic().getCredentials();
                final String combinedCredentials = creds.getUsername() + ":" + creds.getPassword();

                try {
                    base64EncodedCredentials = "Basic " + new String(Base64.encodeBase64(combinedCredentials.getBytes("UTF-8")), "UTF-8");
                } catch (UnsupportedEncodingException uee) {
                    LOG.error("Failed to update basic credentials. Reason: " + uee.getMessage(), uee);
                }
            } else {
                LOG.error("Please check your configuration for service authentication. It appears to be malformed.");
            }
        }
    };

    public UpdateListener<ServiceAuthConfig> getServiceAuthorizationConfigurationListener() {
        return serviceAuthorizationConfigurationListener;
    }    

    public FilterDirector handleRequest(MutableHttpServletRequest request, MutableHttpServletResponse response) {
        request.addHeader(CommonHttpHeader.AUTHORIZATION.headerKey(), base64EncodedCredentials);

        return null;
    }

    public void handleResponse(MutableHttpServletRequest request, MutableHttpServletResponse response) {
        final String wwwAuthenticateHeader = response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.headerKey());

        switch (HttpStatusCode.fromInt(response.getStatus())) {
            case NOT_IMPLEMENTED:
                //If the service does not support delegation, return a 500
                response.setStatus(HttpStatusCode.INTERNAL_SERVER_ERROR.intValue());

                //Remove the WWW-Authenticate header if present
                if (!StringUtilities.isBlank(wwwAuthenticateHeader)) {
                    response.setHeader(CommonHttpHeader.WWW_AUTHENTICATE.headerKey(), null);
                }
                break;

            case FORBIDDEN:
                //If the WWW-Authenticate header is not present or it is not set to delegated then relay a 500
                if (StringUtilities.isBlank(wwwAuthenticateHeader) && !wwwAuthenticateHeader.equalsIgnoreCase("delegated")) {
                    response.setStatus(HttpStatusCode.INTERNAL_SERVER_ERROR.intValue());
                } else {
                    //Remove the header
                    response.setHeader(CommonHttpHeader.WWW_AUTHENTICATE.headerKey(), null);
                }

                break;
        }        
    }
}
