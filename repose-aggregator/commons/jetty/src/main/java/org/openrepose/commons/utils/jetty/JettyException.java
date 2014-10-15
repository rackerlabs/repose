package org.openrepose.commons.utils.jetty;

public class JettyException extends Exception {
    public JettyException(String message) {
        super(message);
    }
    
    public JettyException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public JettyException(Throwable cause) {
        super(cause);
    }
}
