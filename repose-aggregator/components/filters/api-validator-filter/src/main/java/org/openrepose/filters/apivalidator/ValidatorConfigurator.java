/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.filters.apivalidator;

import com.rackspace.com.papi.components.checker.Config;
import com.rackspace.com.papi.components.checker.handler.*;
import org.openrepose.commons.utils.StringUriUtilities;
import org.apache.commons.lang3.StringUtils;
import org.openrepose.filters.apivalidator.config.DelegatingType;
import org.openrepose.filters.apivalidator.config.ValidatorConfiguration;
import org.openrepose.filters.apivalidator.config.ValidatorItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * A helper class to process a ValidationConfiguration.
 */
public class ValidatorConfigurator {
    private static final Logger LOG = LoggerFactory.getLogger(ValidatorConfigurator.class);

    private ValidatorInfo defaultValidator;
    private List<ValidatorInfo> validators;

    public ValidatorConfigurator() {
        // Retain the default constructor, though only used in testing.
    }

    public ValidatorConfigurator(ValidatorConfiguration valConfig, String configRoot, String wadlUri) {
        processConfiguration(valConfig, configRoot, wadlUri);
    }

    public ValidatorInfo getDefaultValidator() {
        return defaultValidator;
    }

    public List<ValidatorInfo> getValidators() {
        return validators;
    }

    public void processConfiguration(ValidatorConfiguration validatorConfiguration, String configRoot, String wadlUri) {
        logDeprecationWarnings(validatorConfiguration);

        defaultValidator = null;

        List<? extends ValidatorItem> validatorItems = validatorConfiguration.getValidator();
        validators = new ArrayList<>(validatorItems.size());

        DelegatingType delegatingType = validatorConfiguration.getDelegating();
        boolean isDelegating = delegatingType != null;
        double delegationQuality = isDelegating ? delegatingType.getQuality() : 0.0;
        String componentName = isDelegating ? delegatingType.getComponentName() : "api-validator";

        for (ValidatorItem validatorItem : validatorItems) {
            Config configuration = createConfiguration(validatorItem, isDelegating, delegationQuality, validatorConfiguration.isMultiRoleMatch(), configRoot, componentName);
            configuration.setPreserveRequestBody(validatorConfiguration.isMultiRoleMatch());
            ValidatorInfo validator = validatorItem.getAny() != null
                    ? new ValidatorInfo(validatorItem.getRole(),
                        validatorItem.getAny(),
                        getWadlPath(wadlUri, configRoot),
                        configuration,
                        validatorItem.getValidatorName())
                    : new ValidatorInfo(validatorItem.getRole(),
                        getWadlPath(validatorItem.getWadl(), configRoot),
                        configuration,
                        validatorItem.getValidatorName());

            validators.add(validator);
            if (validatorItem.isDefault() && defaultValidator == null) {
                defaultValidator = validator;
            }
        }
    }

    private void logDeprecationWarnings(ValidatorConfiguration validatorConfiguration) {
        if (validatorConfiguration.isMultiRoleMatch()) {
            LOG.warn("Support for multi-role-match has been deprecated in Repose 8 and will be removed in Repose 9.");
        }

        if (validatorConfiguration.getValidator().size() > 1) {
            LOG.warn("Support for multiple validators has been deprecated in Repose 8 and will be removed in Repose 9.");
        }

        for (ValidatorItem validatorItem : validatorConfiguration.getValidator()) {
            if (validatorItem.getAny() != null) {
                LOG.warn("Support for embedded WADLs has been deprecated in Repose 8 and will be removed in Repose 9.");
                break;
            }
        }

        for (ValidatorItem validatorItem : validatorConfiguration.getValidator()) {
            if (validatorItem.getRole() != null && !validatorItem.getRole().isEmpty()) {
                LOG.warn("Support for specifying roles in the validator config has been deprecated in Repose 8 and will be removed in Repose 9.  Please use rax:roles in the WADL instead.");
                break;
            }
        }
    }

    private Config createConfiguration(ValidatorItem validatorItem, boolean isDelegating, double delegationQuality,
                                       boolean multiRoleMatch, String configRoot, String componentName) {
        Config config = new Config();

        config.setXSDEngine(validatorItem.getXsdEngine().value());
        config.setXSLEngine(validatorItem.getXslEngine().value());
        config.setResultHandler(getHandlers(validatorItem, isDelegating, delegationQuality, multiRoleMatch, configRoot, componentName));
        config.setCheckWellFormed(validatorItem.isCheckWellFormed());
        config.setCheckXSDGrammar(validatorItem.isCheckXsdGrammar() || validatorItem.isCheckGrammars());
        config.setCheckJSONGrammar(validatorItem.isCheckGrammars());
        config.setCheckElements(validatorItem.isCheckElements());
        config.setXPathVersion(validatorItem.getXpathVersion());
        config.setCheckPlainParams(validatorItem.isCheckPlainParams());
        config.setDoXSDGrammarTransform(validatorItem.isDoXsdGrammarTransform());
        config.setEnablePreProcessExtension(validatorItem.isEnablePreProcessExtension());
        config.setRemoveDups(validatorItem.isRemoveDups());
        config.setValidateChecker(validatorItem.isValidateChecker());
        config.setJoinXPathChecks(validatorItem.isJoinXpathChecks());
        config.setCheckHeaders(validatorItem.isCheckHeaders());
        config.setEnableIgnoreXSDExtension(validatorItem.isEnableIgnoreXsdExtension());
        config.setEnableRaxRolesExtension(validatorItem.isEnableRaxRoles());
        config.setDisableSaxonByteCodeGen(validatorItem.isDisableSaxonByteCodeGen());
        config.setMaskRaxRoles403(validatorItem.isMaskRaxRoles403());
        config.setSetParamDefaults(true);
        config.setEnableAuthenticatedByExtension(true);

        return config;
    }

    private DispatchResultHandler getHandlers(ValidatorItem validatorItem, boolean isDelegating, double delegationQuality,
                                        boolean multiRoleMatch, String configRoot, String componentName) {

        List<ResultHandler> handlers = new ArrayList<>();

        if (isDelegating) {
            handlers.add(new MethodLabelHandler());
            handlers.add(new DelegationHandler(delegationQuality, componentName));
        } else if (!multiRoleMatch) {
            handlers.add(new ServletResultHandler());
        }

        if (validatorItem.isEnableApiCoverage()) {
            handlers.add(new InstrumentedHandler());
            handlers.add(new ApiCoverageHandler());
        }

        if (StringUtils.isNotBlank(validatorItem.getDotOutput())) {
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
        return new DispatchResultHandler(scala.collection.JavaConversions.asScalaBuffer(handlers).toList());
    }

    private String getPath(String path, String configRoot) {
        File file = new File(path);

        if (!file.isAbsolute()) {
            file = new File(configRoot, path);
        }

        return file.getAbsolutePath();
    }

    private String getWadlPath(String uri, String configRoot) {
        //If the wadl path is already absolute, just return that rather than prepending the config root
        if (Paths.get(uri).isAbsolute()) {
            return new File(uri).toString();
        } else {
            return new File(configRoot, uri).toURI().toString();
        }
    }
}
