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
package org.openrepose.filters.ipidentity;

import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.utils.net.IpAddressRange;
import org.openrepose.core.filter.logic.AbstractConfiguredFilterHandlerFactory;
import org.openrepose.filters.ipidentity.config.IpIdentityConfig;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IpIdentityHandlerFactory extends AbstractConfiguredFilterHandlerFactory<IpIdentityHandler> {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(IpIdentityHandlerFactory.class);
    private IpIdentityConfig config;
    private List<IpAddressRange> whitelist;

    public IpIdentityHandlerFactory() {
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        return new HashMap<Class, UpdateListener<?>>() {
            {
                put(IpIdentityConfig.class, new ClientIpIdentityConfigurationListener());
            }
        };
    }

    private class ClientIpIdentityConfigurationListener implements UpdateListener<IpIdentityConfig> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(IpIdentityConfig configurationObject) {
            config = configurationObject;
            whitelist = new ArrayList<IpAddressRange>();
            if (config.getWhiteList() != null) {
                for (String address : config.getWhiteList().getIpAddress()) {
                    try {
                        whitelist.add(new IpAddressRange(address));
                    } catch (UnknownHostException ex) {
                        LOG.warn("Invalid IP address specified in white list: " + address, ex);
                    }
                }
            }
            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }

    @Override
    protected IpIdentityHandler buildHandler() {

        if (!this.isInitialized()) {
            return null;
        }
        return new IpIdentityHandler(config, whitelist);
    }
}
