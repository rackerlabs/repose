package org.openrepose.components.xsdvalidator.servlet;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;

import java.util.HashMap;
import java.util.Map;
import org.openrepose.components.xsdvalidator.servlet.config.XsdValidatorConfiguration;

public class XsdValidatorHandlerFactory extends AbstractConfiguredFilterHandlerFactory<XsdValidatorHandler> {

    private XsdValidatorConfiguration xsdValidatorConfiguration;

    public XsdValidatorHandlerFactory() {
    }

    private class XsdValidationConfigurationListener implements UpdateListener<XsdValidatorConfiguration> {

        @Override
        public void configurationUpdated(XsdValidatorConfiguration configurationObject) {
            xsdValidatorConfiguration = configurationObject;
        }
    }

    @Override
    protected XsdValidatorHandler buildHandler() {
        return new XsdValidatorHandler();
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        final Map<Class, UpdateListener<?>> updateListeners = new HashMap<Class, UpdateListener<?>>();
        updateListeners.put(XsdValidatorConfiguration.class, new XsdValidationConfigurationListener());
        return updateListeners;
    }
}
