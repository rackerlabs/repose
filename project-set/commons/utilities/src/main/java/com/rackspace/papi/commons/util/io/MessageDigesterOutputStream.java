package com.rackspace.papi.commons.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zinic
 */
public class MessageDigesterOutputStream extends OneTimeUseOutputStream {

    private static final Logger LOG = LoggerFactory.getLogger(MessageDigesterOutputStream.class);
    private static final int BUFFER_SIZE = 2048;
    private final MessageDigest digest;
    private byte[] digestBytes;

    public MessageDigesterOutputStream(MessageDigest digest) {
        this.digest = digest;
    }

    public static byte[] calculateMd5Hash(URL url) {
        {
            InputStream urlInput = null;
            try {
                final MessageDigesterOutputStream mdos = new MessageDigesterOutputStream(MessageDigest.getInstance(("MD5")));
                urlInput = url.openStream();

                int read;
                byte[] buffer = new byte[BUFFER_SIZE];
                while ((read = urlInput.read(buffer)) > -1) {
                    mdos.write(buffer, 0, read);
                }
                
                mdos.close();

                return mdos.getDigest();
            } catch (IOException ex) {
                LOG.warn("Error reading stream: " + url.toExternalForm(), ex);
            } catch (NoSuchAlgorithmException ex) {
                LOG.warn("Invalid digest algorithm: MD5", ex);
            } finally {
                try {
                    if (urlInput != null) {
                        urlInput.close();
                    }
                } catch (IOException ex) {
                    LOG.warn("Error closing stream", ex);
                }
            }
        }

        return null;

    }

    public byte[] getDigest() {
        return (byte[])digestBytes.clone();
    }

    @Override
    public void writeByte(int b) throws IOException {
        digest.update((byte) b);
    }

    @Override
    public void closeStream() throws IOException {
        digestBytes = digest.digest();
    }

    @Override
    public void flushStream() throws IOException {
        digest.reset();
    }
}
