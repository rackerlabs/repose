package org.openrepose.rnxp.io.push;

import org.jboss.netty.channel.Channel;
import org.openrepose.rnxp.logging.ThreadStamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zinic
 */
public class SimplePushController implements PushController {

    private final static Logger LOG = LoggerFactory.getLogger(SimplePushController.class);
    private final Channel channel;
    private boolean shouldRead;

    public SimplePushController(Channel channel) {
        this.channel = channel;
        shouldRead = channel.isReadable();
    }

    private void updateChannel() {
        channel.setReadable(shouldRead);
    }

    @Override
    public synchronized void stopMessageFlow() {
        ThreadStamp.log(LOG, "Pausing message flow");

        if (shouldRead) {
            shouldRead = false;
            updateChannel();
        }
    }

    @Override
    public synchronized void requestNext() {
        ThreadStamp.log(LOG, "Requesting more messages");

        if (!shouldRead) {
            shouldRead = true;
            updateChannel();
        }
    }
}
