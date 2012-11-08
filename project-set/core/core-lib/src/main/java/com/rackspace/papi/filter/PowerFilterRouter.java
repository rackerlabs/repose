package com.rackspace.papi.filter;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import javax.servlet.ServletException;

/**
 *
 * @author ddaley
 */
public interface PowerFilterRouter {

    void route(MutableHttpServletRequest servletRequest, MutableHttpServletResponse servletResponse) throws IOException, ServletException, URISyntaxException;
    
}
