package org.openrepose.core.valve.jetty;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.openrepose.core.container.config.SslConfiguration;
import org.openrepose.core.domain.Port;
import org.openrepose.core.domain.ReposeInstanceInfo;
import org.openrepose.core.domain.ServicePorts;
import org.openrepose.core.services.context.impl.PowerApiContextManager;
import org.openrepose.core.servlet.InitParameter;
import org.openrepose.core.valve.jetty.servlet.ProxyServlet;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class ValveJettyServerBuilder {

    private final ServicePorts ports = new ServicePorts();
    private String configurationPathAndFile = "";
    private final SslConfiguration sslConfiguration;
    private final boolean insecure;
    private final String clusterId;
    private final String nodeId;

    public ValveJettyServerBuilder(String configurationPathAndFile, List<Port> ports, SslConfiguration sslConfiguration, boolean insecure,
            String clusterId, String nodeId) {
        this.ports.addAll(ports);
        this.configurationPathAndFile = configurationPathAndFile;
        this.sslConfiguration = sslConfiguration;
        this.insecure = insecure;
        this.clusterId = clusterId;
        this.nodeId = nodeId;
    }

    public Server newServer() {

        Server server = new Server();
        List<Connector> connectors = new ArrayList<Connector>();

        for (Port p : ports) {
            if ("http".equalsIgnoreCase(p.getProtocol())) {
                connectors.add(createHttpConnector(server, p));
            } else if ("https".equalsIgnoreCase(p.getProtocol())) {
                connectors.add(createHttpsConnector(server, p));
            }
        }

        server.setConnectors(connectors.toArray(new Connector[connectors.size()]));

        final ServletContextHandler rootContext = buildRootContext(server);
        //TODO: this will all be replaced in the valve work story thingy
        final FilterHolder powerFilterHolder = new FilterHolder(Filter.class);
        final ServletHolder valveServer = new ServletHolder(new ProxyServlet());

        rootContext.addFilter(powerFilterHolder, "/*", EnumSet.allOf(DispatcherType.class));
        rootContext.addServlet(valveServer, "/*");

        server.setHandler(rootContext);

        return server;
    }

    private Connector createHttpConnector(Server server, Port port) {
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port.getPort());

        return connector;
    }

    private Connector createHttpsConnector(Server server, Port port) {
        SslContextFactory cf = new SslContextFactory();

        cf.setKeyStorePath(configurationPathAndFile + File.separator + sslConfiguration.getKeystoreFilename());
        cf.setKeyStorePassword(sslConfiguration.getKeystorePassword());
        cf.setKeyManagerPassword(sslConfiguration.getKeyPassword());

        ServerConnector sslConnector = new ServerConnector(server, cf);
        sslConnector.setPort(port.getPort());
        return sslConnector;
    }

    private ServletContextHandler buildRootContext(Server serverReference) {
        final ServletContextHandler servletContext = new ServletContextHandler(serverReference, "/");
        servletContext.getInitParams().put(InitParameter.POWER_API_CONFIG_DIR.getParameterName(), configurationPathAndFile);
        servletContext.getInitParams().put(InitParameter.INSECURE.getParameterName(), Boolean.toString(insecure));
        servletContext.getInitParams().put(InitParameter.REPOSE_CLUSTER_ID.getParameterName(), clusterId);
        servletContext.getInitParams().put(InitParameter.REPOSE_NODE_ID.getParameterName(), nodeId);
        
        ReposeInstanceInfo instanceInfo = new ReposeInstanceInfo(clusterId, nodeId);
        try {
            PowerApiContextManager contextManager = PowerApiContextManager.class.newInstance();
            //THis is a spring property thing instead
            //contextManager.setPorts(ports,instanceInfo);
            servletContext.addEventListener(contextManager);
        } catch (InstantiationException e) {
            throw new PowerAppException("Unable to instantiate PowerApiContextManager", e);
        } catch (IllegalAccessException e) {
            throw new PowerAppException("Unable to instantiate PowerApiContextManager", e);
        }

        return servletContext;
    }
}