package org.openrepose.components.xsdvalidator.servlet;



import com.rackspace.com.papi.components.checker.Config;
import com.rackspace.com.papi.components.checker.Validator;
import com.rackspace.com.papi.components.checker.handler.ServletResultHandler;
import com.rackspace.com.papi.components.checker.wadl.WADLException;
import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.util.HashMap;
import java.util.Map;
import javax.xml.transform.sax.SAXSource;
import org.openrepose.components.xsdvalidator.servlet.config.ValidatorConfiguration;
import org.openrepose.components.xsdvalidator.servlet.config.ValidatorItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

public class XsdValidatorHandlerFactory extends AbstractConfiguredFilterHandlerFactory<XsdValidatorHandler> {

   private static final Logger LOG = LoggerFactory.getLogger(XsdValidatorHandlerFactory.class);
   private ValidatorConfiguration validatorConfiguration;
   private Validator defaultValidator;
   private Map<String, Validator> validators;

   public XsdValidatorHandlerFactory() {
   }

   private class XsdValidationConfigurationListener implements UpdateListener<ValidatorConfiguration> {

      @Override
      public void configurationUpdated(ValidatorConfiguration configurationObject) {
         validatorConfiguration = configurationObject;

         validators = new HashMap<String, Validator>();
         defaultValidator = null;

         for (ValidatorItem validatorItem : validatorConfiguration.getValidator()) {
            Config config = new Config();
            config.setResultHandler(new ServletResultHandler());
            config.setUseSaxonEEValidation(validatorItem.isUseSaxon());
            config.setCheckWellFormed(validatorItem.isCheckWellFormed());
            config.setCheckXSDGrammar(validatorItem.isCheckXsdGrammer());
            config.setCheckElements(validatorItem.isCheckElements());
            config.setXPathVersion(validatorItem.getXpathVersion());

            try {
               Validator validator = Validator.apply(new SAXSource(new InputSource(validatorItem.getWadl())), config);
               validators.put(validatorItem.getRole(), validator);
               if (validatorItem.isDefault()) {
                  defaultValidator = validator;
               }
            } catch (Throwable ex) {
               LOG.warn("Cannot load validator for WADL: " + validatorItem.getWadl());
            }

         }
      }
   }

   @Override
   protected XsdValidatorHandler buildHandler() {
      //return new XsdValidatorHandler();
      return new XsdValidatorHandler(defaultValidator, validators);
   }

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      final Map<Class, UpdateListener<?>> updateListeners = new HashMap<Class, UpdateListener<?>>();
      updateListeners.put(ValidatorConfiguration.class, new XsdValidationConfigurationListener());
      return updateListeners;
   }
}
