package com.rackspace.papi.service.datastore.impl.replicated.notification.in;

import com.rackspace.papi.commons.util.io.ByteBufferInputStream;
import com.rackspace.papi.commons.util.io.ByteBufferOutputStream;
import com.rackspace.papi.commons.util.io.ObjectSerializer;
import com.rackspace.papi.commons.util.io.buffer.CyclicByteBuffer;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.impl.replicated.UpdateListener;
import com.rackspace.papi.service.datastore.impl.replicated.data.Message;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelledUpdateListener implements Runnable, UpdateListener {

    private static final Logger LOG = LoggerFactory.getLogger(ChannelledUpdateListener.class);
    private static final int BUFFER_SIZE = 1024;
    private static final int TIMEOUT = 1000;
    private static final int INT_SIZE = 4;
    private boolean done;
    private final ServerSocket socket;
    private final Datastore datastore;
    private final ServerSocketChannel channel;
    private final Selector selector;
    private final String address;

    public ChannelledUpdateListener(Datastore datastore, String address) throws IOException {
        channel = ServerSocketChannel.open();
        selector = Selector.open();
        socket = channel.socket();
        this.address = address;
        socket.bind(new InetSocketAddress(address, 0));
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_ACCEPT);
        done = false;
        this.datastore = datastore;
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public int getPort() {
        return socket.getLocalPort();
    }

    private static class Attachment {

        private final ByteBuffer buffer;
        private final CyclicByteBuffer cyclicBuffer;
        private final ByteBufferOutputStream outputStream;
        private final ByteBufferInputStream inputStream;
        private int objectSize = -1;

        Attachment(ByteBuffer buffer) {
            this.buffer = buffer;
            cyclicBuffer = new CyclicByteBuffer(BUFFER_SIZE, false);
            this.outputStream = new ByteBufferOutputStream(cyclicBuffer);
            this.inputStream = new ByteBufferInputStream(cyclicBuffer);
        }

        private int readInt() throws IOException {
            byte[] data = new byte[INT_SIZE];
            int dataToRead = INT_SIZE;

            while (dataToRead > 0) {
                dataToRead -= inputStream.read(data, 0, dataToRead);
            }

            DataInputStream bis = new DataInputStream(new ByteArrayInputStream(data));
            return bis.readInt();
        }

        private int getObjectSize() throws IOException {
            if (objectSize > 0) {
                return objectSize;
            }

            if (inputStream.available() >= INT_SIZE) {
                objectSize = readInt();
            }

            return objectSize;
        }

        byte[] getObject() throws IOException {
            int size = getObjectSize();

            if (size >= 0 && inputStream.available() >= size) {
                int bytesToRead = size;
                byte[] data = new byte[size];
                int index = 0;
                while (bytesToRead > 0) {
                    bytesToRead -= inputStream.read(data, index, bytesToRead);
                }

                objectSize = -1;

                return data;
            }

            return null;
        }

        OutputStream getOutputStream() {
            return outputStream;
        }
    }

    private void acceptConnection(SelectionKey next) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) next.channel();
        SocketChannel connection = serverChannel.accept();
        LOG.info("Accepted connection from: " + connection);
        connection.configureBlocking(false);
        SelectionKey register = connection.register(selector, SelectionKey.OP_READ);
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        register.attach(new Attachment(buffer));
    }

    private void putMessage(Message.KeyValue value) {
        if (value.getTtl() > 0) {
            datastore.put(value.getKey(), value.getData(), value.getTtl(), TimeUnit.SECONDS, false);
        } else {
            datastore.put(value.getKey(), value.getData(), false);
        }
    }

    private void removeMessage(Message.KeyValue value) {
        datastore.remove(value.getKey(), false);
    }

    private Attachment readData(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        Attachment attachment = (Attachment) key.attachment();
        ByteBuffer buffer = (ByteBuffer) attachment.buffer;

        int read;
        while ((read = client.read(buffer)) > 0) {
            buffer.flip();
            attachment.getOutputStream().write(buffer.array(), 0, read);
            buffer.clear();
        }

        if (read == -1) {
            key.channel().close();
            key.cancel();
        }

        return attachment;
    }

    private void readMessage(SelectionKey key) throws IOException, ClassNotFoundException {
        Attachment attachment = readData(key);
        byte[] data = attachment.getObject();

        while (data != null && data.length > 0) {
            ByteArrayInputStream is = new ByteArrayInputStream(data);
            while (is.available() > 0) {
                Message message = (Message) ObjectSerializer.instance().readObject(is);
                for (Message.KeyValue value : message.getValues()) {
                    switch (value.getOperation()) {
                        case PUT:
                            putMessage(value);
                            break;
                        case REMOVE:
                            removeMessage(value);
                            break;
                            
                        default:
                            
                            break;
                    }
                }
            }

            data = attachment.getObject();
        }
    }

    private void handle(SelectionKey key) throws IOException, ClassNotFoundException {
        if (key.isAcceptable()) {
            acceptConnection(key);
        } else if (key.isReadable()) {
            readMessage(key);
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
                    handle(key);
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
