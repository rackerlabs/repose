package org.openrepose.powerfilter;

import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;
import org.openrepose.core.systemmodel.Node;
import org.openrepose.core.systemmodel.ReposeCluster;
import org.openrepose.powerfilter.PowerFilterChainException;

import java.io.IOException;
import java.net.URISyntaxException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 *
 * @author ddaley
 */
public interface PowerFilterRouter {

    void route(MutableHttpServletRequest servletRequest, MutableHttpServletResponse servletResponse) throws IOException, ServletException, URISyntaxException;
    void initialize(ReposeCluster domain, Node localhost, ServletContext context, String defaultDst) throws PowerFilterChainException;
    
}
