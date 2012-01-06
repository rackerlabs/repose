package com.rackspace.cloud.valve.jetty;

import com.rackspace.cloud.valve.jetty.servlet.ProxyServlet;
import com.rackspace.papi.filter.PowerFilter;
import com.rackspace.papi.service.context.PowerApiContextManager;
import com.rackspace.papi.servlet.InitParameter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.DispatcherType;
import java.util.EnumSet;

public class ValveJettyServerBuilder {    
    private final int portNumber;
    private String configurationPathAndFile = "";

    public ValveJettyServerBuilder(int portNumber, String configurationPathAndFile) {
        this.portNumber = portNumber;
        this.configurationPathAndFile = configurationPathAndFile;
    }

    public Server newServer() {
        final Server jettyServerReference = new Server(portNumber);
        final ServletContextHandler rootContext = buildRootContext(jettyServerReference);

        final ServletHolder valveServer = new ServletHolder(new ProxyServlet());

        rootContext.addFilter(new FilterHolder(PowerFilter.class), "/*", EnumSet.allOf(DispatcherType.class));
        rootContext.addServlet(valveServer, "/*");

        return jettyServerReference;
    }

    private ServletContextHandler buildRootContext(Server serverReference) {
        final ServletContextHandler servletContext = new ServletContextHandler(serverReference, "/");
        servletContext.getInitParams().put(InitParameter.POWER_API_CONFIG_DIR.getParameterName(), configurationPathAndFile);
        
        try {
            servletContext.addEventListener(PowerApiContextManager.class.newInstance());
        } catch (InstantiationException e) {
            throw new PowerAppException("Unable to instantiate PowerApiContextManager", e);
        } catch (IllegalAccessException e) {
            throw new PowerAppException("Unable to instantiate PowerApiContextManager", e);
        }

        return servletContext;
    }   
}