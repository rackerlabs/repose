package com.rackspace.papi.test;

import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class GlassFishMonitorThread extends Thread {

    private final GlassFish glassfish;
    private final int stopPort;
    private static final String MONITOR_NAME = "GlassFish_MONITOR";
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


    public GlassFishMonitorThread(GlassFish glassfish, int stopPort) throws MonitorException {
        this.glassfish = glassfish;
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
            glassfish.stop();
            accept.close();
            socket.close();
        } catch (IOException ex) {
            System.err.println("Unble to stop glassfish instance " + ex.getMessage());
        } catch (GlassFishException e) {
            System.err.println("Unable to stop glassfish instance: " + e.getMessage());
        }
    }
}
