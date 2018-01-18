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

package org.openrepose.core.services.serviceclient.akka.impl;

import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.httpclient.HttpClientService;
import org.openrepose.core.services.opentracing.OpenTracingService;
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient;
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClientFactory;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class AkkaServiceClientFactoryImpl implements AkkaServiceClientFactory {

    private final HttpClientService httpClientService;
    private final ConfigurationService configurationService;
    private final OpenTracingService openTracingService;

    @Inject
    public AkkaServiceClientFactoryImpl(HttpClientService httpClientService,
                                        ConfigurationService configurationService,
                                        OpenTracingService openTracingService) {
        this.httpClientService = httpClientService;
        this.configurationService = configurationService;
        this.openTracingService = openTracingService;
    }

    @Override
    public AkkaServiceClient newAkkaServiceClient() {
        return newAkkaServiceClient(null);
    }

    @Override
    public AkkaServiceClient newAkkaServiceClient(String connectionPoolId) {
        return new AkkaServiceClientImpl(connectionPoolId, httpClientService, configurationService, openTracingService);
    }
}
