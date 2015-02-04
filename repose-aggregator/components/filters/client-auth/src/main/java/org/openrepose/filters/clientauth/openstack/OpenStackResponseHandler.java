package org.openrepose.filters.clientauth.openstack;

import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletResponse;

public class OpenStackResponseHandler {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(OpenStackResponseHandler.class);
    private final ReadableHttpServletResponse response;
    private final String wwwAuthenticate;
    private static final String DELEGATED = "Delegated";

    public OpenStackResponseHandler(ReadableHttpServletResponse response, String wwwAuthenticate) {
        this.response = response;
        this.wwwAuthenticate = wwwAuthenticate;
    }

    public FilterDirector handle() {
        LOG.debug("Handling service response in OpenStack auth filter.");
        FilterDirector myDirector = new FilterDirectorImpl();
        LOG.debug("Incoming response code is " + response.getStatus());
        myDirector.setResponseStatusCode(response.getStatus());

        /// The WWW Authenticate header can be used to communicate to the client
        // (since we are a proxy) how to correctly authenticate itself
        final String wwwAuthenticateHeader = response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString());

        switch (response.getStatus()) {
            // NOTE: We should only mutate the WWW-Authenticate header on a
            // 401 (unauthorized) or 403 (forbidden) response from the origin service
            case HttpServletResponse.SC_UNAUTHORIZED:
            case HttpServletResponse.SC_FORBIDDEN:
                myDirector = updateHttpResponse(myDirector, wwwAuthenticateHeader);
                break;
            case HttpServletResponse.SC_NOT_IMPLEMENTED:
                if (!StringUtilities.isBlank(wwwAuthenticateHeader) && wwwAuthenticateHeader.contains(DELEGATED)) {
                    myDirector.setResponseStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    LOG.error("Repose authentication component is configured as delegetable but origin service does not support delegated mode.");
                } else {
                    myDirector.setResponseStatusCode(HttpServletResponse.SC_NOT_IMPLEMENTED);
                }
                break;
                
            default:
                break;
        }

        LOG.debug("Outgoing response code is " + myDirector.getResponseStatusCode());
        return myDirector;
    }

    private FilterDirector updateHttpResponse(FilterDirector director, String wwwAuthenticateHeader) {
        // If in the case that the origin service supports delegated authentication
        // we should then communicate to the client how to authenticate with us
        if (!StringUtilities.isBlank(wwwAuthenticateHeader) && wwwAuthenticateHeader.contains(DELEGATED)) {
            director.responseHeaderManager().putHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString(), wwwAuthenticate);
        } else {
            // In the case where authentication has failed and we did not receive
            // a delegated WWW-Authenticate header, this means that our own authentication
            // with the origin service has failed and must then be communicated as
            // a 500 (internal server error) to the client
            director.setResponseStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        return director;
    }
}
