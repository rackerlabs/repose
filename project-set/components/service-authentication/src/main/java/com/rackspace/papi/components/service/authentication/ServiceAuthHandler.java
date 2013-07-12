package com.rackspace.papi.components.service.authentication;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;

/**
 * Handles the client request & origin response for the service-authentication filter.  On request this adds the basic
 * authentication header, on response, it looks for NOT_IMPLEMENTED or FORBIDDEN status codes related to basic authentication
 * and returns 500.
 */
public class ServiceAuthHandler extends AbstractFilterLogicHandler {

    public static final String AUTH_HEADER = "Authorization";
    private String credentials;
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ServiceAuthHandler.class);

    public ServiceAuthHandler(String credentials) {
        this.credentials = credentials;

    }

    @Override
    public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {

        final FilterDirector filterDirector = new FilterDirectorImpl();
        filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE);

        if(!StringUtilities.isBlank(credentials)) {
            filterDirector.requestHeaderManager().putHeader(AUTH_HEADER, credentials);
        }
        return filterDirector;
    }

    @Override
    public FilterDirector handleResponse(HttpServletRequest request, ReadableHttpServletResponse response) {

        FilterDirectorImpl myDirector = new FilterDirectorImpl();
        final String wwwAuthenticateHeader = response.getHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString());

        myDirector.setResponseStatus(HttpStatusCode.fromInt(response.getStatus()));
        myDirector.setFilterAction(FilterAction.RETURN);

        switch (HttpStatusCode.fromInt(response.getStatus())) {
            case NOT_IMPLEMENTED:
                //If the service does not support delegation, return a 500
                LOG.warn("Origin service has not implemented delegation");
                myDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);

                //Remove the WWW-Authenticate header if present
                if (!StringUtilities.isBlank(wwwAuthenticateHeader)) {
                    myDirector.responseHeaderManager().removeHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString());
                }
                break;

            case FORBIDDEN:
                //If the WWW-Authenticate header is not present or it is not set to delegated then relay a 500
                LOG.warn("Failed to authentication against the Origin Service");
                if (StringUtilities.isBlank(wwwAuthenticateHeader) || !wwwAuthenticateHeader.equalsIgnoreCase("delegated")) {
                    myDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
                } else {
                    //Remove the header
                    myDirector.responseHeaderManager().removeHeader(CommonHttpHeader.WWW_AUTHENTICATE.toString());

                }

                break;
        }

        return myDirector;
    }
}
