package com.rackspace.cloud.valve.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.io.OutputStream;

import com.rackspace.cloud.valve.jetty.ValveJettyServerBuilder;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PowerApiValveServerControl {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyApp.class);

    private Server serverInstance;
    private CommandLineArguments commandLineArgs;
    private final String LOCALHOST_IP = "127.0.0.1";

    public PowerApiValveServerControl(CommandLineArguments commandLineArgs) {
        this.commandLineArgs = commandLineArgs;
    }

   public void startPowerApiValve() {

        try {
            serverInstance = new ValveJettyServerBuilder(commandLineArgs.getPort(), commandLineArgs.getConfigDirectory()).newServer();
            serverInstance.setStopAtShutdown(true);
            serverInstance.start();
            Thread monitor = new MonitorThread(serverInstance, commandLineArgs.getStopPort(), LOCALHOST_IP);
            monitor.start();
            LOG.info("Power API Valve running and listening on port: " + Integer.toString(commandLineArgs.getPort()));
        } catch (Exception e) {
            LOG.error("Power API Valve could not be started: " + e.getMessage());
        }
    }

    public void stopPowerApiValve() {
        try {
            Socket s = new Socket(InetAddress.getByName(LOCALHOST_IP), commandLineArgs.getStopPort());
            OutputStream out = s.getOutputStream();
            System.out.println("Sending Power API Valve stop request");
            out.write(("\r\n").getBytes());
            out.flush();
            s.close();
        } catch (IOException ioex) {
            LOG.error("An error occured while attempting to stop Power API Valve: " + ioex.getMessage());
        }
    }
}
