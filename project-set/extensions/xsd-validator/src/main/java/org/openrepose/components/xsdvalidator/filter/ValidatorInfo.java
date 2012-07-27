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
    private final String role;
    private final Config config;
    private Validator validator;

    public ValidatorInfo(String role, String wadlUri, Config config) {
        this.role = role;
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
            LOG.warn("Cannot load validator for WADL: " + uri, ex);
        }

    }

    public void clearValidator() {
        validator = null;
    }

    /**
     * This method is to simplify testing.
     * @param validator 
     */
    void setValidator(Validator validator) {
        this.validator = validator;
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
