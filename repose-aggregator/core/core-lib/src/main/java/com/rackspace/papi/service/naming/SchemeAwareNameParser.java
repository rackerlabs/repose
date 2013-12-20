package com.rackspace.papi.service.naming;

import com.rackspace.papi.commons.util.StringUtilities;

import javax.naming.*;

public abstract class SchemeAwareNameParser implements NameParser {

    private static final String URI_SCHEME = "powerapi";

    public void checkName(String name) throws InvalidNameException {
        if (StringUtilities.isBlank(name)) {
            throw new InvalidNameException("Name to parse must not be blank or null");
        }
        
        if (!name.contains(":")) {
            return;
        }

        for (String scheme : validUriSchemes()) {
            if (name.startsWith(scheme)) {
                return;
            }
        }

        throw new InvalidNameException("Invalid or unsupported URI scheme specified");
    }

    protected abstract String[] validUriSchemes();

    @Override
    public Name parse(String name) throws NamingException {
        if (StringUtilities.isBlank(name)) {
            throw new InvalidNameException("Name to parse must not be blank or null");
        }
        
        final StringBuilder parsableName = new StringBuilder(name);

        // Is there a scheme? If so check the scheme and then remove it.
        if (name.contains(":")) {
            checkName(name);

            parsableName.delete(0, URI_SCHEME.length() + 1);
        }
        
        if (parsableName.length() > 0) {
            // Do we have a leading root reference? If so, remove it.
            if (parsableName.charAt(0) == '/') {
                parsableName.deleteCharAt(0);
            }
            
            // Do we have a trailing slash? If so, remove it.
            if (parsableName.charAt(parsableName.length() - 1) == '/') {
                parsableName.deleteCharAt(parsableName.length() - 1);
            }
        }

        return new CompositeName(parsableName.toString());
    }
}
