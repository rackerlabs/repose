package framework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * This is based on the JavaWorld JAVA TRAPS HOW-TO article
 * by Michael C. Daconta titled "When Runtime.exec() won't".
 * It is subtitled "Navigate yourself around pitfalls related to the Runtime.exec() method".
 * It is available at: http://www.javaworld.com/article/2071275/core-java/when-runtime-exec---won-t.html
 */
public class StreamGobbler extends Thread {
    private static final Logger LOG = LoggerFactory.getLogger(StreamGobbler.class);
    InputStream is;
    String label;
    PrintStream os;
    boolean shutdown = false;

    StreamGobbler(InputStream is, String label, PrintStream os) {
        this.is = is;
        this.label = label;
        this.os = os;
    }

    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while(!shutdown) {
                line = br.readLine();
                if(line != null) {
                    os.println(label + line);
                }
                yield();
            }
            os.flush();
        } catch (IOException ioe) {
            LOG.error("Caught the following unexpected exception: " + ioe.getLocalizedMessage());
            LOG.trace("", ioe);
        }
    }

    public void shutdown() {
        shutdown = true;
    }
}
