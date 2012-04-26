package com.rackspace.papi.components.hnorm;

import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import com.rackspace.papi.filter.logic.FilterDirector;
import javax.servlet.http.HttpServletRequest;

import com.rackspacecloud.api.docs.powerapi.header_normalization.v1.HeaderFilterList;
import com.rackspacecloud.api.docs.powerapi.header_normalization.v1.HttpHeader;
import com.rackspacecloud.api.docs.powerapi.header_normalization.v1.HttpHeaderList;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Array;

import java.util.Enumeration;
import org.junit.Before;
import org.junit.Ignore;

// TODO: Update this test as part of new functionality
public class HeaderNormalizerTest {

    private HttpHeader header1 = new HttpHeader();
    private HttpHeader header2 = new HttpHeader();
    private HttpHeader header3 = new HttpHeader();
    private HttpHeader header4 = new HttpHeader();
    private HttpHeaderList blackList = new HttpHeaderList();
    private HttpHeaderList whiteList = new HttpHeaderList();
    private HeaderNormalizer headerNormalizer;
    private HeaderFilterList headerFilterList = new HeaderFilterList();
    private String[] requestHeaders = {"X-Auth-Header", "Content-Type", "X-User-Header", "Accept", "X-Group-Header", "X-Auth-Token"};
    private HttpServletRequest request;
    private FilterDirector director;

    @Before
    public void standUp() {
        request = mock(HttpServletRequest.class);
        director = new FilterDirectorImpl();

        header1.setId("X-Auth-Header");
        header2.setId("X-User-Header");

        blackList.getHeader().add(header1);
        blackList.getHeader().add(header2);


        header3.setId("X-Group-Header");
        header4.setId("Content-Type");
        whiteList.getHeader().add(header3);
        whiteList.getHeader().add(header4);


        Enumeration<String> e = new Enumeration<String>() {

            int size = Array.getLength(requestHeaders);
            int cursor;

            @Override
            public boolean hasMoreElements() {
                return (cursor < size);
            }

            @Override
            public String nextElement() {
                return (String) Array.get(requestHeaders, cursor++);
            }
        };
        when(request.getHeaderNames()).thenReturn(e);
//        headerFilterList.setBlacklist(blackList);
//        headerFilterList.setWhitelist(whiteList);

    }

    @Test
    @Ignore
    public void shouldFlagForRemovalFromBlackList() {

        headerNormalizer = new HeaderNormalizer(headerFilterList, true);
        headerNormalizer.normalizeHeaders(request, director);
        assertTrue("X-Auth-Header should be flagged for removal", director.requestHeaderManager().headersToRemove().contains(header1.getId().toLowerCase()));
        assertTrue("X-User-Header should be flagged for removal", director.requestHeaderManager().headersToRemove().contains(header2.getId().toLowerCase()));
        assertFalse("Accept should not be flagged for removal", director.requestHeaderManager().headersToRemove().contains("accept"));

    }

    @Test
    @Ignore
    public void shouldOnlyAllowFromWhiteList() {
        headerNormalizer = new HeaderNormalizer(headerFilterList, true);
        headerNormalizer.normalizeHeaders(request, director);
        assertTrue("X-Auth-Header should be flagged for removal", director.requestHeaderManager().headersToRemove().contains(header1.getId().toLowerCase()));
        assertTrue("X-User-Header should be flagged for removal", director.requestHeaderManager().headersToRemove().contains(header2.getId().toLowerCase()));
        assertTrue("X-User-Header should be flagged for removal", director.requestHeaderManager().headersToRemove().contains("accept"));
        assertFalse("X-Group-Header should not be flagged for removal", director.requestHeaderManager().headersToRemove().contains(header3.getId().toLowerCase()));
        assertFalse("Content-Type should not be flagged for removal", director.requestHeaderManager().headersToRemove().contains(header4.getId().toLowerCase()));

    }
}
