package org.openrepose.components.apivalidator.filter;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.config.parser.generic.GenericResourceConfigurationParser;
import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import com.rackspace.papi.service.config.ConfigurationService;
import org.openrepose.components.apivalidator.servlet.config.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class uses the <a href="http://en.wikipedia.org/wiki/Factory_method_pattern">factory pattern</a> to construct
 * a handler from the configuration files.
 *
 * ApiValidatorWadlListener and ApiValidationConfigurationListener are classes which re-initialize the handler factory
 * when the wadl or configuration files are changed.
 *
 */
public class ApiValidatorHandlerFactory extends AbstractConfiguredFilterHandlerFactory<ApiValidatorHandler> {
    
  
    private static final Logger LOG = LoggerFactory.getLogger(ApiValidatorHandlerFactory.class);
    private BaseValidatorConfiguration validatorConfiguration;
    private ValidatorInfo defaultValidator;
    private List<ValidatorInfo> validators;
    private boolean initialized = false;
    private final ConfigurationService manager;
    private final ApiValidatorWadlListener wadlListener;
    private final Object lock;
    private final String configRoot;
    private boolean multiRoleMatch = false;
    private final String config;

    public ApiValidatorHandlerFactory(ConfigurationService manager, String configurationRoot, String config) {
        this.manager = manager;
        wadlListener = new ApiValidatorWadlListener();
        lock = new Object();
        this.configRoot = configurationRoot;
        this.config = config;
    }

    private void unsubscribeAll() {
        synchronized (lock) {
            initialized = false;
            if (validators == null) {
                return;
            }

            for (ValidatorInfo info : validators) {
                if (StringUtilities.isNotBlank(info.getUri())) {
                    manager.unsubscribeFrom(info.getUri(), wadlListener);
                }
                if (info.getValidator() != null) {
                    info.getValidator().destroy();
                }
            }
        }
    }

    ApiValidatorWadlListener getWadlListener() {
        return wadlListener;
    }

    void setValidators(List<ValidatorInfo> validators) {
        this.validators = validators;
    }

    public class ApiValidatorWadlListener implements UpdateListener<ConfigurationResource> {

       private boolean isInitialized = false;
       
        private String getNormalizedPath(String uri) {
            String path = uri;
            try {
                path = new URL(uri).toString();
            } catch (MalformedURLException ex) {
                LOG.warn("Invalid URL: " + uri);
            }
            return path;
        }

        @Override
        public void configurationUpdated(ConfigurationResource config) {
            LOG.info("WADL file changed: " + config.name());

            synchronized (lock) {
                if (validators == null) {
                    return;
                }
                boolean found = false;
                boolean loadedWADL=true;

                for (ValidatorInfo info : validators) {
                    if (info.getUri() != null && getNormalizedPath(info.getUri()).equals(config.name())) {
                        if(loadedWADL){
                          loadedWADL=info.reinitValidator();
                        }else{
                           info.reinitValidator(); 
                        }
                        found = true;
                    }
                }

                if (!found) {
                    // If we couldn't match the particular config... be safe and clear 
                    // all fo the validators
                    for (ValidatorInfo info : validators) {
                        info.reinitValidator();
                    }
                }if(!loadedWADL){
                   isInitialized=false;
                }else{
                   isInitialized=true;
                }
            }
             
        }
          
       @Override
      public boolean isInitialized(){
          return isInitialized;
      }
      
  
    }

    private void addListener(String wadl) {
        if (wadl == null) {
            return;
        }

        LOG.info("Watching WADL: " + wadl);
        manager.subscribeTo("api-validator",wadl, wadlListener, new GenericResourceConfigurationParser());
    }

    String getWadlPath(String uri) {
        return !uri.contains("://") ? StringUtilities.join("file://", new File(configRoot, uri).getAbsolutePath()) : uri;
    }

    void initialize() {
        synchronized (lock) {
            if (initialized || validatorConfiguration == null) {
                return;
            }

            ValidatorConfigurator validatorConfigurator = ValidatorConfigurator.createValidatorConfigurator(
                  validatorConfiguration );

            validatorConfigurator.processConfiguration( validatorConfiguration,
                                               configRoot,
                                               config );

            defaultValidator = validatorConfigurator.getDefaultValidator() ;
            validators = validatorConfigurator.getValidators();

            for (ValidatorInfo validator : validators) {
                addListener(validator.getUri());
            }

            initialized = true;
        }
    }


    private class ApiValidationConfigurationListener implements UpdateListener<BaseValidatorConfiguration> {
        private boolean isInitialized = false;
       
        
        @Override
        public void configurationUpdated(BaseValidatorConfiguration configurationObject) {
            validatorConfiguration = configurationObject;
            unsubscribeAll();
            initialize();
            isInitialized=true;
        }
        
        @Override
        public boolean isInitialized(){
            return isInitialized;
        }
    }

    void setValidatorConfiguration(BaseValidatorConfiguration configurationObject) {
        validatorConfiguration = configurationObject;
    }

    @Override
    protected ApiValidatorHandler buildHandler() {
        initialize();
        if (!initialized || !this.isInitialized()) {
            return null;
        }
        return new ApiValidatorHandler(defaultValidator, validators, multiRoleMatch);
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        final Map<Class, UpdateListener<?>> updateListeners = new HashMap<Class, UpdateListener<?>>();
        ApiValidationConfigurationListener avcl = new ApiValidationConfigurationListener();

        for (Class<? extends BaseValidatorConfiguration> c : ValidatorConfigurator.getConfigurationClasses()) {
            updateListeners.put(c, avcl);
        }

        return updateListeners;
    }
}
