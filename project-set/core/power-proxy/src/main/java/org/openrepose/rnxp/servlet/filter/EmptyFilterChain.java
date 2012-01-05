package org.openrepose.rnxp.servlet.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 *
 * @author zinic
 */
public final class EmptyFilterChain implements FilterChain {

    private static final EmptyFilterChain INSTANCE = new EmptyFilterChain();
    
    public static FilterChain getInstance() {
        return INSTANCE;
    }
    
    private EmptyFilterChain() {
        
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        // nop
    }
}
