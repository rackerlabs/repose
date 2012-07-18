package org.openrepose.components.xsdvalidator.filter;

import com.rackspace.com.papi.components.checker.Config;
import com.rackspace.com.papi.components.checker.Validator;
import javax.xml.transform.sax.SAXSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

public class ValidatorInfo {

    private static final Logger LOG = LoggerFactory.getLogger(ValidatorInfo.class);
    private final String uri;
    private final String group;
    private final Config config;
    private Validator validator;

    public ValidatorInfo(String group, String wadlUri, Config config) {
        this.group = group;
        this.uri = wadlUri;
        this.config = config;
    }

    private void initValidator() {
        if (validator != null) {
            return;
        }

        try {
            validator = Validator.apply(new SAXSource(new InputSource(uri)), config);
        } catch (Throwable ex) {
            LOG.warn("Cannot load validator for WADL: " + uri);
        }

    }

    public void clearValidator() {
        validator = null;
    }

    public Validator getValidator() {
        initValidator();
        return validator;
    }

    public String getGroup() {
        return group;
    }

    public String getUri() {
        return uri;
    }
}
