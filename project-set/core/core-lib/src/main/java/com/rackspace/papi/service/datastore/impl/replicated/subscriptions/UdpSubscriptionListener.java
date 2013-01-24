package com.rackspace.papi.service.datastore.impl.replicated.subscriptions;

import com.rackspace.papi.commons.util.io.ObjectSerializer;
import com.rackspace.papi.service.datastore.impl.replicated.Notifier;
import com.rackspace.papi.service.datastore.impl.replicated.ReplicatedDatastore;
import com.rackspace.papi.service.datastore.impl.replicated.SubscriptionListener;
import com.rackspace.papi.service.datastore.impl.replicated.data.Message;
import com.rackspace.papi.service.datastore.impl.replicated.data.Operation;
import com.rackspace.papi.service.datastore.impl.replicated.data.Subscriber;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UdpSubscriptionListener implements SubscriptionListener, Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(UdpSubscriptionListener.class);
    private static final String UNABLE_TO_SERIALIZE = "Unable to serialize subscriber information";
    private static final int BUFFER_SIZE = 1024 * 8;
    private static final int SOCKET_TIMEOUT = 1000;
    private static final int PING_TIME = 5000;
    private final DatagramSocket socket;
    private final byte[] buffer;
    private boolean done;
    private String tcpHost;
    private int tcpPort;
    private final Notifier notifier;
    private final ReplicatedDatastore datastore;
    private boolean synched;
    private final UUID id;
    private final InetSocketAddress socketAddress;
    private final int udpPort;

    public UdpSubscriptionListener(ReplicatedDatastore datastore, Notifier notifier, String udpAddress, int udpPort) throws IOException {
        this.udpPort = udpPort;
        this.socketAddress = new InetSocketAddress(udpAddress, udpPort);
        this.socket = new DatagramSocket(socketAddress);
        this.buffer = new byte[BUFFER_SIZE];
        this.notifier = notifier;
        this.datastore = datastore;
        this.id = UUID.randomUUID();
        this.done = false;
        this.synched = false;
        socket.setSoTimeout(SOCKET_TIMEOUT);
        socket.setReceiveBufferSize(BUFFER_SIZE);
    }

    DatagramSocket getSocket() {
        return socket;
    }

    void setTcpHost(String host) {
        this.tcpHost = host;
    }

    void setTcpPort(int port) {
        this.tcpPort = port;
    }

    public String getId() {
        return id.toString();
    }
   
    public void announce(Subscriber subscriber, Message message) {
        try {
            byte[] messageData = ObjectSerializer.instance().writeObject(message);
            announce(subscriber, messageData);
        } catch (IOException ex) {
            LOG.error("Unable to send multicast announcement", ex);
        }
    }

    public void announce(Subscriber subscriber, byte[] messageData) {
        try {
            InetAddress address = subscriber.getAddress();
            if (address == null) {
                LOG.warn("Unable to determine address for host: " + subscriber.getHost());
                return;
            }
            DatagramPacket messagePacket = new DatagramPacket(messageData, messageData.length, address, subscriber.getUpdPort());
            socket.send(messagePacket);
        } catch (IOException ex) {
            LOG.error("Unable to send multicast announcement", ex);
        }
    }

    public void announce(Message message) {
        try {
            byte[] messageData = ObjectSerializer.instance().writeObject(message);
            for (Subscriber subscriber : notifier.getSubscribers()) {
                announce(subscriber, messageData);
            }
        } catch (IOException ex) {
            LOG.error("Unable to send multicast announcement", ex);
        }
    }

    @Override
    public void join(String host, int port) {
        this.tcpHost = host;
        this.tcpPort = port;
        try {
            byte[] subscriberData = ObjectSerializer.instance().writeObject(new Subscriber(host, port, this.socketAddress.getPort()));
            announce(new Message(Operation.JOINING, id.toString(), subscriberData, 0));
        } catch (IOException ex) {
            LOG.error(UNABLE_TO_SERIALIZE, ex);
        }
    }

    public void sendSyncRequest(String targetId) {
        try {
            Subscriber subscriber = new Subscriber(tcpHost, tcpPort, udpPort);
            byte[] subscriberData = ObjectSerializer.instance().writeObject(subscriber);
            announce(new Message(targetId, Operation.SYNC, id.toString(), subscriberData, 0));
        } catch (IOException ex) {
            LOG.error(UNABLE_TO_SERIALIZE, ex);
        }

    }

    public void listening() {
        try {
            Subscriber subscriber = new Subscriber(tcpHost, tcpPort, udpPort);
            byte[] subscriberData = ObjectSerializer.instance().writeObject(subscriber);
            announce(new Message(Operation.LISTENING, id.toString(), subscriberData, 0));
        } catch (IOException ex) {
            LOG.error(UNABLE_TO_SERIALIZE, ex);
        }

    }

    public long ping() {
        listening();
        return System.currentTimeMillis();
    }

    public void leaving() {
        try {
            Subscriber subscriber = new Subscriber(tcpHost, tcpPort, udpPort);
            byte[] subscriberData = ObjectSerializer.instance().writeObject(subscriber);
            announce(new Message(Operation.LEAVING, id.toString(), subscriberData, 0));
        } catch (IOException ex) {
            LOG.error(UNABLE_TO_SERIALIZE, ex);
        }

    }

    void receivedAnnouncement(String key, String targetId, Operation operation, Subscriber subscriber) {
        if (tcpHost.equalsIgnoreCase(subscriber.getHost()) && subscriber.getPort() == tcpPort) {
            return;
        }

        switch (operation) {
            case JOINING:
                notifier.addSubscriber(subscriber);
                listening();
                break;
            case SYNC:
                if (id.toString().equals(targetId)) {
                    try {
                        datastore.sync(subscriber);
                    } catch (IOException ex) {
                        LOG.error("Error synching with remote node", ex);
                    }
                }
                break;
            case LISTENING:
                if (!synched) {
                    sendSyncRequest(key);
                    synched = true;
                }
                notifier.addSubscriber(subscriber);
                break;
            case LEAVING:
                notifier.removeSubscriber(subscriber);
                break;
                
            default:
                LOG.warn("Unexpected operation: " + operation);
                break;
        }

        LOG.trace("Received " + operation.name() + " Request From: " + subscriber.getHost() + ":" + subscriber.getPort());
    }
    
    private boolean shouldPing(long time) {
        return System.currentTimeMillis() - time > PING_TIME;
    }

    @Override
    public void run() {
        long time = System.currentTimeMillis();
        while (!done) {
            try {
                if (shouldPing(time)) {
                    time = ping();
                }
                DatagramPacket recv = new DatagramPacket(buffer, BUFFER_SIZE);
                socket.receive(recv);
                Message message = (Message) ObjectSerializer.instance().readObject(recv.getData());
                receivedAnnouncement(message.getTargetId(), message.getKey(), message.getOperation(), (Subscriber) ObjectSerializer.instance().readObject(message.getData()));
            } catch (SocketTimeoutException ex) {
                // ignore
            } catch (ClassNotFoundException ex) {
                LOG.error("Unable to deserialize multicast message", ex);
            } catch (IOException ex) {
                LOG.error("Unable to deserialize multicast message", ex);
            }
        }

        leaving();

        LOG.info("Exiting subscription listener thread");
        socket.close();
    }

    @Override
    public void unsubscribe() {
        done = true;
    }
}
