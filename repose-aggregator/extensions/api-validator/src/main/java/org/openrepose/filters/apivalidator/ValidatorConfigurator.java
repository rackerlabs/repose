package org.openrepose.filters.apivalidator;

import com.rackspace.com.papi.components.checker.Config;
import com.rackspace.com.papi.components.checker.handler.InstrumentedHandler;
import com.rackspace.com.papi.components.checker.handler.ResultHandler;
import com.rackspace.com.papi.components.checker.handler.SaveDotHandler;
import com.rackspace.com.papi.components.checker.handler.ServletResultHandler;
import org.openrepose.commons.utils.StringUriUtilities;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.components.apivalidator.servlet.config.ValidatorConfiguration;
import org.openrepose.components.apivalidator.servlet.config.ValidatorItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A helper class to process a ValidationConfiguration.
 */
public class ValidatorConfigurator {
    private static final Logger LOG = LoggerFactory.getLogger(ValidatorConfigurator.class);

    private ValidatorInfo defaultvalidator;
    private List<ValidatorInfo> validators;

    public ValidatorConfigurator() {}

    public ValidatorConfigurator(ValidatorConfiguration valConfig, String configRoot, String wadlUri) {
        processConfiguration(valConfig, configRoot, wadlUri);
    }

    public ValidatorInfo getDefaultValidator() {
        return defaultvalidator;
    }

    public List<ValidatorInfo> getValidators() {
        return validators;
    }

    public void processConfiguration(ValidatorConfiguration validatorConfiguration, String configRoot, String wadlUri) {
        defaultvalidator = null;

        List<? extends ValidatorItem> validatorItems = validatorConfiguration.getValidator();
        validators = new ArrayList<ValidatorInfo>(validatorItems.size());

        for (ValidatorItem validatorItem : validatorItems) {
            Config configuration = createConfiguration(validatorItem, validatorConfiguration.isMultiRoleMatch(), configRoot);
            configuration.setPreserveRequestBody(validatorConfiguration.isMultiRoleMatch());
            ValidatorInfo validator = validatorItem.getAny() != null
                    ? new ValidatorInfo(validatorItem.getRole(),
                    (Element) validatorItem.getAny(),
                    getWadlPath(wadlUri, configRoot),
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

    private Config createConfiguration(ValidatorItem validatorItem, boolean multiRoleMatch, String configRoot) {
        Config config = new Config();

        config.setXSDEngine(validatorItem.getXsdEngine().value());
        config.setXSLEngine(validatorItem.getXslEngine().value());
        config.setResultHandler(getHandlers(validatorItem, multiRoleMatch, configRoot));
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
        config.setEnableRaxRolesExtension(validatorItem.isEnableRaxRoles());
        config.setMaskRaxRoles403(validatorItem.isMaskRaxRoles403());

        return config;
    }

    private DispatchHandler getHandlers(ValidatorItem validatorItem, boolean multiRoleMatch, String configRoot) {
        List<ResultHandler> handlers = new ArrayList<ResultHandler>();

        if (!multiRoleMatch) {
            handlers.add(new ServletResultHandler());
        }

        if (validatorItem.isEnableApiCoverage()) {
            handlers.add(new InstrumentedHandler());
        }

        if (StringUtilities.isNotBlank(validatorItem.getDotOutput())) {
            final String dotPath = StringUriUtilities.formatUri(getPath(validatorItem.getDotOutput(), configRoot));
            File out = new File(dotPath);
            try {
                if (out.exists() && out.canWrite() || !out.exists() && out.createNewFile()) {
                    handlers.add(new SaveDotHandler(out, !validatorItem.isEnableApiCoverage(), true));
                } else {
                    LOG.warn("Cannot write to DOT file: " + dotPath);
                }
            } catch (IOException ex) {
                LOG.warn("Cannot write to DOT file: " + dotPath, ex);
            }
        }
        return new DispatchHandler(handlers.toArray(new ResultHandler[handlers.size()]));
    }

    private String getPath(String path, String configRoot) {
        File file = new File(path);

        if (!file.isAbsolute()) {
            file = new File(configRoot, path);
        }

        return file.getAbsolutePath();
    }

    private String getWadlPath(String uri, String configRoot) {
        return new File(configRoot, uri).toURI().toString();
    }
}
