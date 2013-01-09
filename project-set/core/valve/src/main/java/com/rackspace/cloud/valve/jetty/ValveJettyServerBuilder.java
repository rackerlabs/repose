package com.rackspace.cloud.valve.jetty;

import com.rackspace.cloud.valve.jetty.servlet.ProxyServlet;
import com.rackspace.papi.container.config.SslConfiguration;
import com.rackspace.papi.domain.Port;
import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.domain.ServicePorts;
import com.rackspace.papi.filter.ValvePowerFilter;
import com.rackspace.papi.service.context.impl.PowerApiContextManager;
import com.rackspace.papi.servlet.InitParameter;
import java.io.File;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.DispatcherType;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.eclipse.jetty.http.ssl.SslContextFactory;

public class ValveJettyServerBuilder {

    private final ServicePorts ports = new ServicePorts();
    private String configurationPathAndFile = "";
    private final SslConfiguration sslConfiguration;
    private final String connectionFramework;
    private final boolean insecure;
    private final String clusterId;
    private final String nodeId;

    public ValveJettyServerBuilder(String configurationPathAndFile, List<Port> ports, SslConfiguration sslConfiguration, String connectionFramework, boolean insecure,
            String clusterId, String nodeId) {
        this.ports.addAll(ports);
        this.configurationPathAndFile = configurationPathAndFile;
        this.sslConfiguration = sslConfiguration;
        this.connectionFramework = connectionFramework;
        this.insecure = insecure;
        this.clusterId = clusterId;
        this.nodeId = nodeId;
    }

    public Server newServer() {

        Server server = new Server();
        List<Connector> connectors = new ArrayList<Connector>();

        for (Port p : ports) {
            if ("http".equalsIgnoreCase(p.getProtocol())) {
                connectors.add(createHttpConnector(p));
            } else if ("https".equalsIgnoreCase(p.getProtocol())) {
                connectors.add(createHttpsConnector(p));
            }
        }

        server.setConnectors(connectors.toArray(new Connector[connectors.size()]));

        final ServletContextHandler rootContext = buildRootContext(server);
        final FilterHolder powerFilterHolder = new FilterHolder(ValvePowerFilter.class);
        final ServletHolder valveServer = new ServletHolder(new ProxyServlet());

        rootContext.addFilter(powerFilterHolder, "/*", EnumSet.allOf(DispatcherType.class));
        rootContext.addServlet(valveServer, "/*");

        server.setHandler(rootContext);

        return server;
    }

    private Connector createHttpConnector(Port port) {
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(port.getPort());

        return connector;
    }

    private Connector createHttpsConnector(Port port) {
        SslSelectChannelConnector sslConnector = new SslSelectChannelConnector();

        sslConnector.setPort(port.getPort());
        SslContextFactory cf = sslConnector.getSslContextFactory();

        cf.setKeyStore(configurationPathAndFile + File.separator + sslConfiguration.getKeystoreFilename());
        cf.setKeyStorePassword(sslConfiguration.getKeystorePassword());
        cf.setKeyManagerPassword(sslConfiguration.getKeyPassword());

        return sslConnector;
    }

    private ServletContextHandler buildRootContext(Server serverReference) {
        final ServletContextHandler servletContext = new ServletContextHandler(serverReference, "/");
        servletContext.getInitParams().put(InitParameter.POWER_API_CONFIG_DIR.getParameterName(), configurationPathAndFile);
        servletContext.getInitParams().put(InitParameter.CONNECTION_FRAMEWORK.getParameterName(), connectionFramework);
        servletContext.getInitParams().put(InitParameter.INSECURE.getParameterName(), Boolean.toString(insecure));
        servletContext.getInitParams().put(InitParameter.REPOSE_CLUSTER_ID.getParameterName(), clusterId);
        servletContext.getInitParams().put(InitParameter.REPOSE_NODE_ID.getParameterName(), nodeId);
        
        ReposeInstanceInfo instanceInfo = new ReposeInstanceInfo(clusterId, nodeId);
        try {
            PowerApiContextManager contextManager = PowerApiContextManager.class.newInstance();
            contextManager.setPorts(ports,instanceInfo);
            servletContext.addEventListener(contextManager);
        } catch (InstantiationException e) {
            throw new PowerAppException("Unable to instantiate PowerApiContextManager", e);
        } catch (IllegalAccessException e) {
            throw new PowerAppException("Unable to instantiate PowerApiContextManager", e);
        }

        return servletContext;
    }
}