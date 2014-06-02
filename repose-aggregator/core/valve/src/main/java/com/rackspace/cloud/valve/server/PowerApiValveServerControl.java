package com.rackspace.cloud.valve.server;

import com.rackspace.cloud.valve.jetty.ValveControllerServerBuilder;
import com.rackspace.papi.commons.config.ConfigurationResourceException;
import com.rackspace.papi.commons.config.parser.ConfigurationParserFactory;
import com.rackspace.papi.commons.config.parser.jaxb.JaxbConfigurationParser;
import com.rackspace.papi.commons.config.resource.impl.BufferedURLConfigurationResource;
import com.rackspace.papi.commons.util.io.charset.CharacterSets;
import com.rackspace.papi.container.config.ContainerConfiguration;
import com.rackspace.papi.container.config.SslConfiguration;
import com.rackspace.papi.domain.Port;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PowerApiValveServerControl {

    private static final Logger LOG = LoggerFactory.getLogger(PowerApiValveServerControl.class);
    private static final String LOCALHOST_IP = "127.0.0.1";
    private final List<Port> ports = new ArrayList<Port>();

    private Integer httpPort;
    private Integer httpsPort;
    private Integer stopPort;
    private String configDirectory;
    private Boolean insecure;

    private Server serverInstance = null;

    public PowerApiValveServerControl(
            Integer httpPort,
            Integer httpsPort,
            Integer stopPort,
            String configDirectory,
            Boolean insecure) {

        this.httpPort = httpPort;
        this.httpsPort = httpsPort;
        this.stopPort = stopPort;
        this.configDirectory = configDirectory;
        this.insecure = insecure;

        if (httpPort != null) {
            ports.add(new Port("http", httpPort));
        }

        if (httpsPort != null) {
            ports.add(new Port("https", httpsPort));
        }
    }

    private SslConfiguration validateSsl() throws MalformedURLException {
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

    public void startPowerApiValve() {
        Server serverInstance = null;
        if (serverInstance != null &&
                !serverInstance.isStopped()) {

            throw new IllegalStateException("A serverInstance already exists and has not stopped.");
        }

        try {
            validateSsl();
            serverInstance = new ValveControllerServerBuilder(
                    configDirectory,
                    insecure != null ? insecure : false)
                    .newServer();
            serverInstance.setStopAtShutdown(true);
            serverInstance.start();
            final Thread monitor = new MonitorThread(serverInstance, stopPort, LOCALHOST_IP);
            monitor.start();

            for (Port p : ports) {
                if (p != null) {
                    LOG.info("Repose Controller Server launched");
                }
            }
        } catch (Exception e) {
            LOG.error("Unable to build controller server: " + e.getMessage(), e);
            if (serverInstance != null) {
                try {
                    serverInstance.stop();
                } catch (Exception ex) {
                    LOG.error("Unable to stop controller server: " + ex.getMessage(), ex);
                }
            }
        }
    }

    public void stopPowerApiValve() {

        if (serverInstance != null) {

            try {
                serverInstance.stop();
            } catch (Exception e) {
                LOG.error("An error occurred while trying to stop the Repose Controller. Reason: {}", e.getMessage(), e);
            }
        }

        try {

            final Socket s = new Socket(InetAddress.getByName(LOCALHOST_IP), stopPort);
            final OutputStream out = s.getOutputStream();

            LOG.info("Sending Repose stop request");

            out.write(("\r\n").getBytes(CharacterSets.UTF_8));
            out.flush();
            s.close();
        } catch (IOException ioex) {
            LOG.error("An error occurred while attempting to stop Repose Controller. Reason: " + ioex.getMessage(), ioex);
        }
    }
}
