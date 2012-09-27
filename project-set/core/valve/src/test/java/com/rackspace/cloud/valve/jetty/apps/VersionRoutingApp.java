package com.rackspace.cloud.valve.jetty.apps;

import com.rackspace.cloud.valve.jetty.servlet.VersionOneServlet;
import com.rackspace.cloud.valve.jetty.servlet.VersionTwoServlet;
import com.rackspace.papi.components.versioning.VersioningFilter;
import com.rackspace.papi.service.context.impl.PowerApiContextManager;
import com.rackspace.papi.servlet.InitParameter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.springframework.web.context.ContextLoaderListener;

import javax.servlet.DispatcherType;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class VersionRoutingApp {

    private final Server jettyServerReference;
    private final ServletContextHandler rootContext;

    public VersionRoutingApp(int port) throws Exception {
        jettyServerReference = new Server(port);

        rootContext = buildRootContext(jettyServerReference);

        final EnumSet<DispatcherType> dispatchers = EnumSet.of(DispatcherType.REQUEST);
        
        rootContext.addEventListener(new PowerApiContextManager());

        rootContext.addFilter(VersioningFilter.class, "/*", dispatchers);
        
        rootContext.addServlet(VersionOneServlet.class, "/v1/*");
        rootContext.addServlet(VersionTwoServlet.class, "/*");
    }

    private ServletContextHandler buildRootContext(Server serverReference) {
        final Map<String, String> initParams = new HashMap<String, String>();
        initParams.put(InitParameter.POWER_API_CONFIG_DIR.getParameterName(), "/home/zinic/installed/etc/powerapi");
        

        final ServletContextHandler servletContext = new ServletContextHandler(serverReference, "/");

        servletContext.getInitParams().putAll(initParams);

        servletContext.addEventListener(new ContextLoaderListener());

        return servletContext;
    }

    public void go() throws Exception {
        jettyServerReference.start();
    }

    public static void main(String[] args) throws Exception {
        new VersionRoutingApp(8080).go();
    }
}