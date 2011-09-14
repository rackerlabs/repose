package com.rackspace.cloud.valve.jetty;

import com.rackspace.cloud.valve.filter.ProxyPowerFilter;
import com.rackspace.cloud.valve.jetty.servlet.ProxyServlet;
import com.rackspace.papi.filter.PowerFilter;
import com.rackspace.papi.service.context.PowerApiContextManager;
import com.rackspace.papi.servlet.InitParameter;
import org.apache.commons.httpclient.URI;
import java.util.EnumSet;
import javax.servlet.DispatcherType;
import org.apache.commons.httpclient.URIException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;

import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zinic
 */
public class ProxyApp {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyApp.class);
    private static final String DEFAULT_CFG_DIR = "/etc/powerapi";

    private final Server server;
    private final ServletContextHandler rootContext;

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            new ProxyApp(new Integer(args[0])).server.start();
        } else {
            System.out.println("Expecting single argument representing the proxy port.");
            new ProxyApp(8088).server.start();
        }
    }

    public ProxyApp(int port) throws Exception {
        server = new Server(port);
        server.setSendServerVersion(false);
        rootContext = new ServletContextHandler(server, "/");
        buildServerContext();

        server.start();

        LOG.info("Server started");
    }

    private void buildServerContext() {
        try {
            rootContext.addEventListener(PowerApiContextManager.class.newInstance());
        } catch (InstantiationException e) {
          throw new PowerAppException("Unable to instantiate PowerApiContextManager", e);
        } catch (IllegalAccessException e) {
          throw new PowerAppException("Unable to instantiate PowerApiContextManager", e);
        }
        
        rootContext.getInitParams().put(InitParameter.POWER_API_CONFIG_DIR.getParameterName(), DEFAULT_CFG_DIR);
        rootContext.addFilter(new FilterHolder(ProxyPowerFilter.class), "/*", EnumSet.allOf(DispatcherType.class));
        rootContext.addServlet(new ServletHolder(new ProxyServlet()), "/*");
    }
}
