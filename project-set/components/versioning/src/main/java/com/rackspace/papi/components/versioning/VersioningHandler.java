package com.rackspace.papi.components.versioning;

import com.rackspace.papi.commons.config.manager.LockedConfigurationUpdater;
import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.commons.util.thread.KeyedStackLock;
import org.slf4j.Logger;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.components.versioning.config.ServiceVersionMapping;
import com.rackspace.papi.components.versioning.config.ServiceVersionMappingList;
import com.rackspace.papi.components.versioning.domain.ConfigurationData;
import com.rackspace.papi.components.versioning.util.ContentTransformer;
import com.rackspace.papi.filter.LocalhostFilterList;
import com.rackspace.papi.model.Host;
import com.rackspace.papi.model.PowerProxy;
import com.rackspace.papi.filter.logic.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterDirector;
import java.util.HashMap;
import java.util.Map;

public class VersioningHandler extends AbstractFilterLogicHandler {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(VersioningHandler.class);
    
    private final Map<String, ServiceVersionMapping> configuredMappings = new HashMap<String, ServiceVersionMapping>();
    private final Map<String, Host> configuredHosts = new HashMap<String, Host>();
    private final KeyedStackLock updateLock = new KeyedStackLock();
    private final Object updateKey = new Object(), readKey = new Object();
    private final UpdateListener<ServiceVersionMappingList> versioningConfigurationListener;
    private final UpdateListener<PowerProxy> systemModelConfigurationListener;
    private final ContentTransformer transformer;
    private Host localHost;

    private ServiceVersionMappingList config;
    
    public VersioningHandler() {
        transformer = new ContentTransformer();
        versioningConfigurationListener = new LockedConfigurationUpdater<ServiceVersionMappingList>(updateLock, updateKey) {

            @Override
            protected void onConfigurationUpdated(ServiceVersionMappingList mappings) {
                if (mappings.getServiceRoot() == null || StringUtilities.isBlank(mappings.getServiceRoot().getHref())) {
                    LOG.error("Service root not defined - bailing on config update");
                    return;
                }

                configuredMappings.clear();

                for (ServiceVersionMapping mapping : mappings.getVersionMapping()) {
                    configuredMappings.put(mapping.getId(), mapping);
                }

                config = mappings;
            }
        };
        
        systemModelConfigurationListener = new LockedConfigurationUpdater<PowerProxy>(updateLock, updateKey) {
        
            @Override
            public final void onConfigurationUpdated(PowerProxy configurationObject) {
                localHost = new LocalhostFilterList(configurationObject).getLocalHost();
                
                for (Host powerApiHost : configurationObject.getHost()) {
                    configuredHosts.put(powerApiHost.getId(), powerApiHost);
                }
            }
        };

    }

    public UpdateListener<ServiceVersionMappingList> getVersioningConfigurationListener() {
        return versioningConfigurationListener;
    }

    public UpdateListener<PowerProxy> getSystemModelConfigurationListener() {
        return systemModelConfigurationListener;
    }

    private VersioningHelper buildVersioningHelper() {
        updateLock.lock(readKey);

        try {
            final Map<String, ServiceVersionMapping> copiedVersioningMappings = new HashMap<String, ServiceVersionMapping>(configuredMappings);
            final Map<String, Host> copiedHostDefinitions = new HashMap<String, Host>(configuredHosts);

            final ConfigurationData configData = new ConfigurationData(config.getServiceRoot().getHref(), localHost, copiedHostDefinitions, copiedVersioningMappings);
            
            return new VersioningHelper(configData, transformer);
        } finally {
            updateLock.unlock(readKey);
        }
    }

    public FilterDirector handleRequest(MutableHttpServletRequest request, MutableHttpServletResponse response) {
        return buildVersioningHelper().handleRequest(request);
    }
}
