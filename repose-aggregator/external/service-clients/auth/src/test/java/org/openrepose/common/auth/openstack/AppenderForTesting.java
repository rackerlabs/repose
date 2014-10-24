package org.openrepose.common.auth.openstack;

import org.apache.logging.log4j.core.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AppenderForTesting implements Appender {
    private static List messages = new ArrayList();
    private static volatile State state = State.INITIALIZED;

    @Override
    public void append(LogEvent logEvent) {
        messages.add(logEvent.getMessage().getFormattedMessage());
    }

    public static String[] getMessages() {
        return (String[]) messages.toArray(new String[messages.size()]);
    }

    public static void clear() {
        messages.clear();
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Layout<? extends Serializable> getLayout() {
        return null;
    }

    @Override
    public boolean ignoreExceptions() {
        return false;
    }

    @Override
    public ErrorHandler getHandler() {
        return null;
    }

    @Override
    public void setHandler(ErrorHandler errorHandler) {

    }

    @Override
    public void start() {
        state = State.STARTED;
    }

    @Override
    public void stop() {
        state = State.STOPPED;
    }

    @Override
    public boolean isStarted() {
        return state == State.STARTED;
    }

    @Override
    public boolean isStopped() {
        return state == State.STOPPED;
    }
}
