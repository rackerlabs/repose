/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.nodeservice.request;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.headers.common.ViaHeaderBuilder;
import org.openrepose.core.services.healthcheck.HealthCheckService;
import org.openrepose.nodeservice.containerconfiguration.ContainerConfigurationService;

import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class RequestHeaderServiceImplTest {

    public static class WhenSettingHeaders {
        private RequestHeaderServiceImpl instance;
        private HttpServletRequestWrapper request;
        private ViaHeaderBuilder viaBuilder;

        @Before
        public void setup() {
            request = mock(HttpServletRequestWrapper.class);
            viaBuilder = mock(ViaHeaderBuilder.class);
            instance = new RequestHeaderServiceImpl(mock(ConfigurationService.class), mock(ContainerConfigurationService.class), mock(HealthCheckService.class), "cluster", "node", "1.0");
        }

        @Test
        public void shouldSetXForwardedForHeader() {
            final String remote = "1.2.3.4";

            when(request.getRemoteAddr()).thenReturn(remote);
            instance.setXForwardedFor(request);

            verify(request).addHeader(CommonHttpHeader.X_FORWARDED_FOR, remote);

        }

        @Test
        public void shouldSetViaHeader() {
            final String via = "via";

            when(viaBuilder.buildVia(request)).thenReturn(via);
            instance.updateConfig(viaBuilder);
            instance.setVia(request);
            verify(request).addHeader(CommonHttpHeader.VIA, via);
        }
    }
}
