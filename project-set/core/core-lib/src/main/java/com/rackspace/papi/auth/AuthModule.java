package com.rackspace.papi.auth;

import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.FilterLogicHandler;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author jhopper
 */
public interface AuthModule extends FilterLogicHandler {

    FilterDirector authenticate(HttpServletRequest request);

    /**
     * Any given authentication module may communicate a domain specific authentication
     * realm as a string to be assigned to the WWW-Authenticate header. This method
     * is used to acquire that realm string.
     * 
     * @return 
     */
    String getWWWAuthenticateHeaderContents();
}
