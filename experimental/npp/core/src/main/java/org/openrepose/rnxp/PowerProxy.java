package org.openrepose.rnxp;

import com.rackspace.papi.filter.PowerFilter;
import com.rackspace.papi.service.context.PowerApiContextManager;
import com.rackspace.papi.servlet.InitParameter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;
import org.openrepose.rnxp.servlet.context.NXPServletContext;
import org.openrepose.rnxp.servlet.context.filter.NXPFilterConfig;
import org.openrepose.rnxp.servlet.filter.EmptyFilterChain;
import org.openrepose.rnxp.servlet.http.LiveHttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread safe PowerFilter container class.
 * 
 * @author zinic
 */
public class PowerProxy {

    private static final Logger LOG = LoggerFactory.getLogger(PowerProxy.class);
    private final Map<String, Object> containerAttributes;
    private final PowerApiContextManager ctxManager;
    private final PowerFilter powerFilterInstance;

    public PowerProxy() {
        containerAttributes = new HashMap<String, Object>();
        ctxManager = new PowerApiContextManager();
        powerFilterInstance = new PowerFilter();
    }

    public void init() {
        final ServletContext sc = new NXPServletContext(containerAttributes);
        sc.setInitParameter(InitParameter.POWER_API_CONFIG_DIR.getParameterName(), "/etc/powerapi");

        final Map<String, String> powerFilterParams = new HashMap<String, String>();
        powerFilterParams.put("show-me-papi", "true");

        final FilterConfig fc = new NXPFilterConfig("power-filter", sc, powerFilterParams);

        try {
            ctxManager.contextInitialized(new ServletContextEvent(sc));
            powerFilterInstance.init(fc);
        } catch (ServletException servletException) {
            LOG.error(servletException.getMessage(), servletException);
        }
    }

    public void handleRequest(LiveHttpServletRequest request) throws ServletException {
        try {
            powerFilterInstance.doFilter(request, null, EmptyFilterChain.getInstance());
        } catch (IOException ioe) {
            LOG.error(ioe.getMessage(), ioe);
        } catch (ServletException se) {
            LOG.error(se.getMessage(), se);
        }
    }
}
