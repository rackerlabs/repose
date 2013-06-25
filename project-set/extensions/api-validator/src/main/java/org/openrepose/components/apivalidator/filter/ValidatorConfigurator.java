package org.openrepose.components.apivalidator.filter;

import com.rackspace.com.papi.components.checker.Config;
import com.rackspace.com.papi.components.checker.handler.ResultHandler;
import com.rackspace.com.papi.components.checker.handler.SaveDotHandler;
import com.rackspace.com.papi.components.checker.handler.ServletResultHandler;
import com.rackspace.papi.commons.util.StringUriUtilities;
import com.rackspace.papi.commons.util.StringUtilities;
import org.openrepose.components.apivalidator.servlet.config.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * These classes use the <a href="http://en.wikipedia.org/wiki/Template_method_pattern">template pattern</a> to process
 * different versions of the validator.cfg.xml and maintain backward compatibility.
 *
 * The ValidatorConfigurator class provides the base algorithm and the subclasses provide the version-specific
 * functionality.  The base classes match the JAXB classes generated from the version-aware XML schema.
 *
 * A factory method is used to generate the appropriate ValidatorConfigurator child class without exposing
 * the multiple versions to the calling method.
 *
 */
public abstract class ValidatorConfigurator {

    private static final Logger LOG = LoggerFactory.getLogger(ValidatorConfigurator.class);

    static ValidatorConfigurator createValidatorConfigurator( BaseValidatorConfiguration valconfiguration )
          throws IllegalArgumentException {

        ValidatorConfigurator validatorConfigurator;

        if( valconfiguration instanceof ValidatorConfiguration1 ) {

            validatorConfigurator = new ValidatorConfigurator1();
        }
        else if( valconfiguration instanceof ValidatorConfiguration2 ) {

            validatorConfigurator = new ValidatorConfigurator2();
        }
        else {
            throw new IllegalArgumentException(); // TODO
        }

        return validatorConfigurator;
    }

    protected List<ValidatorInfo> validators;

    protected ValidatorInfo defaultvalidator;

    protected abstract List<? extends BaseValidatorItem> getValidatorItems( BaseValidatorConfiguration validatorConfiguration );

    protected abstract void configureVersion( Config config, BaseValidatorItem validatorItem );

    public ValidatorInfo getDefaultValidator() {

        return defaultvalidator;
    }

    public List<ValidatorInfo> getValidators() {
        return validators;
    }

    public static List< Class<? extends BaseValidatorConfiguration> > getConfigurationClasses() {
        List< Class<? extends BaseValidatorConfiguration> > r = new ArrayList< Class<? extends BaseValidatorConfiguration> >();

        r.add(BaseValidatorConfiguration.class);
        r.add(ValidatorConfiguration1.class);
        r.add(ValidatorConfiguration2.class);

        return r;
    }

    public void processConfiguration( BaseValidatorConfiguration validatorConfiguration, String configRoot, String config ) {

        defaultvalidator = null;

        List<? extends BaseValidatorItem> validatorItems = getValidatorItems( validatorConfiguration );
        validators = new ArrayList<ValidatorInfo>( validatorItems.size() );

        for (BaseValidatorItem validatorItem : validatorItems) {
            Config configuration = createConfiguration(validatorItem,
                                                       validatorConfiguration.isMultiRoleMatch(),
                                                       configRoot);
            ValidatorInfo validator =
                  validatorItem.getAny() != null
                        ? new ValidatorInfo(validatorItem.getRole(),
                                            (Element) validatorItem.getAny(),
                                            getWadlPath(config, configRoot),
                                            configuration,
                                            validatorItem.getValidatorName())
                        : new ValidatorInfo(validatorItem.getRole(),
                                            getWadlPath(validatorItem.getWadl(), configRoot),
                                            configuration,
                                            validatorItem.getValidatorName());

            validators.add(validator);
            if (validatorItem.isDefault() && defaultvalidator == null) {
                defaultvalidator = validator;
            }
        }
    }

    private Config createConfiguration( BaseValidatorItem validatorItem, boolean multiRoleMatch, String configRoot) {

        Config config = new Config() ;

        configureVersion( config, validatorItem );

        config.setResultHandler(getHandlers(validatorItem, multiRoleMatch, configRoot ));
        config.setCheckWellFormed(validatorItem.isCheckWellFormed());
        config.setCheckXSDGrammar(validatorItem.isCheckXsdGrammar());
        config.setCheckElements(validatorItem.isCheckElements());
        config.setXPathVersion(validatorItem.getXpathVersion());
        config.setCheckPlainParams(validatorItem.isCheckPlainParams());
        config.setDoXSDGrammarTransform(validatorItem.isDoXsdGrammarTransform());
        config.setEnablePreProcessExtension(validatorItem.isEnablePreProcessExtension());
        config.setRemoveDups(validatorItem.isRemoveDups());
        config.setValidateChecker(true);
        config.setJoinXPathChecks(validatorItem.isJoinXpathChecks());
        config.setCheckHeaders(validatorItem.isCheckHeaders());
        config.setEnableIgnoreXSDExtension(validatorItem.isEnableIgnoreXsdExtension());


        return config;
    }

    private DispatchHandler getHandlers(BaseValidatorItem validatorItem, boolean multiRoleMatch, String configRoot ) {
        List<ResultHandler> handlers = new ArrayList<ResultHandler>();

        if (!multiRoleMatch) {
            handlers.add(new ServletResultHandler());
        }

        if (StringUtilities.isNotBlank(validatorItem.getDotOutput())) {
            final String dotPath = StringUriUtilities.formatUri( getPath( validatorItem.getDotOutput(), configRoot ) );
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

    private String getPath(String path, String configRoot){
        File file = new File(path);

        if (!file.isAbsolute()){
            file = new File(configRoot, path);
        }

        return file.getAbsolutePath();
    }


    private String getWadlPath(String uri, String configRoot) {
        return !uri.contains("://") ? StringUtilities.join("file://", new File(configRoot, uri).getAbsolutePath()) : uri;
    }

    static class ValidatorConfigurator1 extends ValidatorConfigurator {

        @Override
        protected List<? extends BaseValidatorItem> getValidatorItems( BaseValidatorConfiguration validatorConfiguration ) {
            return ((ValidatorConfiguration1)validatorConfiguration).getValidator();
        }

        @Override
        protected void configureVersion( Config config, BaseValidatorItem validatorItem ) {

            config.setXSDEngine(((ValidatorItem1)validatorItem).isUseSaxon() ? "SaxonEE" : "Xerces");
            config.setXSLEngine(((ValidatorItem1)validatorItem).getXslEngine().value());
        }
    }

    static class ValidatorConfigurator2 extends ValidatorConfigurator {

        @Override
        protected void configureVersion( Config config, BaseValidatorItem validatorItem ) {

            config.setXSDEngine(((ValidatorItem2)validatorItem).getXsdEngine().value());
            config.setXSLEngine(((ValidatorItem2)validatorItem).getXslEngine().value());
        }

        @Override
        protected List<? extends BaseValidatorItem> getValidatorItems( BaseValidatorConfiguration validatorConfiguration ) {
            return ((ValidatorConfiguration2)validatorConfiguration).getValidator();
        }
    }
}