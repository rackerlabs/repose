package com.rackspace.papi.components.versioning.testhelpers;

import com.rackspace.papi.commons.util.http.CommonHttpHeader;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: 6/10/11
 * Time: 12:58 PM
 */
public abstract class HttpServletRequestMockFactory {
    public static HttpServletRequest create(String requestUri, String requestUrl, String acceptHeader) {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);

        when(httpServletRequest.getRequestURI()).thenReturn(requestUri);

        StringBuffer temp = new StringBuffer();
        temp.append(requestUrl);
        when(httpServletRequest.getRequestURL()).thenReturn(temp);

        when(httpServletRequest.getHeader(CommonHttpHeader.ACCEPT.toString())).thenReturn(acceptHeader);

        return httpServletRequest;
    }
}
