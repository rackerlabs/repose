package org.openrepose.rnxp.servlet.context;

/**
 *
 * @author zinic
 */
public class ServletException extends javax.servlet.ServletException {

    public ServletException(String message, Throwable rootCause) {
        super(message, rootCause);
    }

    public ServletException(String message) {
        super(message);
    }
}
