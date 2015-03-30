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
package org.openrepose.core.services.jmx;

import org.openrepose.core.services.reporting.jmx.CompositeDataBuilder;

import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import java.util.ArrayList;
import java.util.List;

public class ConfigurationInformationCompositeDataBuilder extends CompositeDataBuilder {

    private final FilterInformation filter;


    public ConfigurationInformationCompositeDataBuilder(FilterInformation filter) {
        this.filter = filter;
    }

    @Override
    public String getItemName() {
        return filter.getName();
    }

    @Override
    public String getDescription() {
        return "Information about filter " + filter.getName() + ".";
    }

    @Override
    public String[] getItemNames() {
        return new String[]{"id", "name", "regex", "configuration", "successfully initialized", "successfully loaded configurations", "loading failed configurations"};
    }

    @Override
    public String[] getItemDescriptions() {
        return new String[]{"The filter id in the filter chain.",
                "The filter name.",
                "The URI Reg Ex for this filter.",
                "The configuration file specified for this filter instance.",
                "Boolean indication of configuration loaded correctly or not.",
                "successfully loaded configuration files",
                "Failed loading configuration files"
        };
    }

    @Override
    public OpenType[] getItemTypes() {
        return new OpenType[]{SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.BOOLEAN, SimpleType.STRING, SimpleType.STRING};
    }

    @Override
    public Object[] getItems() {
        final List<Object> items = new ArrayList<Object>();

        items.add(filter.getId());
        items.add(filter.getName());
        items.add(filter.getRegex());
        items.add(filter.getConfiguration());
        items.add(filter.getIsConfiguarationLoaded());
        StringBuilder successConfigurationLoading = new StringBuilder();
        for (String key : filter.getSuccessConfigurationLoadinginformation().keySet()) {
            String[] successInformation = filter.getSuccessConfigurationLoadinginformation().get(key);
            successConfigurationLoading.append(key + ": ");
            successConfigurationLoading.append(successInformation[0] + ": ");
            successConfigurationLoading.append(successInformation[1]);
            successConfigurationLoading.append(System.getProperty("line.separator"));
        }

        items.add(successConfigurationLoading.toString());

        StringBuilder failedConfigurationLoading = new StringBuilder();
        for (String key : filter.getFailedConfigurationLoadingInformation().keySet()) {
            String[] successInformation = filter.getFailedConfigurationLoadingInformation().get(key);
            failedConfigurationLoading.append(key + ": ");
            failedConfigurationLoading.append(successInformation[0] + ": ");
            failedConfigurationLoading.append(successInformation[1] + ": ");
            failedConfigurationLoading.append(successInformation[2]);
            failedConfigurationLoading.append(System.getProperty("line.separator"));

        }
        items.add(failedConfigurationLoading.toString());
        return items.toArray();
    }


}
