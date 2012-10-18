package com.rackspace.papi.service.datastore.impl.redundant.subscriptions;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.io.ObjectSerializer;
import com.rackspace.papi.service.datastore.impl.redundant.Notifier;
import com.rackspace.papi.service.datastore.impl.redundant.RedundantDatastore;
import com.rackspace.papi.service.datastore.impl.redundant.SubscriptionListener;
import com.rackspace.papi.service.datastore.impl.redundant.data.Message;
import com.rackspace.papi.service.datastore.impl.redundant.data.Operation;
import com.rackspace.papi.service.datastore.impl.redundant.data.Subscriber;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MulticastSubscriptionListener implements Runnable, SubscriptionListener {

    private static final Logger LOG = LoggerFactory.getLogger(MulticastSubscriptionListener.class);
    private static final int BUFFER_SIZE = 1024 * 8;
    private static final int SOCKET_TIMEOUT = 1000;
    private final MulticastSocket socket;
    private final InetAddress group;
    private final byte[] buffer;
    private boolean done;
    private String tcpHost;
    private int tcpPort;
    private final int groupPort;
    private final Notifier notifier;
    private final RedundantDatastore datastore;
    private final boolean synched;
    private final UUID id;
    private final NetworkInterface net;
    private final InetSocketAddress socketAddress;

    public MulticastSubscriptionListener(RedundantDatastore datastore, Notifier notifier, String multicastAddress, int multicastPort) throws UnknownHostException, IOException {
        this(datastore, notifier, "*", multicastAddress, multicastPort);
    }

    public MulticastSubscriptionListener(RedundantDatastore datastore, Notifier notifier, String nic, String multicastAddress, int multicastPort) throws UnknownHostException, IOException {
        this.group = InetAddress.getByName(multicastAddress);
        LOG.info(group.toString() + " is multicast " + group.isMulticastAddress());
        this.groupPort = multicastPort;
        this.socket = new MulticastSocket(multicastPort);
        this.buffer = new byte[BUFFER_SIZE];
        this.notifier = notifier;
        this.datastore = datastore;
        this.id = UUID.randomUUID();
        this.done = false;
        this.synched = false;
        this.socket.setTimeToLive(5);
        this.net = getInterface(nic);
        
        if (net != null) {
            this.socketAddress = new InetSocketAddress(multicastAddress, multicastPort);
            this.socket.joinGroup(socketAddress, net);
        } else {
            this.socketAddress = null;
            this.socket.joinGroup(group);
        }

        socket.setSoTimeout(SOCKET_TIMEOUT);
        socket.setReceiveBufferSize(BUFFER_SIZE);
    }

    private NetworkInterface getInterface(String name) throws SocketException {
        if (StringUtilities.isBlank(name) || "*".equals(name)) {
            return null;
        }

        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();

        while (nets.hasMoreElements()) {
            NetworkInterface net = nets.nextElement();
            if (net.getName().equals(name)) {
                LOG.info(net.getDisplayName() + " supports multicast " + net.supportsMulticast());
                return net;
            }
        }

        listAvailableNics();

        throw new RuntimeException("Cannot find network interface by name: " + name);
    }

    private void listAvailableNics() throws SocketException {
        NetworkInterface nic = null;
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();

        while (nets.hasMoreElements()) {
            NetworkInterface net = nets.nextElement();
            LOG.info(net.getName() + " supports multicast " + net.supportsMulticast());
        }

    }

    public void announce(Message message) {
        try {
            byte[] messageData = ObjectSerializer.instance().writeObject(message);
            DatagramPacket messagePacket = new DatagramPacket(messageData, messageData.length, group, groupPort);
            socket.send(messagePacket);
        } catch (IOException ex) {
            LOG.error("Unable to send multicast announcement", ex);
        }
    }

    @Override
    public void join(String host, int port) {
        this.tcpHost = host;
        this.tcpPort = port;
        try {
            byte[] subscriberData = ObjectSerializer.instance().writeObject(new Subscriber(host, port));
            announce(new Message(Operation.JOINING, id.toString(), subscriberData, 0));
        } catch (IOException ex) {
            LOG.error("Unable to serialize subscriber information", ex);
        }
    }

    public void sendSyncRequest(String targetId) {
        try {
            Subscriber subscriber = new Subscriber(tcpHost, tcpPort);
            byte[] subscriberData = ObjectSerializer.instance().writeObject(subscriber);
            announce(new Message(Operation.SYNC, targetId, id.toString(), subscriberData, 0));
        } catch (IOException ex) {
            LOG.error("Unable to serialize subscriber information", ex);
        }

    }

    public void listening() {
        try {
            Subscriber subscriber = new Subscriber(tcpHost, tcpPort);
            byte[] subscriberData = ObjectSerializer.instance().writeObject(subscriber);
            announce(new Message(Operation.LISTENING, id.toString(), subscriberData, 0));
        } catch (IOException ex) {
            LOG.error("Unable to serialize subscriber information", ex);
        }

    }

    public void leaving() {
        try {
            Subscriber subscriber = new Subscriber(tcpHost, tcpPort);
            byte[] subscriberData = ObjectSerializer.instance().writeObject(subscriber);
            announce(new Message(Operation.LEAVING, id.toString(), subscriberData, 0));
        } catch (IOException ex) {
            LOG.error("Unable to serialize subscriber information", ex);
        }

    }

    private void receivedAnnouncement(String key, String targetId, Operation operation, Subscriber subscriber) {
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
                }
                notifier.addSubscriber(subscriber);
                break;
            case LEAVING:
                notifier.removeSubscriber(subscriber);
                break;
        }

        LOG.debug("Received " + operation.name() + " Request From: " + subscriber.getHost() + ":" + subscriber.getPort());
    }

    @Override
    public void run() {
        while (!done) {
            try {
                DatagramPacket recv = new DatagramPacket(buffer, BUFFER_SIZE);
                socket.receive(recv);
                Message message = (Message) ObjectSerializer.instance().readObject(recv.getData());
                receivedAnnouncement(message.getKey(), message.getTargetId(), message.getOperation(), (Subscriber) ObjectSerializer.instance().readObject(message.getData()));
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
        try {
            if (this.net != null) {
                socket.leaveGroup(socketAddress, net);
            } else {
                socket.leaveGroup(group);
            }
            socket.close();
        } catch (IOException ex) {
            LOG.error("Unable to leave multicast group", ex);
        }
    }

    @Override
    public void unsubscribe() {
        done = true;
    }
}
