package org.openrepose.components.xsdvalidator.servlet;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;

import java.util.HashMap;
import java.util.Map;
import org.openrepose.components.xsdvalidator.servlet.config.XsdValidatorConfiguration;
import com.rackspace.com.papi.components.checker.Validator;
import com.rackspace.com.papi.components.checker.Config;
import com.rackspace.com.papi.components.checker.ValidatorException;
import com.rackspace.com.papi.components.checker.handler.*;
import java.util.List;
import javax.xml.transform.sax.SAXSource;
import org.xml.sax.InputSource;

//import java.util.ArrayList;
//import java.util.List;

public class XsdValidatorHandlerFactory extends AbstractConfiguredFilterHandlerFactory<XsdValidatorHandler> {

    private XsdValidatorConfiguration xsdValidatorConfiguration;
    private List<Validator> validators;

    public XsdValidatorHandlerFactory() {
    }

    private class XsdValidationConfigurationListener implements UpdateListener<XsdValidatorConfiguration> {

        @Override
        public void configurationUpdated(XsdValidatorConfiguration configurationObject) {
            xsdValidatorConfiguration = configurationObject;
            Config config = new Config();
            config.setResultHandler(new ServletResultHandler());
            config.setUseSaxonEEValidation(false);
            config.setCheckWellFormed(true);
            config.setCheckXSDGrammar(true);
            config.setCheckElements(true);
            config.setXPathVersion(2);
            
            Validator validator = Validator.apply(new SAXSource(new InputSource("wadl")), config);
        }
    }

    @Override
    protected XsdValidatorHandler buildHandler() {
        return new XsdValidatorHandler(validators);
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        final Map<Class, UpdateListener<?>> updateListeners = new HashMap<Class, UpdateListener<?>>();
        updateListeners.put(XsdValidatorConfiguration.class, new XsdValidationConfigurationListener());
        return updateListeners;
    }
}
