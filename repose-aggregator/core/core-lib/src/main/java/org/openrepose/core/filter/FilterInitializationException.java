package org.openrepose.core.filter;

public class FilterInitializationException extends Exception {
    public FilterInitializationException(String message, Throwable t) {
        super(message, t);
    }

    public FilterInitializationException(String message) {
        super(message);
    }
}
