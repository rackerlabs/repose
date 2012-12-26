package org.openrepose.components.routing.servlet;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import org.openrepose.components.routing.servlet.config.DestinationRouterConfiguration;
import org.openrepose.components.routing.servlet.config.Target;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class DestinationRouterHandlerFactory extends AbstractConfiguredFilterHandlerFactory<RoutingTagger> {

    private DestinationRouterConfiguration contextRouterConfiguration;
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RoutingTagger.class);
    private float quality;
    private Target target;
    private static final String DEFAULT_QUALITY = "0.5";

    public DestinationRouterHandlerFactory() {
    }

    private class RoutingConfigurationListener implements UpdateListener<DestinationRouterConfiguration> {

       boolean isIntialized=false;
     
       
        @Override
        public void configurationUpdated(DestinationRouterConfiguration configurationObject) {

            contextRouterConfiguration = configurationObject;

            if (contextRouterConfiguration == null || configurationObject.getTarget() == null) {
                LOG.warn("Configuration file for Destination router seems to be missing or malformed.");
            } else {
                target = contextRouterConfiguration.getTarget();

                determineQuality();
            }
            
             isIntialized=true;
        }
        
        
        @Override
        public boolean isInitialized(){
        return isIntialized;
        }


        private void determineQuality() {
            if (target.isSetQuality()) {
                quality = Float.valueOf(target.getQuality()).floatValue();

            } else {
                quality = Float.valueOf(DEFAULT_QUALITY).floatValue();
            }

        }
    }

    @Override
    protected RoutingTagger buildHandler() {
      if( !this.isInitialized()){
           return null;
       } 
        return new RoutingTagger(target.getId(), quality);
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        final Map<Class, UpdateListener<?>> updateListeners = new HashMap<Class, UpdateListener<?>>();
        updateListeners.put(DestinationRouterConfiguration.class, new RoutingConfigurationListener());
        return updateListeners;
    }
}
