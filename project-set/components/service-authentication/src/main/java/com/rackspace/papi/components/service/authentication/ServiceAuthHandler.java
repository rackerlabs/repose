package com.rackspace.papi.components.service.authentication;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import java.io.UnsupportedEncodingException;
import org.slf4j.Logger;
import org.apache.commons.codec.binary.Base64;
import javax.servlet.http.HttpServletRequest;

public class ServiceAuthHandler extends AbstractFilterLogicHandler {

    private final ServiceAuthenticationConfig config;
    public static final String AUTH_HEADER = "Authorization";
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ServiceAuthHandler.class);
    public ServiceAuthHandler(ServiceAuthenticationConfig config) {
        this.config = config;

    }

    @Override
    public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {

        final FilterDirector filterDirector = new FilterDirectorImpl();
        filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE);
        
        if(config.getCredentials() == null){
            LOG.warn("Unable to locate config file for Reverse Proxy Basic Auth Filter");
            return filterDirector;
        }

        StringBuilder preHash = new StringBuilder(config.getCredentials().getUsername());
        StringBuilder postHash = new StringBuilder("Basic ");
        preHash.append(":").append(config.getCredentials().getPassword());
        try{
            postHash.append(Base64.encodeBase64String(preHash.toString().getBytes("UTF-8")).trim());
        }catch(UnsupportedEncodingException e){
            LOG.error("Failed to update basic credentials. Reason: " + e.getMessage(), e);
        }
        
        filterDirector.requestHeaderManager().putHeader(AUTH_HEADER, postHash.toString());
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
