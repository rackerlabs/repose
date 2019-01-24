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
package org.openrepose.nodeservice.response;

import org.apache.commons.lang3.StringUtils;
import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestUtil;
import org.openrepose.core.spring.ReposeSpringProperties;
import org.openrepose.nodeservice.containerconfiguration.ContainerConfigurationService;
import org.springframework.beans.factory.annotation.Value;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

@Named
public class ResponseHeaderServiceImpl implements ResponseHeaderService {

    private final String reposeVersion;
    private final ContainerConfigurationService containerConfigurationService;

    @Inject
    public ResponseHeaderServiceImpl(ContainerConfigurationService containerConfigurationService,
                                     @Value(ReposeSpringProperties.CORE.REPOSE_VERSION) String reposeVersion) {
        this.containerConfigurationService = containerConfigurationService;
        this.reposeVersion = reposeVersion;
    }

    @Override
    public void setVia(HttpServletRequest request, HttpServletResponse response) {
        final Optional<String> responseVia = containerConfigurationService.getResponseVia();
        final boolean includeViaReposeVersion = containerConfigurationService.includeViaReposeVersion();
        if (responseVia.isPresent() || includeViaReposeVersion) {
            final String existingVia = response.getHeader(CommonHttpHeader.VIA);
            final StringBuilder builder = new StringBuilder();
            if (StringUtils.isNotBlank(existingVia)) {
                builder.append(existingVia).append(", ");
            }
            builder.append(HttpServletRequestUtil.getProtocolVersion(request));
            responseVia.ifPresent(resVia -> {
                builder.append(" ");
                builder.append(responseVia.get());
            });
            if (includeViaReposeVersion) {
                if (!responseVia.isPresent()) {
                    builder.append(" Repose");
                }
                builder.append(" (Repose/").append(reposeVersion).append(")");
            }
            response.setHeader(CommonHttpHeader.VIA, builder.toString());
        }
    }
}
