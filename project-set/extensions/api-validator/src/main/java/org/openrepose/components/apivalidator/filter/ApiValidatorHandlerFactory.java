package org.openrepose.components.apivalidator.filter;

import com.rackspace.com.papi.components.checker.Config;
import com.rackspace.com.papi.components.checker.handler.ResultHandler;
import com.rackspace.com.papi.components.checker.handler.SaveDotHandler;
import com.rackspace.com.papi.components.checker.handler.ServletResultHandler;
import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.config.parser.generic.GenericResourceConfigurationParser;
import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.util.StringUriUtilities;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import com.rackspace.papi.service.config.ConfigurationService;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openrepose.components.apivalidator.servlet.config.ValidatorConfiguration;
import org.openrepose.components.apivalidator.servlet.config.ValidatorItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiValidatorHandlerFactory extends AbstractConfiguredFilterHandlerFactory<ApiValidatorHandler> {

    private static final Logger LOG = LoggerFactory.getLogger(ApiValidatorHandlerFactory.class);
    private ValidatorConfiguration validatorConfiguration;
    private ValidatorInfo defaultValidator;
    private List<ValidatorInfo> validators;
    private boolean initialized = false;
    private final ConfigurationService manager;
    private final ApiValidatorWadlListener wadlListener;
    private final Object lock;
    private final String configRoot;
    private boolean multiRoleMatch=false;

    public ApiValidatorHandlerFactory(ConfigurationService manager, String configurationRoot) {
        this.manager = manager;
        wadlListener = new ApiValidatorWadlListener();
        lock = new Object();
        this.configRoot = configurationRoot;
    }

    private void unsubscribeAll() {
        synchronized (lock) {
            initialized = false;
            if (validators == null) {
                return;
            }

            for (ValidatorInfo info : validators) {
                manager.unsubscribeFrom(info.getUri(), wadlListener);
            }
        }
    }

    ApiValidatorWadlListener getWadlListener() {
        return wadlListener;
    }

    void setValidators(List<ValidatorInfo> validators) {
        this.validators = validators;
    }

    class ApiValidatorWadlListener implements UpdateListener<ConfigurationResource> {

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

                for (ValidatorInfo info : validators) {
                    if (getNormalizedPath(info.getUri()).equals(config.name())) {
                        info.reinitValidator();
                        found = true;
                    }
                }

                if (!found) {
                    // If we couldn't match the particular config... be safe and clear 
                    // all fo the validators
                    for (ValidatorInfo info : validators) {
                       info.reinitValidator();
                    }
                }
            }
        }
    }

    private void addListener(String wadl) {
        LOG.info("Watching WADL: " + wadl);
        manager.subscribeTo(wadl, wadlListener, new GenericResourceConfigurationParser());
    }

    String getWadlPath(String wadl) {
        return !wadl.contains("://") ? StringUtilities.join("file://", configRoot, File.separator, wadl) : wadl;
    }
    
    String getPath(String path){
       File file = new File(configRoot,path);
       
       return file.exists() ? file.getAbsolutePath() : path;
       //return StringUtilities.nullSafeStartsWith(path, File.separator) || StringUtilities.nullSafeStartsWith(path, ":\\") ? path : StringUtilities.join(configRoot, File.separator, path);
    }

    private DispatchHandler getHandlers(ValidatorItem validatorItem) {
        List<ResultHandler> handlers = new ArrayList<ResultHandler>();
        
        if(!multiRoleMatch){
            handlers.add(new ServletResultHandler());
        }
        

        if (StringUtilities.isNotBlank(validatorItem.getDotOutput())) {
            final String dotPath = StringUriUtilities.formatUri(getPath(validatorItem.getDotOutput()));
            File out = new File(dotPath);
            try {
                if (out.exists() && out.canWrite() || !out.exists() && out.createNewFile()) {
                    handlers.add(new SaveDotHandler(out, true, true));
                } else {
                    LOG.warn("Cannot write to DOT file: " + dotPath);
                }
            } catch (IOException ex) {
                LOG.warn("Cannot write to DOT file: " + dotPath, ex);
            }
        }
        return new DispatchHandler(handlers.toArray(new ResultHandler[handlers.size()]));
    }

    void initialize() {
        synchronized (lock) {
            if (initialized || validatorConfiguration == null) {
                return;
            }

            validators = new ArrayList<ValidatorInfo>(validatorConfiguration.getValidator().size());
            defaultValidator = null;
            multiRoleMatch=validatorConfiguration.isMultiRoleMatch();

            for (ValidatorItem validatorItem : validatorConfiguration.getValidator()) {
                Config config = new Config();
                config.setResultHandler(getHandlers(validatorItem));
                config.setUseSaxonEEValidation(validatorItem.isUseSaxon());
                config.setCheckWellFormed(validatorItem.isCheckWellFormed());
                config.setCheckXSDGrammar(validatorItem.isCheckXsdGrammar());
                config.setCheckElements(validatorItem.isCheckElements());
                config.setXPathVersion(validatorItem.getXpathVersion());

                config.setCheckPlainParams(validatorItem.isCheckPlainParams());
                config.setDoXSDGrammarTransform(validatorItem.isDoXsdGrammarTransform());
                config.setEnablePreProcessExtension(validatorItem.isEnablePreProcessExtension());
                config.setRemoveDups(validatorItem.isRemoveDups());
                config.setValidateChecker(validatorItem.isValidateChecker());
                config.setXSLEngine(validatorItem.getXslEngine().value());
                config.setJoinXPathChecks(validatorItem.isJoinXpathChecks());

                ValidatorInfo validator = new ValidatorInfo(validatorItem.getRole(), getPath(validatorItem.getWadl()), config);
                validators.add (validator);
                if (validatorItem.isDefault() && defaultValidator == null) {
                    defaultValidator = validator;
                }
            }

            for (ValidatorInfo validator : validators) {
                addListener(validator.getUri());
            }

            initialized = true;
        }
    }

    private class ApiValidationConfigurationListener implements UpdateListener<ValidatorConfiguration> {

        @Override
        public void configurationUpdated(ValidatorConfiguration configurationObject) {
            validatorConfiguration = configurationObject;
            unsubscribeAll();
            initialize();
        }
    }

    void setValidatorCOnfiguration(ValidatorConfiguration configurationObject) {
        validatorConfiguration = configurationObject;
    }

    @Override
    protected ApiValidatorHandler buildHandler() {
        initialize();
        if (!initialized) {
            return null;
        }
        return new ApiValidatorHandler(defaultValidator, validators, multiRoleMatch);
    }
    


    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        final Map<Class, UpdateListener<?>> updateListeners = new HashMap<Class, UpdateListener<?>>();
        updateListeners.put(ValidatorConfiguration.class, new ApiValidationConfigurationListener());
        return updateListeners;
    }
}
