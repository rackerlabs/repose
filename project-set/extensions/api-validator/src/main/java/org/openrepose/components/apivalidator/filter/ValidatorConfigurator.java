package org.openrepose.components.apivalidator.filter;

import com.rackspace.com.papi.components.checker.Config;
import com.rackspace.com.papi.components.checker.handler.ResultHandler;
import com.rackspace.com.papi.components.checker.handler.SaveDotHandler;
import com.rackspace.com.papi.components.checker.handler.ServletResultHandler;
import com.rackspace.papi.commons.util.StringUriUtilities;
import com.rackspace.papi.commons.util.StringUtilities;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.openrepose.components.apivalidator.servlet.config.ValidatorItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidatorConfigurator {

    private static final Logger LOG = LoggerFactory.getLogger(ValidatorConfigurator.class);
    private final ValidatorItem validatorItem;
    private final boolean multiRoleMatch;
    private final String configRoot;

    public ValidatorConfigurator(ValidatorItem validatorItem, boolean multiRoleMatch, String configRoot) {
        this.validatorItem = validatorItem;
        this.multiRoleMatch = multiRoleMatch;
        this.configRoot = configRoot;
    }

    String getPath(String path){
       File file = new File(path);
       
       if (!file.isAbsolute()){
          file = new File(configRoot, path);
       }
       
       return file.getAbsolutePath();
    }

    private DispatchHandler getHandlers(ValidatorItem validatorItem) {
        List<ResultHandler> handlers = new ArrayList<ResultHandler>();

        if (!multiRoleMatch) {
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

    public Config getConfiguration() {
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
        config.setCheckHeaders(validatorItem.isCheckHeaders());
        config.setEnableIgnoreXSDExtension(validatorItem.isEnableIgnoreXsdExtension());

        return config;
    }
}
