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
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.components.apivalidator.servlet.config.DelegatingType;
import org.openrepose.components.apivalidator.servlet.config.ValidatorConfiguration;
import org.openrepose.components.apivalidator.servlet.config.ValidatorItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

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

    private ValidatorInfo defaultvalidator;
    private List<ValidatorInfo> validators;

    public ValidatorConfigurator() {
    }

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

        DelegatingType delegatingType = validatorConfiguration.getDelegating();
        boolean isDelegating = delegatingType != null;
        double delegationQuality = isDelegating ? delegatingType.getQuality() : 0.0;
        String componentName = isDelegating ? delegatingType.getComponentName() : "api-validator";

        for (ValidatorItem validatorItem : validatorItems) {
            Config configuration = createConfiguration(validatorItem, isDelegating, delegationQuality, validatorConfiguration.isMultiRoleMatch(), configRoot, componentName);
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
        config.setMaskRaxRoles403(validatorItem.isMaskRaxRoles403());
        config.setSetParamDefaults(true);

        return config;
    }

    private DispatchResultHandler getHandlers(ValidatorItem validatorItem, boolean isDelegating, double delegationQuality,
                                        boolean multiRoleMatch, String configRoot, String componentName) {

        List<ResultHandler> handlers = new ArrayList<ResultHandler>();

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
