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
import com.rackspace.com.papi.components.checker.Validator;
import org.openrepose.commons.utils.StringUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import java.util.List;

/**
 * TODO this thing shouldn't contain mutable state :(
 * It's the cause of bugs, when we update the validator in here :(
 */
public class ValidatorInfo {

    private static final Logger LOG = LoggerFactory.getLogger(ValidatorInfo.class);
    private final String uri;
    private final List<String> roles;
    private final Config config;
    private final Object validatorLock = new Object();
    private final Node wadl;
    private final String systemId;
    private final String name;
    private Validator validator;

    public ValidatorInfo(List<String> roles, String wadlUri, Config config, String name) {
        this.uri = wadlUri;
        this.roles = roles;
        this.config = config;
        this.wadl = null;
        this.systemId = null;
        this.name = (StringUtilities.isEmpty(name) && !roles.isEmpty()) ? getNameFromRoles(roles) : name;
    }

    public ValidatorInfo(List<String> roles, Node wadl, String systemId, Config config, String name) {
        this.roles = roles;
        this.config = config;
        this.wadl = wadl;
        this.systemId = systemId;
        this.uri = null;
        this.name = (StringUtilities.isEmpty(name) && !roles.isEmpty()) ? getNameFromRoles(roles) : name;
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

    public boolean initValidator() {
        LOG.debug("CALL TO ValidatorInfo#initValidator. Validator is {}. From thread {}", validator, Thread.currentThread().getName());

        //TODO: I bet this is the cause of our thread bugs, I suspect another thread is asking for a validator,
        // and so it's getting initialized and never cleaned up! SUPER TERRIBLE
        //MUTABLE STATE IS REAL BAD
        synchronized (validatorLock) {
            if (validator != null) {
                return true;
            }

            try {
                LOG.debug("Calling the validator creation method for {}", name);
                validator = Validator.apply(name + System.currentTimeMillis(), getSource(), config);
                return true;
            } catch (Throwable t) {
                // we need to be able to catch WADLException which extends Throwable, and in its infinite wisdom, the
                // Java compiler doesn't let us catch it directly, so we have to catch Throwable.
                LOG.warn("Error loading validator for WADL: " + uri, t);
                return false;
            }
        }
    }

    public void clearValidator() {
        synchronized (validatorLock) {
            if (validator != null) {
                validator.destroy();
                validator = null;
            }
        }
    }

    public boolean reinitValidator() {
        synchronized (validatorLock) {
            if (validator != null) {
                LOG.debug("in reInitValidator Destroying: {}", validator);
                validator.destroy();
                validator = null;
            }
        }
        return initValidator();
    }

    public Validator getValidator() {
        initValidator();
        return validator;
    }

    /**
     * This method is to simplify testing.
     *
     * @param validator
     */
    void setValidator(Validator validator) {
        this.validator = validator;
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

    String getNameFromRoles(List<String> roles) {
        StringBuilder name = new StringBuilder();
        for (String role : roles) {
            // To prevent any errors later when the MBean is created, the following substitutions are made to the role
            // names as per: https://docs.oracle.com/javase/8/docs/api/javax/management/ObjectName.html
            // ... should not contain the string "//", which is reserved for future use.
            // ... nonempty string of characters which may not contain any of the characters comma (,), equals (=), colon (:), asterisk (*), or question mark (?).
            //
            // And to prevent any issues or confusion with the space ( ) and non-breaking space (\u00A0) characters
            // which are now known to be in roles, they were added to this list also.
            name.append(role
                    .replace('/', '-')
                    .replace(',', '-')
                    .replace('=', '-')
                    .replace(':', '-')
                    .replace('*', '-')
                    .replace('?', '-')
                    .replace(' ', '-')
                    .replace('\u00A0','-')
                    + "_"
            );
        }
        return name.substring(0, name.length() - 1);
    }
}
