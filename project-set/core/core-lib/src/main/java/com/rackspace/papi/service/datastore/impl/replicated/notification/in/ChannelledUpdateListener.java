package com.rackspace.papi.service.datastore.impl.replicated.notification.in;

import com.rackspace.papi.commons.util.io.ObjectSerializer;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.impl.replicated.UpdateListener;
import com.rackspace.papi.service.datastore.impl.replicated.data.Message;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelledUpdateListener implements Runnable, UpdateListener {

    private static final Logger LOG = LoggerFactory.getLogger(ChannelledUpdateListener.class);
    private static final int BUFFER_SIZE = 1024;
    private static final int TIMEOUT = 1000;
    private boolean done;
    private final ServerSocket socket;
    private final Datastore datastore;
    private final ServerSocketChannel channel;
    private final Selector selector;

    public ChannelledUpdateListener(Datastore datastore, String address) throws IOException {
        channel = ServerSocketChannel.open();
        selector = Selector.open();
        socket = channel.socket();
        socket.bind(new InetSocketAddress(address, 0));
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_ACCEPT);
        done = false;
        this.datastore = datastore;
    }

    @Override
    public String getAddress() {
        return socket.getInetAddress().getHostAddress();
    }

    @Override
    public int getPort() {
        return socket.getLocalPort();
    }

    private void acceptConnection(SelectionKey next) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) next.channel();
        SocketChannel connection = serverChannel.accept();
        LOG.info("Accepted connection from: " + connection);
        connection.configureBlocking(false);
        SelectionKey register = connection.register(selector, SelectionKey.OP_READ);
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        register.attach(buffer);
    }

    private void putMessage(Message message) {
        for (Message.KeyValue value : message.getValues()) {
            if (message.getTtl() > 0) {
                datastore.put(value.getKey(), value.getData(), value.getTtl(), TimeUnit.SECONDS, false);
            } else {
                datastore.put(value.getKey(), value.getData(), false);
            }
            //LOG.debug(socket.getLocalPort() + " Received: " + value.getKey() + ": " + new String(value.getData()));
        }
    }

    private void removeMessage(Message message) {
        datastore.remove(message.getKey(), false);
        //LOG.debug(socket.getLocalPort() + " Received: " + message.getKey() + ": " + new String(message.getData()));
    }
    
    private ByteArrayOutputStream readData(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        ByteArrayOutputStream os = new ByteArrayOutputStream(BUFFER_SIZE);
        int read;
        while ((read = client.read(buffer)) > 0) {
            buffer.flip();
            os.write(buffer.array(), 0, read);
            buffer.clear();
        }

        if (read == -1) {
            key.channel().close();
            key.cancel();
        }
        
        return os;
    }

    private void readMessage(SelectionKey key) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream os = readData(key);
        byte[] data = os.toByteArray();
        //LOG.info("Read " + data.length + " bytes of data");

        if (data.length == 0) {
            return;
        }

        ByteArrayInputStream is = new ByteArrayInputStream(data);
        while (is.available() > 0) {
            Message message = (Message) ObjectSerializer.instance().readObject(is);
            switch (message.getOperation()) {
                case PUT:
                    putMessage(message);
                    break;
                case REMOVE:
                    removeMessage(message);
                    break;
            }
        }
    }

    @Override
    public void run() {
        while (!done) {
            try {
                int select = selector.select(TIMEOUT);
                if (select == 0) {
                    continue;
                }

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> it = selectedKeys.iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();

                    if (key.isAcceptable()) {
                        acceptConnection(key);
                    } else if (key.isReadable()) {
                        readMessage(key);
                    }
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
            channel.close();
        } catch (IOException ex) {
            LOG.warn("Error closing channel", ex);
        }
        
        LOG.info("Exiting update listener thread");
    }

    @Override
    public void done() {
        done = true;
    }
}
