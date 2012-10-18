package com.rackspace.papi.service.datastore.impl.redundant.notification.in;

import com.rackspace.papi.commons.util.io.ObjectSerializer;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.impl.redundant.data.Message;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientUpdateProcessorThread implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ClientUpdateProcessorThread.class);
    private static final int BUFFER_SIZE = 1000;
    private final Socket request;
    private final Datastore datastore;
    private final byte[] buffer;
    private boolean done;

    public ClientUpdateProcessorThread(Socket request, Datastore datastore) {
        this.request = request;
        this.datastore = datastore;
        this.buffer = new byte[BUFFER_SIZE];
        this.done = false;
    }

    @Override
    public void run() {
        byte[] ok = new byte[]{1};
        ByteArrayOutputStream os = new ByteArrayOutputStream(BUFFER_SIZE);

        while (!done) {
            try {
                try {
                    InputStream is = request.getInputStream();
                    if (is.available() == 0) {
                        continue;
                    }

                    /*
                     int read;
                     os.reset();
                     while (is.available() > 0 && (read = is.read(buffer)) != -1) {
                     os.write(buffer, 0, read);
                     }

                     request.getOutputStream().write(ok);
                     request.getOutputStream().flush();

                     byte[] data = os.toByteArray();
                     if (data.length > 0) {

                     Message message = (Message) ObjectSerializer.instance().readObject(data);
                     */
                    Message message = (Message) ObjectSerializer.instance().readObject(is);
                    switch (message.getOperation()) {
                        case PUT:
                            for (Message.KeyValue value : message.getValues()) {
                                if (message.getTtl() > 0) {
                                    datastore.put(value.getKey(), value.getData(), value.getTtl(), TimeUnit.SECONDS, false);
                                } else {
                                    datastore.put(value.getKey(), value.getData(), false);
                                }
                                LOG.debug("Received: " + value.getKey());
                            }
                            break;
                        case REMOVE:
                            datastore.remove(message.getKey(), false);
                            LOG.debug("Received: " + message.getKey());
                            break;
                    }
                    //}

                } finally {
                    //request.close();
                }
            } catch (SocketTimeoutException ex) {
                // ignore
            } catch (ClosedByInterruptException ex) {
                LOG.warn("Thread Interrupted");
            } catch (ClassNotFoundException ex) {
                LOG.error("Unable to deserialize update message", ex);
            } catch (IOException ex) {
                LOG.error("Unable to deserialize update message", ex);
            }
        }

        try {
            LOG.info("Exiting client processor thread");
            request.close();
        } catch (IOException ex) {
            LOG.warn("Error closing client connection", ex);
        }

    }

    public void done() {
        this.done = true;
    }
}
