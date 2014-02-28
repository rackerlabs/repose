package com.rackspace.papi.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ContainerMonitorThread extends Thread {

    private final static Logger LOG = LoggerFactory.getLogger(ContainerMonitorThread.class);
    private final ReposeContainer container;
    private final int stopPort;
    private static final String MONITOR_NAME = "Repose_Container_MONITOR";
    private static final String LOCAL_ADDRESS = "127.0.0.1";
    private ServerSocket socket;

    public static class MonitorException extends RuntimeException {
        public MonitorException(String message) {
            super(message);
        }

        public MonitorException(Throwable cause) {
            super(cause);
        }
    }

    public ContainerMonitorThread(ReposeContainer container, int stopPort) throws MonitorException {
        this.container = container;
        this.stopPort = stopPort;
        setDaemon(true);
        setName(MONITOR_NAME);
        try {
            socket = new ServerSocket(stopPort, 1, InetAddress.getByName(LOCAL_ADDRESS));
        } catch (Exception e) {
            throw new MonitorException(e);
        }
    }

    @Override
    public void run() {

        Socket accept;

        try {
            accept = socket.accept();
            BufferedReader reader = new BufferedReader(new InputStreamReader(accept.getInputStream()));
            reader.readLine();
            container.stopRepose();
            accept.close();
            socket.close();
        } catch (IOException ex) {
            LOG.trace("Unable to stop repose container", ex);
            System.err.println("Unble to stop repose container instance " + ex.getMessage());
        } catch (Exception e) {
            LOG.trace("Unable to stop repose container", e);
            System.err.println("Unable to stop repose container instance: " + e.getMessage());
        }
    }
}
