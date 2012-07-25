package org.openrepose.components.xsdvalidator.filter;

import com.rackspace.com.papi.components.checker.Config;
import com.rackspace.com.papi.components.checker.Validator;
import com.rackspace.com.papi.components.checker.handler.ResultHandler;
import com.rackspace.com.papi.components.checker.servlet.CheckerServletRequest;
import com.rackspace.com.papi.components.checker.servlet.CheckerServletResponse;
import com.rackspace.com.papi.components.checker.step.Result;
import javax.servlet.FilterChain;
import javax.xml.transform.sax.SAXSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import scala.Option;
import scala.collection.immutable.List;
import scala.collection.immutable.List$;

public class ValidatorInfo {

    private static final Logger LOG = LoggerFactory.getLogger(ValidatorInfo.class);
    private final String uri;
    private final String role;
    private final Config config;
    private Validator validator;
    private final String dotOutput;

    public ValidatorInfo(String role, String wadlUri, String dotOutput, Config config) {
        this.role = role;
        this.uri = wadlUri;
        this.config = config;
        this.dotOutput = dotOutput;
    }
    
    private void initValidator() {
        if (validator != null) {
            return;
        }

        try {
            validator = Validator.apply(new SAXSource(new InputSource(uri)), config);
        } catch (Throwable ex) {
            LOG.warn("Cannot load validator for WADL: " + uri, ex);
        }

    }

    public void clearValidator() {
        validator = null;
    }

    public Validator getValidator() {
        initValidator();
        return validator;
    }

    public String getRole() {
        return role;
    }

    public String getUri() {
        return uri;
    }
}
