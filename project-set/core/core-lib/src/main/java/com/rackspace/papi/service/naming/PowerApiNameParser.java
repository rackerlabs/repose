package com.rackspace.papi.service.naming;

public class PowerApiNameParser extends SchemeAwareNameParser {

    public static final String[] VALID_URI_SCHEMES = {"powerapi"};

    @Override
    protected String[] validUriSchemes() {
        return VALID_URI_SCHEMES;
    }
}
