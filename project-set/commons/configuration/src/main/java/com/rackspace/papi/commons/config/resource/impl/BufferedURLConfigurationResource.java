package com.rackspace.papi.commons.config.resource.impl;

import com.rackspace.papi.commons.config.ConfigurationResourceException;
import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.util.arrays.ByteArrayComparator;
import com.rackspace.papi.commons.util.io.MessageDigesterOutputStream;
import com.rackspace.papi.commons.util.io.OutputStreamSplitter;
import com.rackspace.papi.commons.util.io.SimpleByteBufferInputStream;
import com.rackspace.papi.commons.util.io.SimpleByteBufferOutputStream;
import com.rackspace.papi.commons.util.io.buffer.CyclicSimpleByteBuffer;
import com.rackspace.papi.commons.util.io.buffer.SimpleByteBuffer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class BufferedURLConfigurationResource implements ConfigurationResource<BufferedURLConfigurationResource> {

    private final byte[] internalByteArray;
    private final URL resourceUrl;
    private SimpleByteBuffer byteBuffer;
    private byte[] digest;

    public BufferedURLConfigurationResource(URL resourceUrl) {
        this.resourceUrl = resourceUrl;

        internalByteArray = new byte[2048];
    }

    @Override
    public String name() {
        return resourceUrl.toString();
    }

    @Override
    public boolean exists() throws IOException {
        return resourceUrl.openConnection().getContentLength() > 0;
    }

    private MessageDigesterOutputStream newDigesterOutputStream(String digestSpec) {
        try {
            return new MessageDigesterOutputStream(MessageDigest.getInstance(digestSpec));
        } catch (NoSuchAlgorithmException nsae) {
            throw new ConfigurationResourceException("unrecognized digest specification", nsae);
        }
    }

    //TODO::Review - File descriptor management is a concern we have not looked at in depth
    private byte[] read(SimpleByteBuffer buffer) {
        final OutputStream bufferOut = new SimpleByteBufferOutputStream(buffer);
        final MessageDigesterOutputStream mdos = newDigesterOutputStream("MD5");

        final OutputStreamSplitter splitter = new OutputStreamSplitter(bufferOut, mdos);

        try {
            final InputStream urlInput = resourceUrl.openStream();

            int read;

            while ((read = urlInput.read(internalByteArray)) > -1) {
                splitter.write(internalByteArray, 0, read);
            }

            splitter.close();
            urlInput.close();

            return mdos.getDigest();
        } catch (IOException ioe) {
            throw new ConfigurationResourceException("Failed to read from configuration resource: " + ioe.getMessage(), ioe);
        }
    }

    @Override
    public synchronized boolean updated() throws IOException {
        final SimpleByteBuffer freshBuffer = new CyclicSimpleByteBuffer();
        final byte[] newDigest = read(freshBuffer);

        if (digest == null || !new ByteArrayComparator(digest, newDigest).arraysAreEqual()) {
            byteBuffer = freshBuffer;
            digest = newDigest;

            return true;
        }

        return false;
    }

    @Override
    public synchronized InputStream newInputStream() throws IOException {
        if (byteBuffer == null && !updated()) {
            throw new IOException("Failed to perform initial read");
        }
        
        return new SimpleByteBufferInputStream(byteBuffer.copy());
    }
}
