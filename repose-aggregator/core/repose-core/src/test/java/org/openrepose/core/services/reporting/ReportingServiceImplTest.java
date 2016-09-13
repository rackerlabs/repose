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
package org.openrepose.core.services.reporting;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.reporting.destinations.DestinationInfo;
import org.openrepose.core.services.reporting.impl.ReportingServiceImpl;
import org.openrepose.nodeservice.containerconfiguration.ContainerConfigurationService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@RunWith(Enclosed.class)
public class ReportingServiceImplTest {

    public static class WhenResetting {

        private static final int REFRESH_SECONDS = 2;
        private ReportingService reportingService;
        private List<String> destinationIds = new ArrayList<String>();

        @Before
        public void setup() {
            destinationIds.add("id_1");
            destinationIds.add("id_2");
            destinationIds.add("id_7");

            reportingService = new ReportingServiceImpl(mock(ConfigurationService.class), mock(ContainerConfigurationService.class));
            reportingService.updateConfiguration(destinationIds, REFRESH_SECONDS);
        }

        @Test
        public void shouldResetValuesEvery2Seconds() throws InterruptedException {
            for (int i = 0; i < 5; i++) {
                reportingService.incrementRequestCount("id_7");
            }

            assertEquals(5, reportingService.getDestinationInfo("id_7").getTotalRequests());

            // sleep for three seconds
            Thread.sleep(3 * 1000);

            assertEquals(0, reportingService.getDestinationInfo("id_7").getTotalRequests());
        }

        @Test
        public void shouldReturnDestinationList() {

            List<DestinationInfo> dst = reportingService.getDestinations();

            assertEquals(3, dst.size());
        }

        @Test
        public void shouldRecordServiceResponse() {

            Long responseTime = new Long("200");
            reportingService.recordServiceResponse("id_1", 202, responseTime);
            DestinationInfo dstInfo = reportingService.getDestinationInfo("id_1");
            assertEquals(1, dstInfo.getTotalStatusCode(202));
        }
    }
}
