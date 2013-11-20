package com.rackspace.papi.components.logging.util;

import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.BufferOverflowException;

/**
 *
 * @author jhopper
 */

public class FileLogger implements SimpleLogger {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(FileLogger.class);
    private static final Charset CHAR_SET = Charset.forName("UTF-8");
    private static final ByteBuffer NEWLINE = ByteBuffer.wrap("\n".getBytes(CHAR_SET));
    static final int BUFFER_LIMIT = 2048;
    private final ByteBuffer buffer;
    private final File f;
    private final String possibleCause= "Possible Causes: Exception coming from HTTP logging Filter, while writing to log buffer." + "\n\t\t" +
            " 1. Please put Distributed Datastore filter first in the list of filters in System model, if you are using it." + "\n\t\t" +
            " 2. Please modify your Rate Limiting configuration file (If Rate limiting filter is being used) to not have broad capture groups, that can fill the datastore and eventually logs."  + "\n\t\t"+
            " 3. Please check your disk space and http log file size where repose HTTP logging filter is writing to."+ "\n\t\t";

    public FileLogger(File f) {
        this.f = f;

        buffer = ByteBuffer.allocate(BUFFER_LIMIT);
    }

    @Override
    public void log(String st) {
        final byte[] stringBytes = st.getBytes(CHAR_SET);
     
      FileOutputStream fileOutputStream =null;
      FileChannel channel =null;
       
               
        try {
            
            fileOutputStream = new FileOutputStream(f, true);
            channel = fileOutputStream.getChannel();
            
            int index = buffer.arrayOffset();

            while (index < stringBytes.length) {
                final int remainder = stringBytes.length - index;
                final int length = getRemainderAvailable(remainder);

                buffer.put(stringBytes, index, length);

                buffer.flip();


                channel.write(buffer);

                if (buffer.hasRemaining()) {
                    buffer.compact();
                } else {
                    buffer.clear();
                }

                index += length;
               
            }
            synchronized (NEWLINE) {
                channel.write(NEWLINE);
                NEWLINE.rewind();
            }
    
            
        } catch (BufferOverflowException ioe) {
            LOG.error(possibleCause+ioe.getMessage(), ioe);
        } catch (IOException ioe) {
            LOG.error(ioe.getMessage(), ioe);
        } finally {
            buffer.clear();
           
            try{
                if(channel!=null){ 
                   channel.close();
                 }
                if(fileOutputStream!=null){
                  fileOutputStream.close();
                }
            }catch(IOException ioe) {
              LOG.error(ioe.getMessage(), ioe);
            }
        }
       
    }

    int getRemainderAvailable(int remainder) {
        return remainder > BUFFER_LIMIT ? BUFFER_LIMIT : remainder;
    }

    @Override
    public void destroy() {
    }
}
