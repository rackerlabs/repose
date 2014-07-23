package com.rackspace.papi.service.headers.response;

import com.rackspace.papi.service.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.commons.util.servlet.http.RouteDestination;
import com.rackspace.papi.container.config.ContainerConfiguration;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.headers.common.ViaHeaderBuilder;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;

@Named
public class ResponseHeaderServiceImpl implements ResponseHeaderService {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ResponseHeaderServiceImpl.class);
    private final ConfigurationService configurationManager;
    private final ContainerConfigurationListener configurationListener;
    private ViaHeaderBuilder viaHeaderBuilder;
    private LocationHeaderBuilder locationHeaderBuilder;
    private String reposeVersion = "";

    @Inject
    public ResponseHeaderServiceImpl(ConfigurationService configurationManager,
                                     @Qualifier("reposeVersion") String reposeVersion) {
        this.reposeVersion = reposeVersion;
        this.configurationManager = configurationManager;
        this.configurationListener = new ContainerConfigurationListener();
    }

    @PostConstruct
    public void afterPropertiesSet() {
        configurationManager.subscribeTo("container.cfg.xml", configurationListener, ContainerConfiguration.class);
    }

    @PreDestroy
    public void destroy() {
        configurationManager.unsubscribeFrom("container.cfg.xml", configurationListener);
    }

    @Override
    public synchronized void updateConfig(ViaHeaderBuilder viaHeaderBuilder,
                                          LocationHeaderBuilder locationHeaderBuilder) {
        this.viaHeaderBuilder = viaHeaderBuilder;
        this.locationHeaderBuilder = locationHeaderBuilder;
    }

    @Override
    public void setVia(MutableHttpServletRequest request, MutableHttpServletResponse response) {
        final String existingVia = response.getHeader(CommonHttpHeader.VIA.toString());
        final String myVia = viaHeaderBuilder.buildVia(request);
        final String via = StringUtilities.isBlank(existingVia) ? myVia : existingVia + ", " + myVia;

        response.setHeader(CommonHttpHeader.VIA.name(), via);
    }

    @Override
    public void fixLocationHeader(HttpServletRequest originalRequest,
                                  MutableHttpServletResponse response,
                                  RouteDestination destination,
                                  String destinationLocationUri,
                                  String proxiedRootContext) {
        String destinationUri = cleanPath(destinationLocationUri);

        if (!destinationUri.matches("^https?://.*")) {
            // local dispatch
            destinationUri = proxiedRootContext;
        }

        try {
            locationHeaderBuilder.setLocationHeader(
                    originalRequest,
                    response,
                    destinationUri,
                    destination.getContextRemoved(),
                    proxiedRootContext);
        } catch (MalformedURLException ex) {
            LOG.warn("Invalid URL in location header processing", ex);
        }
    }

    private String cleanPath(String uri) {
        return uri == null ? "" : uri.split("\\?")[0];
    }

    /**
     * Listens for updates to the container.cfg.xml file which holds the via
     * header receivedBy value.
     */
    private class ContainerConfigurationListener implements UpdateListener<ContainerConfiguration> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(ContainerConfiguration configurationObject) {

            if (configurationObject.getDeploymentConfig() != null) {
                final String viaReceivedBy = configurationObject.getDeploymentConfig().getVia();

                final ViaResponseHeaderBuilder viaBuilder = new ViaResponseHeaderBuilder(reposeVersion, viaReceivedBy);
                final LocationHeaderBuilder locationBuilder = new LocationHeaderBuilder();
            }
            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }
}
