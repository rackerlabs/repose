package org.openrepose.rnxp.servlet.context;

import com.rackspace.papi.commons.util.http.PowerApiHeader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.openrepose.rnxp.http.proxy.StreamController;
import org.openrepose.rnxp.servlet.http.LiveHttpServletRequest;

/**
 *
 * @author zinic
 */
public class ExternalRoutableRequestDispatcher implements RequestDispatcher {

    @Override
    public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        final LiveHttpServletRequest httpRequest = (LiveHttpServletRequest) request;
        
        // So you want to go somewhere with this request? Okay, where to, boss?
        final Enumeration<String> possibleRoutes = httpRequest.getHeaders(PowerApiHeader.ROUTE_DESTINATION.headerKey());

        if (possibleRoutes.hasMoreElements()) {
            // TODO:Implement - Use quality parameter for routing
            final URL destination = asURL(possibleRoutes.nextElement());
            
            final StreamController streamController = httpRequest.getStreamController();
            streamController.engageRemote(destination.getHost(), httpPort(destination));
            streamController.commitRequest(httpRequest);
        }
        
        throw new IllegalArgumentException("Request opting to forward must forward to a routable destination");
    }
    
    private static int httpPort(URL url) {
        final int urlPort = url.getPort();
        
        return  urlPort == -1 ? 80 : urlPort;
    }
    
    private URL asURL(String url) throws ServletException {
        try {
            return new URL(url);
        } catch(MalformedURLException exception) {
            throw new ServletException("Destination route URL is invalid. URL: " + url, exception);
        }
    }

    @Override
    public void include(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        throw new UnsupportedOperationException("Includes are not supported with this request dispatcher");
    }
}
