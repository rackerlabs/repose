package com.rackspace.cloud.valve.jetty;

import com.rackspace.cloud.valve.jetty.servlet.BasicResponseServlet;
import com.rackspace.papi.components.clientauth.ClientAuthenticationFilter;
import com.rackspace.papi.servlet.InitParameter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.web.context.ContextLoaderListener;

import javax.servlet.DispatcherType;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author zinic
 */
public class FilterRequestControlTest {

    private final Server jettyServerReference;
    private final ServletContextHandler rootContext;

    public FilterRequestControlTest(int port) throws Exception {
        jettyServerReference = new Server(port);
        jettyServerReference.setSendServerVersion(false);

        rootContext = buildRootContext(jettyServerReference);

        final EnumSet<DispatcherType> dispatchers = EnumSet.allOf(DispatcherType.class);

        rootContext.addFilter(ClientAuthenticationFilter.class, "/*", dispatchers);
        rootContext.addServlet(new ServletHolder(new BasicResponseServlet()), "/*");
    }

    private ServletContextHandler buildRootContext(Server serverReference) {
        final Map<String, String> initParams = new HashMap<String, String>();
        initParams.put(InitParameter.POWER_API_CONFIG_DIR.getParameterName(), "/etc/powerapi");
        

        final ServletContextHandler servletContext = new ServletContextHandler(serverReference, "/", ServletContextHandler.NO_SESSIONS);

        servletContext.getInitParams().putAll(initParams);

        servletContext.addEventListener(new ContextLoaderListener());

        return servletContext;
    }

    public void go() throws Exception {
        jettyServerReference.start();
    }

    public static void main(String[] args) throws Exception {
        new FilterRequestControlTest(8088).go();
    }
}
