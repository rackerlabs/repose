package com.rackspace.papi.service.datastore.impl.redundant.notification.in;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.impl.redundant.UpdateListener;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.channels.ClosedByInterruptException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadedUpdateListener implements Runnable, UpdateListener {

    private static final Logger LOG = LoggerFactory.getLogger(ThreadedUpdateListener.class);
    private static final int SOCKET_TIMEOUT = 1000;
    private boolean done;
    private final ServerSocket socket;
    private final Datastore datastore;
    private final List<ChildThread> children;

    public ThreadedUpdateListener(Datastore datastore) throws IOException {
        socket = new ServerSocket(0);
        socket.setSoTimeout(SOCKET_TIMEOUT);
        done = false;
        this.datastore = datastore;
        this.children = new ArrayList<ChildThread>();
    }

    public String getAddress() {
        return socket.getInetAddress().getHostAddress();
    }

    public int getPort() {
        return socket.getLocalPort();
    }

    private static class ChildThread {

        private final Thread thread;
        private final ClientUpdateProcessorThread processor;

        ChildThread(Thread thread, ClientUpdateProcessorThread processor) {
            this.thread = thread;
            this.processor = processor;

        }

        public Thread getThread() {
            return thread;
        }

        public ClientUpdateProcessorThread getProcessor() {
            return processor;
        }
    }

    @Override
    public void run() {
        while (!done) {
            try {
                Socket request = socket.accept();
                if (request == null) {
                    continue;
                }

                ClientUpdateProcessorThread client = new ClientUpdateProcessorThread(request, datastore);
                Thread thread = new Thread(client);
                thread.start();
                children.add(new ChildThread(thread, client));

            } catch (SocketTimeoutException ex) {
                // ignore
            } catch (ClosedByInterruptException ex) {
                LOG.warn("Thread Interrupted");
            } catch (IOException ex) {
                LOG.error("Unable to deserialize update message", ex);
            }
        }

        for (ChildThread child : children) {
            child.getProcessor().done();
            if (child.getThread().isAlive()) {
                child.getThread().interrupt();
            }
        }

        LOG.info("Exiting update listener thread");
    }

    public void done() {
        done = true;
    }
}
