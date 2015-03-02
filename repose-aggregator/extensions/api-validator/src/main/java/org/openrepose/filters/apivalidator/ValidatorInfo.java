package org.openrepose.filters.apivalidator;

import com.rackspace.com.papi.components.checker.Config;
import com.rackspace.com.papi.components.checker.Validator;
import org.openrepose.commons.utils.StringUtilities;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;

/**
 * TODO this thing shouldn't contain mutable state :(
 * It's the cause of bugs, when we update the validator in here :(
 */
public class ValidatorInfo {

    private static final Logger LOG = LoggerFactory.getLogger(ValidatorInfo.class);
    private final String uri;
    private final List<String> roles;
    private final Config config;
    private Validator validator;
    private final Object validatorLock = new Object();
    private final Node wadl;
    private final String systemId;
    private final String name;

    public ValidatorInfo(List<String> roles, String wadlUri, Config config, String name) {
        this.uri = wadlUri;
        this.roles = roles;
        this.config = config;
        this.wadl = null;
        this.systemId = null;
       
        if (StringUtilities.isEmpty(name) && !roles.isEmpty() && !roles.isEmpty()) {
             this.name =getNameFromRoles(roles);
        } else {
            this.name = name;
        }
    }

    public ValidatorInfo(List<String> roles, Node wadl, String systemId, Config config, String name) {
        this.roles = roles;
        this.config = config;
        this.wadl = wadl;
        this.systemId = systemId;
        this.uri = null;
        if (StringUtilities.isEmpty(name) && !roles.isEmpty() && !roles.isEmpty()) {
            this.name =getNameFromRoles(roles);
        } else {
            this.name = name;
        }
    }

    private Source getSource() {
        if (wadl != null) {
            return new DOMSource(wadl, systemId);
        }

        if (uri != null) {
            return new SAXSource(new InputSource(uri));
        }

        throw new IllegalArgumentException("WADL Source Not Specified");
    }

    //The exceptions thrown by the validator are all custom exceptions which extend throwable
    @SuppressWarnings({"squid:S1181","PMD.AvoidCatchingThrowable"})
    public boolean initValidator() {
        LOG.debug("CALL TO ValidatorInfo#initValidator. Validator is {}. From thread {}", validator, Thread.currentThread().getName());

        //TODO: I bet this is the cause of our thread bugs, I suspect another thread is asking for a validator,
        // and so it's getting initialized and never cleaned up! SUPER TERRIBLE
        //MUTABLE STATE IS REAL BAD
        synchronized(validatorLock) {
            if (validator != null) {
                return true;
            }

            try {
                LOG.debug("Calling the validator creation method for {}", name);
                validator = Validator.apply(name + System.currentTimeMillis(), getSource(), config);
                return true;
            } catch (Throwable ex) {
                LOG.warn("Error loading validator for WADL: " + uri, ex);
                return false;
            }
        }
    }

    public void clearValidator() {
        synchronized(validatorLock) {
            if (validator != null) {
                validator.destroy();
                validator = null;
            }
        }
    }

    public boolean reinitValidator() {
        synchronized(validatorLock) {
            if (validator != null) {
                LOG.debug("in reInitValidator Destroying: {}", validator);
                validator.destroy();
                validator = null;
            }
        }
        return initValidator();
    }

    /**
     * This method is to simplify testing.
     *
     * @param validator
     */
    void setValidator(Validator validator) {
        this.validator = validator;
    }

    public Validator getValidator() {
        initValidator();
        return validator;
    }

    public List<String> getRoles() {
        return roles;
    }

    public String getUri() {
        return uri;
    }

    public String getName() {
        return name;
    }
    
    String getNameFromRoles(List<String> roles){
        StringBuilder name=new StringBuilder();
        for(String role:roles){
            name.append(role+"_");
        }
        return name.toString();
    }
}
