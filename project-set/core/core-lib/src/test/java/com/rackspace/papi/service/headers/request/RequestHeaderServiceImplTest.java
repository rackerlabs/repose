package com.rackspace.papi.service.headers.request;

import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.service.headers.common.ViaHeaderBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class RequestHeaderServiceImplTest {

    public static class WhenSettingHeaders {
        private RequestHeaderServiceImpl instance;
        private MutableHttpServletRequest request;
        private ViaHeaderBuilder viaBuilder;

        @Before
        public void setup() {
            request = mock(MutableHttpServletRequest.class);
            viaBuilder = mock(ViaHeaderBuilder.class);
            instance = new RequestHeaderServiceImpl();
        }
        
        @Test
        public void shouldSetXForwardedForHeader() {
            final String remote = "1.2.3.4";
            
            when(request.getRemoteAddr()).thenReturn(remote);
            instance.setXForwardedFor(request);
            
            verify(request).addHeader(CommonHttpHeader.X_FORWARDED_FOR.toString(), remote);
            
        }
        
        @Test
        public void shouldSetViaHeader() {
            final String via = "via";
            
            when(viaBuilder.buildVia(request)).thenReturn(via);
            instance.updateConfig(viaBuilder);
            instance.setVia(request);
            verify(request).addHeader(CommonHttpHeader.VIA.toString(), via);
        }
    }
}
