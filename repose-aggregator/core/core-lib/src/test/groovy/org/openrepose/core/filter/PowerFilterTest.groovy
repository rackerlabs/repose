package org.openrepose.core.filter
import org.openrepose.core.services.config.impl.PowerApiConfigurationManager
import org.openrepose.core.services.context.ContextAdapter
import org.openrepose.core.services.context.ServiceContext
import org.openrepose.core.services.context.container.ContainerConfigurationService
import org.openrepose.core.services.event.PowerProxyEventManager
import org.openrepose.core.services.headers.response.ResponseHeaderService
import org.openrepose.core.services.reporting.ReportingService
import org.openrepose.core.services.rms.ResponseMessageService
import org.junit.Ignore
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.springframework.mock.web.MockHttpServletResponse

import javax.naming.Context
import javax.naming.NamingException
import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static org.openrepose.commons.utils.servlet.http.RequestValuesImpl.REQUEST_URI_ATTRIBUTE
import static org.hamcrest.Matchers.equalTo
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertThat
import static org.mockito.Matchers.any
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when
/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class PowerFilterTest {
    public static class WhenUsingPowerFilterTest {
        @Test
        public void shouldInstantiate() {
            PowerFilter powerFilter = new PowerFilter();

            assertNotNull(powerFilter);            
        }

        @Test
        public void shouldDestroy() {
            PowerFilter powerFilter = new PowerFilter();
            powerFilter.destroy();
        }

        @Test @Ignore("There aren't any assertions in this test so I'm going to disable it for review")
        public void shouldDoFilter() throws IOException, ServletException, NamingException {
            ResponseMessageService mockedResponseMessageService = mock(ResponseMessageService.class);
            Context mockedContext = mock(Context.class);
            ServiceContext mockedServiceContext = mock(ServiceContext.class);
            ServiceContext mockedEventServiceContext = mock(ServiceContext.class);
            ServiceContext mockedConfigServiceContext = mock(ServiceContext.class);
            FilterConfig mockedFilterConfig = mock(FilterConfig.class);
            ServletContext mockedServletContext = mock(ServletContext.class);
            FakeFilterRegistration mockedFilterRegistration = new FakeFilterRegistration();
            PowerProxyEventManager mockEventManager = mock(PowerProxyEventManager.class);
            PowerApiConfigurationManager mockConfigManager = mock(PowerApiConfigurationManager.class);

            when(mockedServiceContext.getService()).thenReturn(mockedResponseMessageService);
            when(mockedEventServiceContext.getService()).thenReturn(mockEventManager);
            when(mockedConfigServiceContext.getService()).thenReturn(mockConfigManager);
            
            when(mockedContext.lookup("powerapi:/services/rms")).thenReturn(mockedServiceContext);
            when(mockedContext.lookup("powerapi:/kernel/event")).thenReturn(mockedEventServiceContext);
            when(mockedContext.lookup("powerapi:/services/configuration")).thenReturn(mockedConfigServiceContext);
            
            when(mockedFilterConfig.getServletContext()).thenReturn(mockedServletContext);
            when(mockedServletContext.addFilter(any(String.class), any(Filter.class))).thenReturn(mockedFilterRegistration);
            when(mockedServletContext.getAttribute("PAPI_ServletContext")).thenReturn(mockedContext);

            Enumeration<String> mockedHeaderNames = mock(Enumeration.class);
            FilterChain mockedFilterChain = mock(FilterChain.class);
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeaderNames()).thenReturn(mockedHeaderNames);
            HttpServletResponse response = mock(HttpServletResponse.class);
            when(response.getStatus()).thenReturn(200);

            PowerFilter powerFilter = new PowerFilter();

            powerFilter.init(mockedFilterConfig);

            powerFilter.doFilter(request, response, mockedFilterChain);
        }

        @Test
        public void should400WithBadUri() throws Exception {
            PowerFilter powerFilter = new PowerFilter()
            def contextAdapter = mock(ContextAdapter)
            when(contextAdapter.containerConfigurationService()).thenReturn(mock(ContainerConfigurationService))
            when(contextAdapter.responseMessageService()).thenReturn(mock(ResponseMessageService))
            powerFilter.papiContext = contextAdapter
            powerFilter.responseHeaderService = mock(ResponseHeaderService)
            powerFilter.reportingService = mock(ReportingService)
            def request = mock(HttpServletRequest.class)
            when(request.getAttribute(REQUEST_URI_ATTRIBUTE)).thenReturn("http://openrepose.org/[butts]")
            def response = new MockHttpServletResponse()
            powerFilter.doFilter(request, response, mock(FilterChain.class))

            assertThat(response.getStatus(), equalTo(HttpServletResponse.SC_BAD_REQUEST))
        }
    }
}
