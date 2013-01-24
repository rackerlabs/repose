package com.rackspace.papi.components.clientauth.rackspace.v1_1;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import org.slf4j.Logger;

public class RackspaceResponseHandler {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RackspaceResponseHandler.class);

   private final ReadableHttpServletResponse response;
   private final String wwwAuthenticate;
   private static final String DELEGATED = "Delegated";

   public RackspaceResponseHandler(ReadableHttpServletResponse response, String wwwAuthenticate) {
      this.response = response;
      this.wwwAuthenticate = wwwAuthenticate;
   }

   public FilterDirector handle() {
        FilterDirector myDirector = new FilterDirectorImpl();
        myDirector.setResponseStatusCode(response.getStatus());
        /// The WWW Authenticate header can be used to communicate to the client
        // (since we are a proxy) how to correctly authenticate itself
        final String wwwAuthenticateHeader = response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString());

        switch (myDirector.getResponseStatus()) {
            // NOTE: We should only mutate the WWW-Authenticate header on a
            // 401 (unauthorized) or 403 (forbidden) response from the origin service
            case UNAUTHORIZED:
            case FORBIDDEN:
                updateHttpResponse(myDirector, wwwAuthenticateHeader);
                break;
            case NOT_IMPLEMENTED:
                if ((!StringUtilities.isBlank(wwwAuthenticateHeader) && wwwAuthenticateHeader.contains("Delegated"))) {
                    myDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
                    LOG.error("Repose authentication component is configured as delegetable but origin service does not support delegated mode.");
                } else {
                    myDirector.setResponseStatus(HttpStatusCode.NOT_IMPLEMENTED);
                }
                break;
                
            default:
                break;
        }

        return myDirector;
    }

    private void updateHttpResponse(FilterDirector director, String wwwAuthenticateHeader) {
        // If in the case that the origin service supports delegated authentication
        // we should then communicate to the client how to authenticate with us
        if (!StringUtilities.isBlank(wwwAuthenticateHeader) && wwwAuthenticateHeader.contains(DELEGATED)) {
            director.responseHeaderManager().putHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString(), wwwAuthenticate);
        } else {
            // In the case where authentication has failed and we did not receive
            // a delegated WWW-Authenticate header, this means that our own authentication
            // with the origin service has failed and must then be communicated as
            // a 500 (internal server error) to the client
            director.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
        }
    }
}
