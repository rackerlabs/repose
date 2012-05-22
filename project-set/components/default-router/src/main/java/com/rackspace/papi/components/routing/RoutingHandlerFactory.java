package com.rackspace.papi.components.routing;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import com.rackspace.papi.model.SystemModel;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.HashMap;
import java.util.Map;

public class RoutingHandlerFactory extends AbstractConfiguredFilterHandlerFactory<RoutingTagger> implements ApplicationContextAware {

    private ApplicationContext applicationContext;
    private SystemModel systemModel;

    public RoutingHandlerFactory() {
    }

    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        this.applicationContext = ac;
    }

    private class RoutingConfigurationListener implements UpdateListener<SystemModel> {

        @Override
        public void configurationUpdated(SystemModel configurationObject) {
            systemModel = configurationObject;
        }
    }

    @Override
    protected RoutingTagger buildHandler() {
        return applicationContext
            .getBean("routingTagger", RoutingTagger.class)
            .setSystemModel(systemModel);
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        return new HashMap<Class, UpdateListener<?>>() {

            {
                put(SystemModel.class, new RoutingConfigurationListener());
            }
        };
    }
}
