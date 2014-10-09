package com.rackspace.cloud.valve.server;

import com.rackspace.cloud.valve.jetty.ValveControllerServerBuilder;
import org.openrepose.commons.config.ConfigurationResourceException;
import org.openrepose.commons.config.parser.ConfigurationParserFactory;
import org.openrepose.commons.config.parser.jaxb.JaxbConfigurationParser;
import org.openrepose.commons.config.resource.impl.BufferedURLConfigurationResource;
import com.rackspace.papi.container.config.ContainerConfiguration;
import com.rackspace.papi.container.config.SslConfiguration;
import com.rackspace.papi.domain.Port;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PowerApiValveServerControl {
    private static final Logger LOG = LoggerFactory.getLogger(PowerApiValveServerControl.class);
    private final List<Port> ports = new ArrayList<Port>();
    private final Server serverInstance;

    public PowerApiValveServerControl(
            Integer httpPort,
            Integer httpsPort,
            String configDirectory,
            Boolean insecure) {

        if (httpPort != null) {
            ports.add(new Port("http", httpPort));
        }

        if (httpsPort != null) {
            ports.add(new Port("https", httpsPort));
        }

        Server deferServer = null;
        try {
            validateSsl(httpsPort, configDirectory);

            deferServer = new ValveControllerServerBuilder(
                    configDirectory,
                    insecure != null ? insecure : false)
                    .newServer();
            deferServer.setStopAtShutdown(true);
        } catch (MalformedURLException murle) {
            LOG.error("Server controller not built -- unable to validate SSL settings: " + murle.getMessage(), murle);
        }
        serverInstance = deferServer;
    }

    public void startPowerApiValve() {
        if (serverInstance != null) {
            if (!serverInstance.isStopped()) {
                throw new IllegalStateException("A serverInstance already exists and has not stopped.");
            }

            try {
                serverInstance.start();

                Runtime.getRuntime().addShutdownHook(new Thread() {
                    @Override
                    public void run() {
                        stopServer(serverInstance);
                    }
                });

                for (Port p : ports) {
                    if (p != null) {
                        LOG.info("Repose Controller Server launched");
                    }
                }
            } catch (Exception e) {
                LOG.error("Unable to build controller server: " + e.getMessage(), e);
                stopServer(serverInstance);
            }
        }
    }

    public void stopPowerApiValve() {
        stopServer(serverInstance);
    }

    private static void stopServer(Server server) {
        if (server != null && !server.isStopped()) {
            LOG.info("Stopping Repose...");
            try {
                server.stop();
                LOG.info("Repose has been stopped");
            } catch (Exception e) {
                LOG.error("Unable to stop controller server: " + e.getMessage(), e);
            }
        }
    }

    private SslConfiguration validateSsl(Integer httpsPort, String configDirectory) throws MalformedURLException {
        SslConfiguration sslConfiguration = null;

        if (httpsPort != null) {
            sslConfiguration = readSslConfiguration(configDirectory);

            if (sslConfiguration == null) {
                throw new ConfigurationResourceException(
                        "Repose is configured to run on https but the ssl configuration is not in container.cfg.xml.");
            }

            if (sslConfiguration.getKeystoreFilename() == null) {
                throw new ConfigurationResourceException(
                        "Repose is configured to run on https but the ssl keystore filename is not in container.cfg.xml.");
            }

            if (sslConfiguration.getKeystorePassword() == null) {
                throw new ConfigurationResourceException(
                        "Repose is configured to run on https but the ssl keystore password is not in container.cfg.xml.");
            }

            if (sslConfiguration.getKeyPassword() == null) {
                throw new ConfigurationResourceException(
                        "Repose is configured to run on https but the ssl key password is not in container.cfg.xml.");
            }
        }

        return sslConfiguration;
    }

    private SslConfiguration readSslConfiguration(String cfgRoot) throws MalformedURLException {
        final URL configurationLocation = new URL("file://" + cfgRoot + File.separator + "container.cfg.xml");
        final JaxbConfigurationParser<ContainerConfiguration> containerConfigParser =
                ConfigurationParserFactory.getXmlConfigurationParser(ContainerConfiguration.class, null);
        final ContainerConfiguration cfg =
                containerConfigParser.read(new BufferedURLConfigurationResource(configurationLocation));

        if (cfg != null && cfg.getDeploymentConfig() != null) {
            return cfg.getDeploymentConfig().getSslConfiguration();
        }

        throw new ConfigurationResourceException(
                "Container configuration is not valid. Please check your configuration.");
    }
}
