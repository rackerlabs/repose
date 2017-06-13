package com.rackspace.lefty.tenant;

import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created by adrian on 6/12/17.
 */
public class TenantCullingFilterTest {
    @Test
    public void doFilterCallsChainDoFilter() throws Exception {
        TenantCullingFilter filter = new TenantCullingFilter();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(mock(ServletRequest.class), mock(ServletResponse.class), filterChain);

        verify(filterChain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }
}
